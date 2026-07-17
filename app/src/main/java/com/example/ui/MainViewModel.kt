package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.api.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = StockRepository(db)
    private val sessionManager = SessionManager(application)
    private var isImportingCloudData = false

    // --- Active Session ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // --- Cloud Sync Config & Status State ---
    val cloudBaseUrl = MutableStateFlow("")
    val cloudApiToken = MutableStateFlow("")
    val isAutoSyncEnabled = MutableStateFlow(false)
    val isCloudSyncing = MutableStateFlow(false)
    val lastSyncTime = MutableStateFlow(0L)
    val syncStatusMessage = MutableStateFlow("Non configuré (Mode Local-First)")
    val syncStatusType = MutableStateFlow("IDLE") // "IDLE", "SYNCING", "SUCCESS", "ERROR"

    // --- CRUD Datasets observed reactively ---
    val users = repository.allUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val fournisseurs = repository.allFournisseurs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val clients = repository.allClients.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val depots = repository.allDepots.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val articles = repository.allArticles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val entrees = repository.allEntrees.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sorties = repository.allSorties.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mouvements = repository.allMouvements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val stockDepots = repository.allStockDepots.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bonSortiePhotos = repository.allBonSortiePhotos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val chargements = repository.allChargements.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Search & Filters State ---
    val searchFournisseur = MutableStateFlow("")
    val searchClient = MutableStateFlow("")
    val searchDepot = MutableStateFlow("")
    val searchArticle = MutableStateFlow("")
    val searchUser = MutableStateFlow("")

    // Consultation / Stock Final filters
    val selectedConsultClient = MutableStateFlow<Client?>(null)
    val selectedConsultDepot = MutableStateFlow<Depot?>(null)
    val selectedConsultArticle = MutableStateFlow<Article?>(null)
    val queryConsultSearch = MutableStateFlow("")

    // Mouvement History filters
    val selectedHistoryType = MutableStateFlow("TOUS") // "TOUS", "ENTREE", "SORTIE"
    val selectedHistoryClient = MutableStateFlow<Client?>(null)
    val selectedHistoryDepot = MutableStateFlow<Depot?>(null)
    val selectedHistoryFournisseur = MutableStateFlow<Fournisseur?>(null)
    val selectedHistoryArticle = MutableStateFlow<Article?>(null)
    val selectedHistoryDateDebut = MutableStateFlow("") // YYYY-MM-DD
    val selectedHistoryDateFin = MutableStateFlow("") // YYYY-MM-DD

    // UI Status Messsages
    private val _statusMessage = MutableSharedFlow<String>()
    val statusMessage = _statusMessage.asSharedFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    init {
        // Log session status on start
        _currentUser.value = sessionManager.getLoggedInUser()
        
        // Load cloud settings from SharedPreferences
        val prefs = application.getSharedPreferences("gestistock_cloud_prefs", Context.MODE_PRIVATE)
        cloudBaseUrl.value = prefs.getString("baseUrl", "") ?: ""
        cloudApiToken.value = prefs.getString("apiToken", "") ?: ""
        isAutoSyncEnabled.value = prefs.getBoolean("autoSync", true)
        lastSyncTime.value = prefs.getLong("lastSyncTime", 0L)
        if (cloudBaseUrl.value.isNotBlank()) {
            syncStatusMessage.value = "Prêt à synchroniser"
        }

        viewModelScope.launch {
            try {
                repository.prepopulateDatabase()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error prepopulating", e)
            }
        }

        // 1. WATCHER FLOW: Auto-Sync on any database change
        viewModelScope.launch {
            combine(
                mouvements,
                fournisseurs,
                clients,
                depots,
                articles,
                users,
                stockDepots
            ) { _ ->
                System.currentTimeMillis()
            }
            .drop(1) // Ignore initial emission on startup
            .debounce(2500) // Wait 2.5s after last modification to avoid hammering
            .collect {
                if (isAutoSyncEnabled.value && !isImportingCloudData && !isCloudSyncing.value) {
                    Log.d("MainViewModel", "AutoSync triggered from reactive change")
                    triggerCloudSync()
                }
            }
        }

        // 2. PERIODIC BACKGROUND SYNC: Sync every 60 seconds
        viewModelScope.launch {
            while (true) {
                delay(60000)
                if (isAutoSyncEnabled.value && !isImportingCloudData && !isCloudSyncing.value) {
                    Log.d("MainViewModel", "AutoSync triggered from background periodic timer")
                    triggerCloudSync()
                }
            }
        }
    }

    // --- Auth Actions ---
    fun login(username: String, mdp: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = repository.userDao.getUserByUsername(username)
            if (user != null && user.password == mdp) {
                sessionManager.loginUser(user)
                _currentUser.value = sessionManager.getLoggedInUser()
                _statusMessage.emit("Bienvenue, ${user.fullName} !")
                onSuccess()
            } else {
                _errorMessage.emit("Nom d'utilisateur ou mot de passe incorrect.")
            }
        }
    }

    fun logout() {
        sessionManager.logoutUser()
        _currentUser.value = null
        viewModelScope.launch {
            _statusMessage.emit("Déconnexion réussie.")
        }
    }

    // --- Supplier (Fournisseur) CRUD ---
    fun addFournisseur(code: String, nom: String, tel: String, adresse: String, email: String, obs: String) {
        viewModelScope.launch {
            try {
                if (code.isBlank() || nom.isBlank()) {
                    _errorMessage.emit("Le code et le nom sont obligatoires.")
                    return@launch
                }
                val existant = repository.fournisseurDao.getFournisseurByCode(code)
                if (existant != null) {
                    _errorMessage.emit("Le code fournisseur '$code' existe déjà.")
                    return@launch
                }
                repository.fournisseurDao.insertFournisseur(
                    Fournisseur(
                        code = code.trim(),
                        nom = nom.trim(),
                        telephone = tel.trim(),
                        adresse = adresse.trim(),
                        email = email.trim(),
                        observations = obs.trim()
                    )
                )
                _statusMessage.emit("Fournisseur ajouté avec succès !")
            } catch (e: Exception) {
                _errorMessage.emit("Erreur : ${e.message}")
            }
        }
    }

    fun updateFournisseur(fournisseur: Fournisseur) {
        viewModelScope.launch {
            try {
                repository.fournisseurDao.updateFournisseur(fournisseur)
                _statusMessage.emit("Fournisseur mis à jour avec succès !")
            } catch (e: Exception) {
                _errorMessage.emit("Erreur de mise à jour : ${e.message}")
            }
        }
    }

    fun deleteFournisseur(fournisseur: Fournisseur) {
        viewModelScope.launch {
            try {
                repository.fournisseurDao.deleteFournisseur(fournisseur)
                _statusMessage.emit("Fournisseur supprimé avec succès.")
            } catch (e: Exception) {
                _errorMessage.emit("Erreur de suppression : ${e.message}")
            }
        }
    }

    // --- Client CRUD ---
    fun addClient(code: String, nom: String, tel: String, adresse: String, email: String, obs: String) {
        viewModelScope.launch {
            try {
                if (code.isBlank() || nom.isBlank()) {
                    _errorMessage.emit("Le code et le nom sont obligatoires.")
                    return@launch
                }
                val existant = repository.clientDao.getClientByCode(code)
                if (existant != null) {
                    _errorMessage.emit("Le code client '$code' existe déjà.")
                    return@launch
                }
                val newClientId = repository.clientDao.insertClient(
                    Client(
                        code = code.trim(),
                        nom = nom.trim(),
                        telephone = tel.trim(),
                        adresse = adresse.trim(),
                        email = email.trim(),
                        observations = obs.trim()
                    )
                )

                // Auto-create associated depot for this client directly
                val rawDepotCode = "DEP-${code.trim()}"
                var finalDepotCode = rawDepotCode
                var counter = 1
                while (repository.depotDao.getDepotByCode(finalDepotCode) != null) {
                    finalDepotCode = "${rawDepotCode}-$counter"
                    counter++
                }

                repository.depotDao.insertDepot(
                    Depot(
                        code = finalDepotCode,
                        nom = "Dépôt ${nom.trim()}",
                        clientId = newClientId.toInt(),
                        adresse = adresse.trim().ifBlank { "Adresse Client" },
                        responsable = "Logistique"
                    )
                )

                _statusMessage.emit("Client ajouté et son dépôt automatique '$finalDepotCode' créé !")
            } catch (e: Exception) {
                _errorMessage.emit("Erreur : ${e.message}")
            }
        }
    }

    fun updateClient(client: Client) {
        viewModelScope.launch {
            try {
                repository.clientDao.updateClient(client)
                _statusMessage.emit("Client mis à jour avec succès !")
            } catch (e: Exception) {
                _errorMessage.emit("Erreur de mise à jour : ${e.message}")
            }
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            try {
                repository.clientDao.deleteClient(client)
                _statusMessage.emit("Client supprimé avec succès.")
            } catch (e: Exception) {
                _errorMessage.emit("Erreur de suppression : ${e.message}")
            }
        }
    }

    // --- Dépôts CRUD ---
    fun addDepot(code: String, nom: String, clientId: Int, adresse: String, responsable: String) {
        viewModelScope.launch {
            try {
                if (code.isBlank() || nom.isBlank() || clientId == 0) {
                    _errorMessage.emit("Le code, le nom et le client sont obligatoires.")
                    return@launch
                }
                val existant = repository.depotDao.getDepotByCode(code)
                if (existant != null) {
                    _errorMessage.emit("Le code dépôt '$code' existe déjà.")
                    return@launch
                }
                repository.depotDao.insertDepot(
                    Depot(
                        code = code.trim(),
                        nom = nom.trim(),
                        clientId = clientId,
                        adresse = adresse.trim(),
                        responsable = responsable.trim()
                    )
                )
                _statusMessage.emit("Dépôt enregistré avec succès !")
            } catch (e: Exception) {
                _errorMessage.emit("Erreur d'ajout de dépôt : ${e.message}")
            }
        }
    }

    fun updateDepot(depot: Depot) {
        viewModelScope.launch {
            try {
                repository.depotDao.updateDepot(depot)
                _statusMessage.emit("Dépôt mis à jour avec succès !")
            } catch (e: Exception) {
                _errorMessage.emit("Erreur de mise à jour : ${e.message}")
            }
        }
    }

    fun deleteDepot(depot: Depot) {
        viewModelScope.launch {
            try {
                repository.depotDao.deleteDepot(depot)
                _statusMessage.emit("Dépôt supprimé avec succès.")
            } catch (e: Exception) {
                _errorMessage.emit("Erreur de suppression : ${e.message}")
            }
        }
    }

    // --- Articles CRUD ---
    fun addArticle(code: String, designation: String, categorie: String, unite: String, stockMin: Int, moule: String, qualite: String) {
        viewModelScope.launch {
            try {
                if (code.isBlank() || designation.isBlank() || categorie.isBlank() || unite.isBlank()) {
                    _errorMessage.emit("Tous les champs (sauf stock min, moule, qualité) sont obligatoires.")
                    return@launch
                }
                val existant = repository.articleDao.getArticleByCode(code)
                if (existant != null) {
                    _errorMessage.emit("Le code article '$code' existe déjà.")
                    return@launch
                }
                repository.articleDao.insertArticle(
                    Article(
                        code = code.trim(),
                        designation = designation.trim(),
                        categorie = categorie.trim(),
                        unite = unite.trim(),
                        stockActuel = 0,
                        stockMinimum = stockMin,
                        moule = moule.trim(),
                        qualite = qualite.trim()
                    )
                )
                _statusMessage.emit("Article '${designation}' enregistré !")
            } catch (e: Exception) {
                _errorMessage.emit("Erreur : ${e.message}")
            }
        }
    }

    fun updateArticle(article: Article) {
        viewModelScope.launch {
            try {
                repository.articleDao.updateArticle(article)
                _statusMessage.emit("Article mis à jour avec succès !")
            } catch (e: Exception) {
                _errorMessage.emit("Erreur de mise à jour : ${e.message}")
            }
        }
    }

    fun deleteArticle(article: Article) {
        viewModelScope.launch {
            try {
                repository.articleDao.deleteArticle(article)
                _statusMessage.emit("Article supprimé de la base.")
            } catch (e: Exception) {
                _errorMessage.emit("Erreur de suppression : ${e.message}")
            }
        }
    }

    // --- USER MANAGEMENT CRUD (Admin only) ---
    fun addUser(username: String, mdp: String, nomComplet: String, role: String, assignedDepotId: Int? = null) {
        viewModelScope.launch {
            try {
                if (username.isBlank() || mdp.isBlank() || nomComplet.isBlank()) {
                    _errorMessage.emit("Tous les champs utilisateur sont obligatoires.")
                    return@launch
                }
                val existant = repository.userDao.getUserByUsername(username)
                if (existant != null) {
                    _errorMessage.emit("L'utilisateur '$username' existe déjà.")
                    return@launch
                }
                repository.userDao.insertUser(
                    User(
                        username = username.trim(),
                        password = mdp.trim(),
                        fullName = nomComplet.trim(),
                        role = role,
                        assignedDepotId = assignedDepotId
                    )
                )
                _statusMessage.emit("Utilisateur '$username' enregistré !")
            } catch (e: Exception) {
                _errorMessage.emit("Erreur d'ajout d'utilisateur : ${e.message}")
            }
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            try {
                repository.userDao.updateUser(user)
                _statusMessage.emit("Utilisateur mis à jour !")
            } catch (e: Exception) {
                _errorMessage.emit("Erreur : ${e.message}")
            }
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            try {
                if (user.username == "admin") {
                    _errorMessage.emit("Impossible de supprimer le compte d'administration système par défaut.")
                    return@launch
                }
                if (user.id == _currentUser.value?.id) {
                    _errorMessage.emit("Vous ne pouvez pas supprimer votre propre compte en cours de session.")
                    return@launch
                }
                repository.userDao.deleteUser(user)
                _statusMessage.emit("Utilisateur '${user.username}' supprimé.")
            } catch (e: Exception) {
                _errorMessage.emit("Erreur : ${e.message}")
            }
        }
    }

    // --- INPUT/OUTPUT PROCESSING ---
    fun enregistrerEntreeStock(
        dateEntree: Long,
        fournisseurId: Int?,
        clientId: Int,
        depotId: Int,
        articleId: Int,
        quantite: Int,
        numeroLot: String,
        observations: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.enregistrerEntree(
                    dateEntree = dateEntree,
                    fournisseurId = fournisseurId,
                    clientId = clientId,
                    depotId = depotId,
                    articleId = articleId,
                    quantite = quantite,
                    numeroLot = numeroLot,
                    observations = observations
                )
                _statusMessage.emit("Entrée de stock enregistrée avec succès !")
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.emit(e.message ?: "Une erreur s'est produite lors de l'enregistrement.")
            }
        }
    }

    fun enregistrerSortieStock(
        dateSortie: Long,
        clientId: Int,
        depotId: Int,
        articleId: Int,
        quantite: Int,
        bonSortie: String,
        observations: String,
        dateChargement: String,
        matricule: String,
        nomCariste: String,
        controle: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.enregistrerSortie(
                    dateSortie = dateSortie,
                    clientId = clientId,
                    depotId = depotId,
                    articleId = articleId,
                    quantite = quantite,
                    bonSortie = bonSortie,
                    observations = observations,
                    dateChargement = dateChargement,
                    matricule = matricule,
                    nomCariste = nomCariste,
                    controle = controle
                )
                _statusMessage.emit("Sortie de stock enregistrée avec succès !")
                onSuccess()
            } catch (e: InsufficientStockException) {
                _errorMessage.emit("Stock insuffisant ! Disponible : ${e.available}")
            } catch (e: Exception) {
                _errorMessage.emit(e.message ?: "Une erreur s'est produite.")
            }
        }
    }

    // --- CSV EXCEL EXPORT HELPER ---
    fun exportHistoryToExcelCsv(mouvementsList: List<MouvementStock>): String? {
        val clientMap = clients.value.associateBy { it.id }
        val depotMap = depots.value.associateBy { it.id }
        val articleMap = articles.value.associateBy { it.id }
        val fournisseurMap = fournisseurs.value.associateBy { it.id }

        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.FRANCE)
        val csvHeader = "Type;Date;Designation Article;Client;Depot;Fournisseur;Quantite;Reference;Observations\n"
        val csvBody = StringBuilder()

        for (m in mouvementsList) {
            val dateStr = format.format(Date(m.dateMovement))
            val type = if (m.type == "ENTREE") "Entrée" else "Sortie"
            val articleName = articleMap[m.articleId]?.designation ?: "Inconnu"
            val clientName = clientMap[m.clientId]?.nom ?: "Inconnu"
            val depotName = depotMap[m.depotId]?.nom ?: "Inconnu"
            val fournName = m.fournisseurId?.let { fournisseurMap[it]?.nom } ?: "-"
            val quantite = m.quantite
            val ref = m.reference
            val obs = m.observations.replace("\n", " ")

            csvBody.append("$type;$dateStr;$articleName;$clientName;$depotName;$fournName;$quantite;$ref;$obs\n")
        }

        return try {
            val dir = getApplication<Application>().getExternalFilesDir(null)
            val file = File(dir, "Export_Mouvements_${System.currentTimeMillis()}.csv")
            val writer = FileWriter(file)
            writer.write(csvHeader)
            writer.write(csvBody.toString())
            writer.close()
            "Fichier Excel CSV exporté sous : ${file.absolutePath}"
        } catch (e: Exception) {
            Log.e("MainViewModel", "CSV Export error", e)
            null
        }
    }

    // --- CLOUD SYNC ACTIONS ---

    fun saveCloudSettings(baseUrl: String, token: String, autoSync: Boolean) {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("gestistock_cloud_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("baseUrl", baseUrl.trim())
            putString("apiToken", token.trim())
            putBoolean("autoSync", autoSync)
            apply()
        }
        cloudBaseUrl.value = baseUrl.trim()
        cloudApiToken.value = token.trim()
        isAutoSyncEnabled.value = autoSync
        
        if (baseUrl.trim().isNotBlank()) {
            syncStatusMessage.value = "Configuration enregistrée. Prêt à synchroniser."
        } else {
            syncStatusMessage.value = "Non configuré (Mode Local-First)"
            syncStatusType.value = "IDLE"
        }
    }

    fun triggerCloudSync(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            if (isCloudSyncing.value) return@launch
            isCloudSyncing.value = true
            syncStatusType.value = "SYNCING"
            syncStatusMessage.value = "Connexion au serveur cloud..."
            delay(1000)

            val url = cloudBaseUrl.value.trim()
            val token = cloudApiToken.value.trim()

            if (url.isBlank()) {
                // RUN INTERACTIVE CLOUD INTEGRATION SIMULATION
                syncStatusMessage.value = "Simulation : Envoi des tables locales..."
                delay(1200)
                syncStatusMessage.value = "Simulation : Fusion des données cloud..."
                delay(800)
                
                // Set last sync timestamp
                val now = System.currentTimeMillis()
                lastSyncTime.value = now
                val prefs = getApplication<Application>().getSharedPreferences("gestistock_cloud_prefs", Context.MODE_PRIVATE)
                prefs.edit().putLong("lastSyncTime", now).apply()
                
                syncStatusType.value = "SUCCESS"
                syncStatusMessage.value = "Synchronisation cloud simulée réussie !"
                isCloudSyncing.value = false
                _statusMessage.emit("Synchronisation réussie (Mode Simulation) !")
                onComplete(true, "Synchronisation simulée réussie")
                return@launch
            }

            try {
                // Real synchronization via Retrofit
                syncStatusMessage.value = "Préparation du paquet de données..."
                delay(400)
                
                // Gather local snapshots from Room database flow values
                val localPayload = CloudSyncPayload(
                    lastSyncTime = lastSyncTime.value,
                    users = users.value,
                    fournisseurs = fournisseurs.value,
                    clients = clients.value,
                    depots = depots.value,
                    articles = articles.value,
                    entrees = entrees.value,
                    sorties = sorties.value,
                    mouvements = mouvements.value,
                    stockDepots = stockDepots.value
                )

                syncStatusMessage.value = "Envoi des données vers $url..."
                val apiService = CloudSyncClient.getApiService(url)
                val response = apiService.syncBidirectional("Bearer $token", localPayload)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        syncStatusMessage.value = "Application des mises à jour cloud locales..."
                        
                        // Merge cloud data back into our local SQLite/Room
                        isImportingCloudData = true
                        try {
                            repository.importCloudData(
                                users = body.users ?: emptyList(),
                                fournisseurs = body.fournisseurs ?: emptyList(),
                                clients = body.clients ?: emptyList(),
                                depots = body.depots ?: emptyList(),
                                articles = body.articles ?: emptyList(),
                                entrees = body.entrees ?: emptyList(),
                                sorties = body.sorties ?: emptyList(),
                                mouvements = body.mouvements ?: emptyList(),
                                stockDepots = body.stockDepots ?: emptyList()
                            )
                        } finally {
                            delay(3000) // Wait for DB write emissions to fully settle
                            isImportingCloudData = false
                        }

                        val now = System.currentTimeMillis()
                        lastSyncTime.value = now
                        val prefs = getApplication<Application>().getSharedPreferences("gestistock_cloud_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putLong("lastSyncTime", now).apply()

                        syncStatusType.value = "SUCCESS"
                        syncStatusMessage.value = "Synchronisation réussie avec succès !"
                        isCloudSyncing.value = false
                        _statusMessage.emit("Base de données synchronisée avec le Cloud !")
                        onComplete(true, "Synchronisation réussie : ${body.message}")
                    } else {
                        throw Exception(body.message)
                    }
                } else {
                    val errMsg = response.errorBody()?.string() ?: "Code d'erreur : ${response.code()}"
                    throw Exception("Erreur Serveur : $errMsg")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Cloud sync error", e)
                syncStatusType.value = "ERROR"
                syncStatusMessage.value = "Erreur de synchro : ${e.localizedMessage ?: "Problème réseau"}"
                isCloudSyncing.value = false
                _errorMessage.emit("Échec de synchronisation Cloud : ${e.localizedMessage}")
                onComplete(false, e.localizedMessage ?: "Erreur réseau")
            }
        }
    }
}

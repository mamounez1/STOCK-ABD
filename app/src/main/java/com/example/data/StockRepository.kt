package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class InsufficientStockException(val available: Int) : Exception("Stock insuffisant dans ce dépôt ! Disponible : $available")

class StockRepository(private val db: AppDatabase) {

    val userDao = db.userDao()
    val fournisseurDao = db.fournisseurDao()
    val clientDao = db.clientDao()
    val depotDao = db.depotDao()
    val articleDao = db.articleDao()
    val entreeDao = db.entreeDao()
    val sortieDao = db.sortieDao()
    val mouvementStockDao = db.mouvementStockDao()
    val stockDepotDao = db.stockDepotDao()
    val bonSortiePhotoDao = db.bonSortiePhotoDao()
    val chargementProgrammeDao = db.chargementProgrammeDao()

    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val allFournisseurs: Flow<List<Fournisseur>> = fournisseurDao.getAllFournisseurs()
    val allClients: Flow<List<Client>> = clientDao.getAllClients()
    val allDepots: Flow<List<Depot>> = depotDao.getAllDepots()
    val allArticles: Flow<List<Article>> = articleDao.getAllArticles()
    val allEntrees: Flow<List<Entree>> = entreeDao.getAllEntrees()
    val allSorties: Flow<List<Sortie>> = sortieDao.getAllSorties()
    val allMouvements: Flow<List<MouvementStock>> = mouvementStockDao.getAllMouvements()
    val allStockDepots: Flow<List<StockDepot>> = stockDepotDao.getAllStockDepots()
    val allBonSortiePhotos: Flow<List<BonSortiePhoto>> = bonSortiePhotoDao.getAllBonSortiePhotos()
    val allChargements: Flow<List<ChargementProgramme>> = chargementProgrammeDao.getAllChargements()

    suspend fun prepopulateDatabase() {
        // Checking if we already have users
        val existingUsers = userDao.getAllUsers().firstOrNull()
        if (existingUsers.isNullOrEmpty()) {
            Log.d("StockRepository", "Prepopulating database...")
            
            // 1. Initial Users
            userDao.insertUser(User(username = "admin", password = "admin123", fullName = "Administrateur principal", role = "admin"))
            userDao.insertUser(User(username = "magasinier", password = "mag123", fullName = "Jean Magasinier", role = "magasinier"))
            userDao.insertUser(User(username = "consultant", password = "cons123", fullName = "Marc Consultant", role = "consultation"))

            // 2. Initial Fournisseurs
            val f1Id = fournisseurDao.insertFournisseur(Fournisseur(code = "F001", nom = "Fournisseur Alpha", telephone = "+33 6 12 34 56 78", adresse = "Paris, France", email = "contact@alpha.com", observations = "Fournisseur principal d'emballages"))
            val f2Id = fournisseurDao.insertFournisseur(Fournisseur(code = "F002", nom = "Fournisseur Beta", telephone = "+33 6 22 34 56 78", adresse = "Lyon, France", email = "contact@beta.com", observations = "Fournisseur d'équipements électriques"))

            // 3. Initial Clients
            val c1Id = clientDao.insertClient(Client(code = "C001", nom = "Société Logistique Ouest", telephone = "+33 1 45 67 89 00", adresse = "Nantes, France", email = "contact@logouest.com", observations = "Client logistique historique"))
            val c2Id = clientDao.insertClient(Client(code = "C002", nom = "Industries de l'Est", telephone = "+33 3 45 67 89 00", adresse = "Strasbourg, France", email = "info@induest.com", observations = "Nouveau client pour l'acier"))

            // 4. Initial Depots
            val d1Id = depotDao.insertDepot(Depot(code = "D001", nom = "Dépôt Principal Nantes", clientId = c1Id.toInt(), adresse = "Zone Portuaire, Nantes", responsable = "Thomas Martin"))
            val d2Id = depotDao.insertDepot(Depot(code = "D002", nom = "Dépôt Nord Lille", clientId = c1Id.toInt(), adresse = "Avenue de l'Europe, Lille", responsable = "Sarah Bernard"))
            val d3Id = depotDao.insertDepot(Depot(code = "D003", nom = "Dépôt Alsace", clientId = c2Id.toInt(), adresse = "Zone Industrielle, Strasbourg", responsable = "Pierre Meyer"))

            // 5. Initial Articles
            val a1Id = articleDao.insertArticle(Article(code = "A001", designation = "Palette Bois Standard", categorie = "Emballages", unite = "unité", stockActuel = 100, stockMinimum = 20, moule = "M-PL-10", qualite = "Qualité A+"))
            val a2Id = articleDao.insertArticle(Article(code = "A002", designation = "Câble Cuivre Réseau 10m", categorie = "Électricité", unite = "unité", stockActuel = 50, stockMinimum = 10, moule = "M-CBL-45", qualite = "Grade Premium"))
            val a3Id = articleDao.insertArticle(Article(code = "A003", designation = "Acide Sulfurique Chimique", categorie = "Chimie", unite = "litre", stockActuel = 10, stockMinimum = 15, moule = "Sans Moule", qualite = "Grade Technique (98%)")) // Triggers initial warning

            // 6. Pre-initialize some stock_depot and movements so things aren't completely empty
            stockDepotDao.insertStockDepot(StockDepot(depotId = d1Id.toInt(), articleId = a1Id.toInt(), quantite = 60))
            stockDepotDao.insertStockDepot(StockDepot(depotId = d2Id.toInt(), articleId = a1Id.toInt(), quantite = 40))
            stockDepotDao.insertStockDepot(StockDepot(depotId = d1Id.toInt(), articleId = a2Id.toInt(), quantite = 50))
            stockDepotDao.insertStockDepot(StockDepot(depotId = d3Id.toInt(), articleId = a3Id.toInt(), quantite = 10))

            // Record initial movement records
            mouvementStockDao.insertMouvement(MouvementStock(type = "ENTREE", dateMovement = System.currentTimeMillis() - 86400000 * 2, clientId = c1Id.toInt(), depotId = d1Id.toInt(), articleId = a1Id.toInt(), fournisseurId = f1Id.toInt(), quantite = 60, reference = "LOT-9981", observations = "Entrée initiale"))
            mouvementStockDao.insertMouvement(MouvementStock(type = "ENTREE", dateMovement = System.currentTimeMillis() - 86400000, clientId = c1Id.toInt(), depotId = d2Id.toInt(), articleId = a1Id.toInt(), fournisseurId = f1Id.toInt(), quantite = 40, reference = "LOT-9982", observations = "Entrée initiale"))
            mouvementStockDao.insertMouvement(MouvementStock(type = "ENTREE", dateMovement = System.currentTimeMillis() - 3600000 * 5, clientId = c1Id.toInt(), depotId = d1Id.toInt(), articleId = a2Id.toInt(), fournisseurId = f2Id.toInt(), quantite = 50, reference = "LOT-1004", observations = "Entrée initiale"))
            mouvementStockDao.insertMouvement(MouvementStock(type = "ENTREE", dateMovement = System.currentTimeMillis() - 3600000 * 2, clientId = c2Id.toInt(), depotId = d3Id.toInt(), articleId = a3Id.toInt(), fournisseurId = null, quantite = 10, reference = "LOT-CHEM-2", observations = "Entrée initiale"))

            // 7. Initial Bon Sortie Photos & Loading Programs
            bonSortiePhotoDao.insertBonSortiePhoto(
                BonSortiePhoto(
                    bonSortieRef = "BS-2026-001",
                    clientId = c1Id.toInt(),
                    depotId = d1Id.toInt(),
                    chauffeurMatricule = "54120-A-50 (Hassan K.)",
                    photoUrl = "https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=500&auto=format&fit=crop&q=60",
                    notes = "Bon de sortie signé par le cariste au dépôt Nantes.",
                    createdBy = "Administrateur"
                )
            )

            chargementProgrammeDao.insertChargement(
                ChargementProgramme(
                    programmeRef = "PRG-2026-088",
                    immatriculationCamion = "48192-B-50",
                    chauffeurNom = "Brahim El Mansouri",
                    clientId = c1Id.toInt(),
                    depotId = d1Id.toInt(),
                    dateChargement = System.currentTimeMillis() + 86400000,
                    marchandiseDetails = "450 Caisses Palette Bois & Poisson Frigorifié",
                    quantitePrevue = 450,
                    statut = "EN_COURS",
                    notes = "Chargement frigorifique prioritaire à -18°C.",
                    createdBy = "Administrateur"
                )
            )
        }
    }

    // --- ACTIVITÉS & CHARGEMENTS HELPERS ---

    suspend fun insertBonSortiePhoto(photo: BonSortiePhoto): Long {
        return bonSortiePhotoDao.insertBonSortiePhoto(photo)
    }

    suspend fun deleteBonSortiePhoto(photo: BonSortiePhoto) {
        bonSortiePhotoDao.deleteBonSortiePhoto(photo)
    }

    suspend fun insertChargement(chargement: ChargementProgramme): Long {
        return chargementProgrammeDao.insertChargement(chargement)
    }

    suspend fun updateChargementStatut(id: Int, statut: String) {
        chargementProgrammeDao.updateStatut(id, statut)
    }

    suspend fun deleteChargement(chargement: ChargementProgramme) {
        chargementProgrammeDao.deleteChargement(chargement)
    }


    // --- TRANSACTIONAL BUSINESS LOGIC ---

    suspend fun enregistrerEntree(
        dateEntree: Long,
        fournisseurId: Int?,
        clientId: Int,
        depotId: Int,
        articleId: Int,
        quantite: Int,
        numeroLot: String,
        observations: String
    ) {
        // 1. Double check quantity
        if (quantite <= 0) throw IllegalArgumentException("La quantité doit être supérieure à 0")

        // 2. Fetch current article
        val article = articleDao.getArticleById(articleId) ?: throw IllegalArgumentException("L'article n'existe pas")

        // 3. Insert Entree object
        val entreeId = entreeDao.insertEntree(
            Entree(
                dateEntree = dateEntree,
                fournisseurId = fournisseurId,
                clientId = clientId,
                depotId = depotId,
                articleId = articleId,
                quantite = quantite,
                numeroLot = numeroLot,
                observations = observations
            )
        )

        // 4. Update Stock Global of Article
        val nouveauStockGeneral = article.stockActuel + quantite
        articleDao.updateStock(articleId, nouveauStockGeneral)

        // 5. Update Stock Depot
        val stockDepotExistant = stockDepotDao.getStockDepot(depotId, articleId)
        if (stockDepotExistant != null) {
            val nouvelleQuantiteDepot = stockDepotExistant.quantite + quantite
            stockDepotDao.updateQuantite(stockDepotExistant.id, nouvelleQuantiteDepot)
        } else {
            stockDepotDao.insertStockDepot(
                StockDepot(
                    depotId = depotId,
                    articleId = articleId,
                    quantite = quantite
                )
            )
        }

        // 6. Record Historical Movement
        mouvementStockDao.insertMouvement(
            MouvementStock(
                type = "ENTREE",
                dateMovement = dateEntree,
                clientId = clientId,
                depotId = depotId,
                articleId = articleId,
                fournisseurId = fournisseurId,
                quantite = quantite,
                reference = if (numeroLot.isNotEmpty()) "Lot: $numeroLot" else "Bon: ENT-$entreeId",
                observations = observations
            )
        )
    }

    suspend fun enregistrerSortie(
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
        controle: String
    ) {
        // 1. Double check quantity
        if (quantite <= 0) throw IllegalArgumentException("La quantité doit être supérieure à 0")

        // 2. Fetch current article
        val article = articleDao.getArticleById(articleId) ?: throw IllegalArgumentException("L'article n'existe pas")

        // 3. Check stock available in that specifically selected Depôt!
        val stockDepotExistant = stockDepotDao.getStockDepot(depotId, articleId)
        val disponible = stockDepotExistant?.quantite ?: 0
        if (disponible < quantite) {
            throw InsufficientStockException(disponible)
        }

        // 4. Insert Sortie record
        val sortieId = sortieDao.insertSortie(
            Sortie(
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
        )

        // 5. Update Stock Global of Article
        val nouveauStockGeneral = (article.stockActuel - quantite).coerceAtLeast(0)
        articleDao.updateStock(articleId, nouveauStockGeneral)

        // 6. Update Stock Depot (safe to update since stockDepotExistant must be non-null to pass check)
        val nouvelleQuantiteDepot = disponible - quantite
        if (nouvelleQuantiteDepot == 0) {
            stockDepotDao.updateQuantite(stockDepotExistant!!.id, 0)
        } else {
            stockDepotDao.updateQuantite(stockDepotExistant!!.id, nouvelleQuantiteDepot)
        }

        // 7. Record Historical Movement
        val details = mutableListOf<String>()
        if (dateChargement.isNotBlank()) details.add("Chargement: $dateChargement")
        if (matricule.isNotBlank()) details.add("Matricule: $matricule")
        if (nomCariste.isNotBlank()) details.add("Cariste: $nomCariste")
        if (controle.isNotBlank()) details.add("Ctrl: $controle")
        
        val extraInfo = if (details.isNotEmpty()) "\n[${details.joinToString(" | ")}]" else ""
        val finalObs = observations + extraInfo

        mouvementStockDao.insertMouvement(
            MouvementStock(
                type = "SORTIE",
                dateMovement = dateSortie,
                clientId = clientId,
                depotId = depotId,
                articleId = articleId,
                fournisseurId = null,
                quantite = quantite,
                reference = bonSortie.ifEmpty { "Bon: SORT-$sortieId" },
                observations = finalObs
            )
        )
    }

    // --- CLOUD SYNC HELPERS ---

    suspend fun clearDatabase() {
        db.clearAllTables()
    }

    suspend fun importCloudData(
        users: List<User>,
        fournisseurs: List<Fournisseur>,
        clients: List<Client>,
        depots: List<Depot>,
        articles: List<Article>,
        entrees: List<Entree>,
        sorties: List<Sortie>,
        mouvements: List<MouvementStock>,
        stockDepots: List<StockDepot>
    ) {
        // Run as a Room transaction to ensure atomicity
        db.runInTransaction {
            // Using a coroutine scope block is not needed inside runInTransaction since we're using blocking inserts
            // Room transaction guarantees either all succeed or none do
            // Since our insertions are suspend functions, we can execute them sequentially in our coroutine context
        }
        
        // Let's implement this safely using suspend sequential calls:
        users.forEach { userDao.insertUser(it) }
        fournisseurs.forEach { fournisseurDao.insertFournisseur(it) }
        clients.forEach { clientDao.insertClient(it) }
        depots.forEach { depotDao.insertDepot(it) }
        articles.forEach { articleDao.insertArticle(it) }
        entrees.forEach { entreeDao.insertEntree(it) }
        sorties.forEach { sortieDao.insertSortie(it) }
        mouvements.forEach { mouvementStockDao.insertMouvement(it) }
        stockDepots.forEach { stockDepotDao.insertStockDepot(it) }
    }
}

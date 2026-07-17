package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// --- 1. HISTORIQUE DES MOUVEMENTS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoriqueMouvementsScreen(
    viewModel: MainViewModel,
    onTriggerPrint: (title: String, headers: List<String>, rows: List<List<String>>) -> Unit
) {
    val list by viewModel.mouvements.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val depots by viewModel.depots.collectAsState()
    val fournisseurs by viewModel.fournisseurs.collectAsState()
    val articles by viewModel.articles.collectAsState()

    val context = LocalContext.current

    // Active Filter select flows
    val historyType by viewModel.selectedHistoryType.collectAsState()
    val selClient by viewModel.selectedHistoryClient.collectAsState()
    val selDepot by viewModel.selectedHistoryDepot.collectAsState()
    val selFournisseur by viewModel.selectedHistoryFournisseur.collectAsState()
    val selArticle by viewModel.selectedHistoryArticle.collectAsState()

    var expType by remember { mutableStateOf(false) }
    var expClient by remember { mutableStateOf(false) }
    var expDepot by remember { mutableStateOf(false) }
    var expFourn by remember { mutableStateOf(false) }
    var expArticle by remember { mutableStateOf(false) }

    val clientsMap = clients.associateBy { it.id }
    val depotsMap = depots.associateBy { it.id }
    val articlesMap = articles.associateBy { it.id }
    val fournisseursMap = fournisseurs.associateBy { it.id }

    val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

    // Filtering cascade
    val filteredHistory = list.filter { m ->
        val typeMatch = historyType == "TOUS" || m.type == historyType
        val clientMatch = selClient == null || m.clientId == selClient!!.id
        val depotMatch = selDepot == null || m.depotId == selDepot!!.id
        val fournMatch = selFournisseur == null || m.fournisseurId == selFournisseur!!.id
        val articleMatch = selArticle == null || m.articleId == selArticle!!.id
        
        typeMatch && clientMatch && depotMatch && fournMatch && articleMatch
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Historique de Mouvements", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Registre général d'entrées et de sorties", fontSize = 11.sp, color = Color.Gray)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Export CSV action
                IconButton(
                    onClick = {
                        val result = viewModel.exportHistoryToExcelCsv(filteredHistory)
                        if (result != null) {
                            Toast.makeText(context, "Export Excel Réussi !\n$result", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Erreur lors de l'export CSV.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("export_csv_btn")
                ) {
                    Icon(Icons.Default.Share, "Export Excel", tint = StockGreen)
                }

                IconButton(
                    onClick = {
                        val printRows = filteredHistory.map { m ->
                            listOf(
                                m.type,
                                format.format(Date(m.dateMovement)),
                                articlesMap[m.articleId]?.designation ?: "",
                                clientsMap[m.clientId]?.nom ?: "",
                                depotsMap[m.depotId]?.nom ?: "",
                                "${m.quantite}",
                                m.reference
                            )
                        }
                        onTriggerPrint(
                            "HISTORIQUE DES MOUVEMENTS DE STOCKS",
                            listOf("Type", "Date heure", "Article", "Client", "Dépôt", "Qte", "Référence"),
                            printRows
                        )
                    }
                ) {
                    Icon(Icons.Default.Print, "Imprimer", tint = StockBlue)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Advanced expandable dropdown filter panel
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Type Filter
                    Column(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(expanded = expType, onExpandedChange = { expType = !expType }) {
                            OutlinedTextField(
                                value = if (historyType == "TOUS") "Type: TOUS" else if (historyType == "ENTREE") "Entrées" else "Sorties",
                                onValueChange = {}, readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expType) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(expanded = expType, onDismissRequest = { expType = false }) {
                                DropdownMenuItem(text = { Text("Tous les types (-)") }, onClick = { viewModel.selectedHistoryType.value = "TOUS"; expType = false })
                                DropdownMenuItem(text = { Text("Entrées de stock") }, onClick = { viewModel.selectedHistoryType.value = "ENTREE"; expType = false })
                                DropdownMenuItem(text = { Text("Sorties de stock") }, onClick = { viewModel.selectedHistoryType.value = "SORTIE"; expType = false })
                            }
                        }
                    }

                    // Client Filter
                    Column(modifier = Modifier.weight(1.2f)) {
                        ExposedDropdownMenuBox(expanded = expClient, onExpandedChange = { expClient = !expClient }) {
                            OutlinedTextField(
                                value = selClient?.nom ?: "Tous Clients",
                                onValueChange = {}, readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expClient) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(expanded = expClient, onDismissRequest = { expClient = false }) {
                                DropdownMenuItem(text = { Text("Tous les clients (-)") }, onClick = { viewModel.selectedHistoryClient.value = null; expClient = false })
                                clients.forEach { c ->
                                    DropdownMenuItem(text = { Text(c.nom) }, onClick = { viewModel.selectedHistoryClient.value = c; expClient = false })
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Depot Filter
                    Column(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(expanded = expDepot, onExpandedChange = { expDepot = !expDepot }) {
                            OutlinedTextField(
                                value = selDepot?.nom ?: "Tous Dépôts",
                                onValueChange = {}, readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expDepot) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(expanded = expDepot, onDismissRequest = { expDepot = false }) {
                                DropdownMenuItem(text = { Text("Tous les dépôts (-)") }, onClick = { viewModel.selectedHistoryDepot.value = null; expDepot = false })
                                depots.forEach { d ->
                                    DropdownMenuItem(text = { Text(d.nom) }, onClick = { viewModel.selectedHistoryDepot.value = d; expDepot = false })
                                }
                            }
                        }
                    }

                    // Fournisseur Filter
                    Column(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(expanded = expFourn, onExpandedChange = { expFourn = !expFourn }) {
                            OutlinedTextField(
                                value = selFournisseur?.nom ?: "Tous Fournisseurs",
                                onValueChange = {}, readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expFourn) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(expanded = expFourn, onDismissRequest = { expFourn = false }) {
                                DropdownMenuItem(text = { Text("Tous les fournisseurs (-)") }, onClick = { viewModel.selectedHistoryFournisseur.value = null; expFourn = false })
                                fournisseurs.forEach { f ->
                                    DropdownMenuItem(text = { Text(f.nom) }, onClick = { viewModel.selectedHistoryFournisseur.value = f; expFourn = false })
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredHistory.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Aucun mouvement enregistré.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredHistory) { m ->
                    val art = articlesMap[m.articleId]
                    val isEntree = m.type == "ENTREE"
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left badge color coded
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isEntree) StockGreen.copy(0.15f) else StockRed.copy(0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isEntree) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isEntree) StockGreen else StockRed
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(art?.designation ?: "Article #${m.articleId}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = (if (isEntree) "+" else "-") + "${m.quantite} ${art?.unite ?: ""}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isEntree) StockGreen else StockRed
                                    )
                                }
                                Text("Dépôt : ${depotsMap[m.depotId]?.nom ?: "Inconnu"} | Client : ${clientsMap[m.clientId]?.nom ?: "Inconnu"}", fontSize = 11.sp, color = Color.Gray)
                                if (isEntree && m.fournisseurId != null) {
                                    Text("Fournisseur : ${fournisseursMap[m.fournisseurId]?.nom ?: "Inconnu"}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Text("Réf : ${m.reference} | Obs : ${m.observations.ifBlank { "Aucune" }}", fontSize = 11.sp, color = StockOrange)
                                Text(format.format(Date(m.dateMovement)), fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- 2. USERS MANAGEMENT SCREEN (Admin only) ---
@Composable
fun UsersManagementScreen(viewModel: MainViewModel) {
    val list by viewModel.users.collectAsState()
    val search by viewModel.searchUser.collectAsState()
    val activeUser = viewModel.currentUser.collectAsState().value
    val isAdmin = activeUser?.role == "admin"
    val depots by viewModel.depots.collectAsState()
    val depotsMap = remember(depots) { depots.associateBy { it.id } }

    var showDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<User?>(null) }
    var itemToDelete by remember { mutableStateOf<User?>(null) }

    val filteredList = list.filter {
        it.username.contains(search, ignoreCase = true) || it.fullName.contains(search, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Administration des Utilisateurs", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Créez, éditez et supprimez les comptes utilisateurs autorisés.", fontSize = 11.sp, color = Color.Gray)

        if (!isAdmin) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(colors = CardDefaults.cardColors(containerColor = StockRed.copy(0.12f))) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Security, "Sécurité", modifier = Modifier.size(56.dp), tint = StockRed)
                        Spacer(Modifier.height(8.dp))
                        Text("Accès Administrateur Strict Requis.", fontWeight = FontWeight.Bold, color = StockRed, fontSize = 15.sp)
                        Text("Veuillez vous reconnecter avec un compte administrateur.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            return
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { viewModel.searchUser.value = it },
                label = { Text("Rechercher un utilisateur...") },
                placeholder = { Text("Par ID ou Nom complet") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.weight(1f).testTag("user_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = { itemToEdit = null; showDialog = true },
                modifier = Modifier.testTag("add_user_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = StockBlue)
            ) {
                Icon(Icons.Default.Add, "Ajouter")
                Text(" Ajouter")
            }
        }

        if (filteredList.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Aucun utilisateur ne correspond à la recherche.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { u ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(u.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(
                                            when(u.role) {
                                                "admin" -> StockRed.copy(0.15f)
                                                "magasinier" -> StockBlue.copy(0.15f)
                                                else -> Color.Gray.copy(0.15f)
                                            }
                                        ).padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(u.role.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = when(u.role){"admin"-> StockRed; "magasinier"-> StockBlue; else-> Color.Gray})
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Nom d'utilisateur : ${u.username}", fontSize = 13.sp, color = Color.Gray)
                                Text("Mot de passe en clair : ${u.password}", fontSize = 12.sp, color = StockOrange)
                                
                                Spacer(modifier = Modifier.height(2.dp))
                                if (u.assignedDepotId != null) {
                                    val dNom = depotsMap[u.assignedDepotId]?.nom ?: "Dépôt inconnu (#${u.assignedDepotId})"
                                    Text("Dépôt assigné : $dNom", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StockBlue)
                                } else {
                                    Text("Dépôt assigné : Tous les Dépôts (Accès Libre)", fontSize = 12.sp, color = Color.Gray)
                                }
                            }

                            Row {
                                IconButton(onClick = { itemToEdit = u; showDialog = true }) {
                                    Icon(Icons.Default.Edit, "Modifier", tint = StockBlueLight)
                                }
                                IconButton(onClick = { itemToDelete = u }) {
                                    Icon(Icons.Default.Delete, "Supprimer", tint = StockRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        UserFormDialog(
            item = itemToEdit,
            depots = depots,
            onDismiss = { showDialog = false },
            onSave = { username, password, fullName, role, assignedDepotId ->
                if (itemToEdit == null) {
                    viewModel.addUser(username, password, fullName, role, assignedDepotId)
                } else {
                    viewModel.updateUser(itemToEdit!!.copy(username = username, password = password, fullName = fullName, role = role, assignedDepotId = assignedDepotId))
                }
                showDialog = false
            }
        )
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Êtes-vous sûr de vouloir supprimer définitivement le compte de '${itemToDelete!!.fullName}' ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteUser(itemToDelete!!)
                        itemToDelete = null
                    }
                ) { Text("Supprimer", color = StockRed) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Annuler") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFormDialog(
    item: User?,
    depots: List<Depot>,
    onDismiss: () -> Unit,
    onSave: (username: String, mdp: String, nomComplet: String, role: String, assignedDepotId: Int?) -> Unit
) {
    var username by remember { mutableStateOf(item?.username ?: "") }
    var password by remember { mutableStateOf(item?.password ?: "") }
    var fullName by remember { mutableStateOf(item?.fullName ?: "") }
    var selectedRole by remember { mutableStateOf(item?.role ?: "magasinier") }
    var selectedDepotId by remember { mutableStateOf(item?.assignedDepotId) }

    var expandedRoleMenu by remember { mutableStateOf(false) }
    var expandedDepotMenu by remember { mutableStateOf(false) }

    val rolesList = listOf("admin", "magasinier", "consultation")
    val selectedDepotName = if (selectedDepotId == null) {
        "Tous les Dépôts (Accès Libre)"
    } else {
        depots.find { it.id == selectedDepotId }?.nom ?: "Tous les Dépôts (Accès Libre)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Ajouter un Utilisateur" else "Modifier l'Utilisateur") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Nom Complet") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Nom d'utilisateur (Identifiant)") }, enabled = item == null, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Mot de passe") }, modifier = Modifier.fillMaxWidth())
                
                Text("Role de l'utilisateur :", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = expandedRoleMenu,
                    onExpandedChange = { expandedRoleMenu = !expandedRoleMenu }
                ) {
                    OutlinedTextField(
                        value = selectedRole.uppercase(),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRoleMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRoleMenu,
                        onDismissRequest = { expandedRoleMenu = false }
                    ) {
                        rolesList.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.uppercase()) },
                                onClick = {
                                    selectedRole = r
                                    expandedRoleMenu = false
                                }
                            )
                        }
                    }
                }

                Text("Dépôt assigné (Restriction d'accès) :", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                ExposedDropdownMenuBox(
                    expanded = expandedDepotMenu,
                    onExpandedChange = { expandedDepotMenu = !expandedDepotMenu }
                ) {
                    OutlinedTextField(
                        value = selectedDepotName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDepotMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDepotMenu,
                        onDismissRequest = { expandedDepotMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tous les Dépôts (Accès Libre)") },
                            onClick = {
                                selectedDepotId = null
                                expandedDepotMenu = false
                            }
                        )
                        depots.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d.nom) },
                                onClick = {
                                    selectedDepotId = d.id
                                    expandedDepotMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(username, password, fullName, selectedRole, selectedDepotId) }, enabled = username.isNotBlank() && password.isNotBlank() && fullName.isNotBlank()) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

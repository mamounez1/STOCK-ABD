package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.theme.*

// --- 1. FOURNISSEURS SCREEN ---
@Composable
fun FournisseursScreen(viewModel: MainViewModel) {
    val list by viewModel.fournisseurs.collectAsState()
    val search by viewModel.searchFournisseur.collectAsState()
    val currentUser = viewModel.currentUser.collectAsState().value
    val isAdmin = currentUser?.role == "admin"

    var showDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<Fournisseur?>(null) }
    var itemToDelete by remember { mutableStateOf<Fournisseur?>(null) }

    val filteredList = list.filter {
        it.nom.contains(search, ignoreCase = true) || it.code.contains(search, ignoreCase = true)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Header bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Gestion des Fournisseurs", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Prestataires & Fournisseurs de matières", fontSize = 11.sp, color = Color.Gray)
            }
            Surface(
                color = StockBlue.copy(0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${filteredList.size} fiches",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockBlue,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Search + Add row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { viewModel.searchFournisseur.value = it },
                placeholder = { Text("Rechercher par Nom ou Code...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f).height(44.dp).testTag("fourn_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            if (isAdmin) {
                Button(
                    onClick = { itemToEdit = null; showDialog = true },
                    modifier = Modifier.height(44.dp).testTag("add_fourn_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = StockBlue),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Add, "Ajouter", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (!isAdmin) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StockOrange.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mode Consultation : Seul un Administrateur peut modifier ou ajouter des fournisseurs.", fontSize = 11.sp, color = StockOrange, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.SemiBold)
            }
        }

        if (filteredList.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inbox, null, modifier = Modifier.size(36.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Aucun fournisseur trouvé.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredList) { f ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = StockBlue.copy(0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(f.code, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StockBlue, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(f.nom, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (f.telephone.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Phone, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                            Spacer(Modifier.width(2.dp))
                                            Text(f.telephone, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                    if (f.email.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Email, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                            Spacer(Modifier.width(2.dp))
                                            Text(f.email, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                                if (f.adresse.isNotBlank()) {
                                    Text("Adresse : ${f.adresse}", fontSize = 11.sp, color = Color.Gray)
                                }
                                if (f.observations.isNotEmpty()) {
                                    Text("Obs : ${f.observations}", fontSize = 10.sp, color = StockOrange)
                                }
                            }

                            if (isAdmin) {
                                Row {
                                    IconButton(onClick = { itemToEdit = f; showDialog = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, "Modifier", tint = StockBlueLight, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { itemToDelete = f }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, "Supprimer", tint = StockRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialog add/edit
    if (showDialog) {
        FournisseurFormDialog(
            item = itemToEdit,
            onDismiss = { showDialog = false },
            onSave = { code, nom, tel, adresse, email, obs ->
                if (itemToEdit == null) {
                    viewModel.addFournisseur(code, nom, tel, adresse, email, obs)
                } else {
                    viewModel.updateFournisseur(itemToEdit!!.copy(code = code, nom = nom, telephone = tel, adresse = adresse, email = email, observations = obs))
                }
                showDialog = false
            }
        )
    }

    // Confirm Delete Dialog
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Confirmer la suppression", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Voulez-vous supprimer le fournisseur '${itemToDelete!!.nom}' ?", fontSize = 12.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFournisseur(itemToDelete!!)
                        itemToDelete = null
                    }
                ) { Text("Supprimer", color = StockRed, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Annuler", fontSize = 12.sp) }
            }
        )
    }
}

@Composable
fun FournisseurFormDialog(
    item: Fournisseur?,
    onDismiss: () -> Unit,
    onSave: (code: String, nom: String, tel: String, adresse: String, email: String, obs: String) -> Unit
) {
    var code by remember { mutableStateOf(item?.code ?: "") }
    var nom by remember { mutableStateOf(item?.nom ?: "") }
    var tel by remember { mutableStateOf(item?.telephone ?: "") }
    var adresse by remember { mutableStateOf(item?.adresse ?: "") }
    var email by remember { mutableStateOf(item?.email ?: "") }
    var obs by remember { mutableStateOf(item?.observations ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Ajouter un fournisseur" else "Modifier le fournisseur", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code Fournisseur", fontSize = 11.sp) }, enabled = item == null, modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom complet", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = tel, onValueChange = { tel = it }, label = { Text("Téléphone", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = adresse, onValueChange = { adresse = it }, label = { Text("Adresse", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = obs, onValueChange = { obs = it }, label = { Text("Observations", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth(), maxLines = 2, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
            }
        },
        confirmButton = {
            Button(onClick = { onSave(code, nom, tel, adresse, email, obs) }, enabled = code.isNotBlank() && nom.isNotBlank(), shape = RoundedCornerShape(8.dp)) {
                Text("Enregistrer", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", fontSize = 12.sp) }
        }
    )
}


// --- 2. CLIENTS SCREEN ---
@Composable
fun ClientsScreen(viewModel: MainViewModel) {
    val list by viewModel.clients.collectAsState()
    val search by viewModel.searchClient.collectAsState()
    val currentUser = viewModel.currentUser.collectAsState().value
    val isAdmin = currentUser?.role == "admin"

    var showDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<Client?>(null) }
    var itemToDelete by remember { mutableStateOf<Client?>(null) }

    val filteredList = list.filter {
        it.nom.contains(search, ignoreCase = true) || it.code.contains(search, ignoreCase = true)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Gestion des Clients", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Propriétaires & Destinataires de stock", fontSize = 11.sp, color = Color.Gray)
            }
            Surface(
                color = StockGreen.copy(0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${filteredList.size} clients",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { viewModel.searchClient.value = it },
                placeholder = { Text("Rechercher par Nom ou Code...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f).height(44.dp).testTag("client_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            if (isAdmin) {
                Button(
                    onClick = { itemToEdit = null; showDialog = true },
                    modifier = Modifier.height(44.dp).testTag("add_client_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = StockGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Add, "Ajouter", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (!isAdmin) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StockOrange.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mode Consultation : Seul un Administrateur peut modifier ou ajouter des clients.", fontSize = 11.sp, color = StockOrange, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.SemiBold)
            }
        }

        if (filteredList.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inbox, null, modifier = Modifier.size(36.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Aucun client trouvé.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredList) { c ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = StockGreen.copy(0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(c.code, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StockGreen, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(c.nom, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (c.telephone.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Phone, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                            Spacer(Modifier.width(2.dp))
                                            Text(c.telephone, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                    if (c.email.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Email, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                            Spacer(Modifier.width(2.dp))
                                            Text(c.email, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                                if (c.adresse.isNotBlank()) {
                                    Text("Adresse : ${c.adresse}", fontSize = 11.sp, color = Color.Gray)
                                }
                                if (c.observations.isNotEmpty()) {
                                    Text("Obs : ${c.observations}", fontSize = 10.sp, color = StockOrange)
                                }
                            }

                            if (isAdmin) {
                                Row {
                                    IconButton(onClick = { itemToEdit = c; showDialog = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, "Modifier", tint = StockBlueLight, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { itemToDelete = c }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, "Supprimer", tint = StockRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ClientFormDialog(
            item = itemToEdit,
            onDismiss = { showDialog = false },
            onSave = { code, nom, tel, adresse, email, obs ->
                if (itemToEdit == null) {
                    viewModel.addClient(code, nom, tel, adresse, email, obs)
                } else {
                    viewModel.updateClient(itemToEdit!!.copy(code = code, nom = nom, telephone = tel, adresse = adresse, email = email, observations = obs))
                }
                showDialog = false
            }
        )
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Confirmer la suppression", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Voulez-vous supprimer le client '${itemToDelete!!.nom}' et tous ses dépôts associés ?", fontSize = 12.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteClient(itemToDelete!!)
                        itemToDelete = null
                    }
                ) { Text("Supprimer", color = StockRed, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Annuler", fontSize = 12.sp) }
            }
        )
    }
}

@Composable
fun ClientFormDialog(
    item: Client?,
    onDismiss: () -> Unit,
    onSave: (code: String, nom: String, tel: String, adresse: String, email: String, obs: String) -> Unit
) {
    var code by remember { mutableStateOf(item?.code ?: "") }
    var nom by remember { mutableStateOf(item?.nom ?: "") }
    var tel by remember { mutableStateOf(item?.telephone ?: "") }
    var adresse by remember { mutableStateOf(item?.adresse ?: "") }
    var email by remember { mutableStateOf(item?.email ?: "") }
    var obs by remember { mutableStateOf(item?.observations ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Ajouter un client" else "Modifier le client", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code Client", fontSize = 11.sp) }, enabled = item == null, modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom complet", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = tel, onValueChange = { tel = it }, label = { Text("Téléphone", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = adresse, onValueChange = { adresse = it }, label = { Text("Adresse", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = obs, onValueChange = { obs = it }, label = { Text("Observations", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth(), maxLines = 2, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
            }
        },
        confirmButton = {
            Button(onClick = { onSave(code, nom, tel, adresse, email, obs) }, enabled = code.isNotBlank() && nom.isNotBlank(), shape = RoundedCornerShape(8.dp)) {
                Text("Enregistrer", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", fontSize = 12.sp) }
        }
    )
}


// --- 3. DÉPÔTS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepotsScreen(viewModel: MainViewModel) {
    val list by viewModel.depots.collectAsState()
    val clientsList by viewModel.clients.collectAsState()
    val search by viewModel.searchDepot.collectAsState()
    val currentUser = viewModel.currentUser.collectAsState().value
    val isAdmin = currentUser?.role == "admin"

    var showDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<Depot?>(null) }
    var itemToDelete by remember { mutableStateOf<Depot?>(null) }

    val clientMap = clientsList.associateBy { it.id }

    val filteredList = list.filter {
        it.nom.contains(search, ignoreCase = true) || it.code.contains(search, ignoreCase = true)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Gestion des Dépôts", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Emplacements & Entrepôts logistiques", fontSize = 11.sp, color = Color.Gray)
            }
            Surface(
                color = StockOrange.copy(0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${filteredList.size} dépôts",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockOrange,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { viewModel.searchDepot.value = it },
                placeholder = { Text("Rechercher par Nom ou Code...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f).height(44.dp).testTag("depot_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            if (isAdmin && clientsList.isNotEmpty()) {
                Button(
                    onClick = { itemToEdit = null; showDialog = true },
                    modifier = Modifier.height(44.dp).testTag("add_depot_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = StockOrange),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Add, "Ajouter", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (clientsList.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StockRed.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Attention : Veuillez d'abord ajouter au moins un Client ! Un dépôt est obligatoirement lié à un client.", fontSize = 11.sp, color = StockRed, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
            }
        } else if (!isAdmin) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StockOrange.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mode Consultation : Seul un Administrateur peut enregistrer ou modifier des Dépôts.", fontSize = 11.sp, color = StockOrange, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.SemiBold)
            }
        }

        if (filteredList.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inbox, null, modifier = Modifier.size(36.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Aucun dépôt trouvé.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredList) { d ->
                    val relatedClient = clientMap[d.clientId]?.nom ?: "Inconnu"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = StockOrange.copy(0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(d.code, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StockOrange, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(d.nom, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Client rattaché : $relatedClient", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                if (d.responsable.isNotBlank()) {
                                    Text("Responsable : ${d.responsable}", fontSize = 11.sp, color = Color.Gray)
                                }
                                if (d.adresse.isNotBlank()) {
                                    Text("Adresse : ${d.adresse}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            if (isAdmin) {
                                Row {
                                    IconButton(onClick = { itemToEdit = d; showDialog = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, "Modifier", tint = StockBlueLight, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { itemToDelete = d }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, "Supprimer", tint = StockRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        DepotFormDialog(
            item = itemToEdit,
            clients = clientsList,
            onDismiss = { showDialog = false },
            onSave = { code, nom, clientId, adresse, resp ->
                if (itemToEdit == null) {
                    viewModel.addDepot(code, nom, clientId, adresse, resp)
                } else {
                    viewModel.updateDepot(itemToEdit!!.copy(code = code, nom = nom, clientId = clientId, adresse = adresse, responsable = resp))
                }
                showDialog = false
            }
        )
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Confirmer la suppression", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Voulez-vous supprimer le dépôt '${itemToDelete!!.nom}' ?", fontSize = 12.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDepot(itemToDelete!!)
                        itemToDelete = null
                    }
                ) { Text("Supprimer", color = StockRed, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Annuler", fontSize = 12.sp) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepotFormDialog(
    item: Depot?,
    clients: List<Client>,
    onDismiss: () -> Unit,
    onSave: (code: String, nom: String, clientId: Int, adresse: String, responsable: String) -> Unit
) {
    var code by remember { mutableStateOf(item?.code ?: "") }
    var nom by remember { mutableStateOf(item?.nom ?: "") }
    var selectedClientId by remember { mutableStateOf(item?.clientId ?: (clients.firstOrNull()?.id ?: 0)) }
    var adresse by remember { mutableStateOf(item?.adresse ?: "") }
    var responsable by remember { mutableStateOf(item?.responsable ?: "") }

    var expanded by remember { mutableStateOf(false) }
    val currentClientName = clients.find { it.id == selectedClientId }?.nom ?: "Sélectionner Client"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Créer un dépôt" else "Modifier le dépôt", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code Dépôt", fontSize = 11.sp) }, enabled = item == null, modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom dépôt", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))

                Text("Client associé :", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = currentClientName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth().height(46.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        clients.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.nom, fontSize = 12.sp) },
                                onClick = {
                                    selectedClientId = c.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(value = adresse, onValueChange = { adresse = it }, label = { Text("Adresse d'exploitation", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = responsable, onValueChange = { responsable = it }, label = { Text("Responsable logistique", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
            }
        },
        confirmButton = {
            Button(onClick = { onSave(code, nom, selectedClientId, adresse, responsable) }, enabled = code.isNotBlank() && nom.isNotBlank() && selectedClientId != 0, shape = RoundedCornerShape(8.dp)) {
                Text("Enregistrer", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", fontSize = 12.sp) }
        }
    )
}


// --- 4. ARTICLES SCREEN ---
@Composable
fun ArticlesScreen(viewModel: MainViewModel) {
    val list by viewModel.articles.collectAsState()
    val search by viewModel.searchArticle.collectAsState()
    val currentUser = viewModel.currentUser.collectAsState().value
    val isAdmin = currentUser?.role == "admin"

    var showDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<Article?>(null) }
    var itemToDelete by remember { mutableStateOf<Article?>(null) }

    val filteredList = list.filter {
        it.designation.contains(search, ignoreCase = true) || it.code.contains(search, ignoreCase = true) || it.categorie.contains(search, ignoreCase = true)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Gestion des Articles", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Catalogue Références, Moules & Unités", fontSize = 11.sp, color = Color.Gray)
            }
            Surface(
                color = StockBlueLight.copy(0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${filteredList.size} articles",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StockBlueLight,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { viewModel.searchArticle.value = it },
                placeholder = { Text("Désignation, Code ou Catégorie...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.weight(1f).height(44.dp).testTag("article_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            if (isAdmin) {
                Button(
                    onClick = { itemToEdit = null; showDialog = true },
                    modifier = Modifier.height(44.dp).testTag("add_article_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = StockBlueLight),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Add, "Ajouter", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (!isAdmin) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StockOrange.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mode Consultation : Seul un Administrateur peut modifier ou ajouter des articles.", fontSize = 11.sp, color = StockOrange, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.SemiBold)
            }
        }

        if (filteredList.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inbox, null, modifier = Modifier.size(36.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Aucun article répertorié.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredList) { a ->
                    val isUnderStock = a.stockActuel <= a.stockMinimum
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = StockBlueLight.copy(0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(a.code, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StockBlueLight, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(a.designation, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Catégorie : ${a.categorie} | Unité : ${a.unite}", fontSize = 11.sp, color = Color.Gray)
                                if (a.moule.isNotBlank() || a.qualite.isNotBlank()) {
                                    Text(
                                        text = "Moule : ${if (a.moule.isNotBlank()) a.moule else "—"} | Qualité : ${if (a.qualite.isNotBlank()) a.qualite else "—"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                    Text("Stock actuel : ", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        "${a.stockActuel}", 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 12.sp, 
                                        color = if (isUnderStock) StockRed else StockGreen
                                    )
                                    Text(" | Min : ${a.stockMinimum}", fontSize = 11.sp, color = Color.Gray)
                                    if (isUnderStock) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            color = StockRed.copy(0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("SOUS MIN", color = StockRed, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                            }

                            if (isAdmin) {
                                Row {
                                    IconButton(onClick = { itemToEdit = a; showDialog = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, "Modifier", tint = StockBlueLight, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { itemToDelete = a }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, "Supprimer", tint = StockRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ArticleFormDialog(
            item = itemToEdit,
            onDismiss = { showDialog = false },
            onSave = { code, designation, categorie, unite, stockMin, moule, qualite ->
                if (itemToEdit == null) {
                    viewModel.addArticle(code, designation, categorie, unite, stockMin, moule, qualite)
                } else {
                    viewModel.updateArticle(itemToEdit!!.copy(code = code, designation = designation, categorie = categorie, unite = unite, stockMinimum = stockMin, moule = moule, qualite = qualite))
                }
                showDialog = false
            }
        )
    }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Confirmer la suppression", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Voulez-vous supprimer l'article '${itemToDelete!!.designation}' ?", fontSize = 12.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteArticle(itemToDelete!!)
                        itemToDelete = null
                    }
                ) { Text("Supprimer", color = StockRed, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Annuler", fontSize = 12.sp) }
            }
        )
    }
}

@Composable
fun ArticleFormDialog(
    item: Article?,
    onDismiss: () -> Unit,
    onSave: (code: String, designation: String, categorie: String, unite: String, stockMin: Int, moule: String, qualite: String) -> Unit
) {
    var code by remember { mutableStateOf(item?.code ?: "") }
    var designation by remember { mutableStateOf(item?.designation ?: "") }
    var categorie by remember { mutableStateOf(item?.categorie ?: "") }
    var unite by remember { mutableStateOf(item?.unite ?: "unité") }
    var stockMinStr by remember { mutableStateOf(item?.stockMinimum?.toString() ?: "5") }
    var moule by remember { mutableStateOf(item?.moule ?: "") }
    var qualite by remember { mutableStateOf(item?.qualite ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Ajouter un article" else "Modifier l'article", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code Article", fontSize = 11.sp) }, enabled = item == null, modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = designation, onValueChange = { designation = it }, label = { Text("Désignation", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = categorie, onValueChange = { categorie = it }, label = { Text("Catégorie", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = moule, onValueChange = { moule = it }, label = { Text("Moule", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = qualite, onValueChange = { qualite = it }, label = { Text("Qualité", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = unite, onValueChange = { unite = it }, label = { Text("Unité", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
                OutlinedTextField(value = stockMinStr, onValueChange = { stockMinStr = it }, label = { Text("Stock Min", fontSize = 11.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().height(46.dp), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), shape = RoundedCornerShape(8.dp))
            }
        },
        confirmButton = {
            Button(onClick = { onSave(code, designation, categorie, unite, stockMinStr.toIntOrNull() ?: 0, moule, qualite) }, enabled = code.isNotBlank() && designation.isNotBlank(), shape = RoundedCornerShape(8.dp)) {
                Text("Enregistrer", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", fontSize = 12.sp) }
        }
    )
}

package com.example.ui.screens
 
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// --- 1. ENTRÉES STOCK ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntreesStockScreen(
    viewModel: MainViewModel,
    onTriggerPrint: (title: String, headers: List<String>, rows: List<List<String>>) -> Unit
) {
    val suppliers by viewModel.fournisseurs.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val depots by viewModel.depots.collectAsState()
    val articles by viewModel.articles.collectAsState()
    val currentUserByRole = viewModel.currentUser.collectAsState().value
    val isConsultant = currentUserByRole?.role == "consultation"

    var selectedSupplier by remember { mutableStateOf<Fournisseur?>(null) }
    var selectedClient by remember { mutableStateOf<Client?>(null) }
    var selectedDepot by remember { mutableStateOf<Depot?>(null) }
    var selectedArticle by remember { mutableStateOf<Article?>(null) }

    var quantiteStr by remember { mutableStateOf("") }
    var numeroLot by remember { mutableStateOf("") }
    var observations by remember { mutableStateOf("") }

    // Dropdowns visibility
    var expSupplier by remember { mutableStateOf(false) }
    var expClient by remember { mutableStateOf(false) }
    var expDepot by remember { mutableStateOf(false) }
    var expArticle by remember { mutableStateOf(false) }

    // Dynamic Depot Filtering based on selected client
    val availableDepots = depots.filter { it.clientId == selectedClient?.id }

    // Reset depot when client changes
    LaunchedEffect(selectedClient) {
        selectedDepot = null
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).testTag("entree_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- COMPACT HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Entrée de Stock", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Réception & Achat de marchandises", fontSize = 11.sp, color = Color.Gray)
            }
            Surface(
                color = StockGreen.copy(0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(12.dp), tint = StockGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nouvel Arrivage", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StockGreen)
                }
            }
        }

        if (isConsultant) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StockRed.copy(0.12f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Accès Refusé. Le profil consultant ne possède pas les permissions d'écrire ou de modifier les stocks.",
                    fontSize = 11.sp, color = StockRed, modifier = Modifier.padding(10.dp), fontWeight = FontWeight.SemiBold
                )
            }
        } else if (clients.isEmpty() || articles.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StockOrange.copy(0.12f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Attention : Veuillez vous assurer d'avoir enregistré au moins un Client, un Dépôt rattaché, et un Article avant d'enregistrer des entrées.",
                    fontSize = 11.sp, color = StockOrange, modifier = Modifier.padding(10.dp), fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    
                    // 1. Client & Depot side-by-side
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("1. Client *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            ExposedDropdownMenuBox(expanded = expClient, onExpandedChange = { expClient = !expClient }) {
                                OutlinedTextField(
                                    value = selectedClient?.nom ?: "Client",
                                    onValueChange = {}, readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expClient) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth().height(44.dp),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                                ExposedDropdownMenu(expanded = expClient, onDismissRequest = { expClient = false }) {
                                    clients.forEach { c ->
                                        DropdownMenuItem(text = { Text(c.nom, fontSize = 12.sp) }, onClick = { selectedClient = c; expClient = false })
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("2. Dépôt Réception *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            ExposedDropdownMenuBox(
                                expanded = expDepot,
                                onExpandedChange = { if (selectedClient != null) expDepot = !expDepot }
                            ) {
                                OutlinedTextField(
                                    value = selectedDepot?.nom ?: if (selectedClient == null) "Sélectionner Client" else "Dépôt",
                                    onValueChange = {}, readOnly = true,
                                    enabled = selectedClient != null,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expDepot) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth().height(44.dp),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                                ExposedDropdownMenu(expanded = expDepot, onDismissRequest = { expDepot = false }) {
                                    if (availableDepots.isEmpty()) {
                                        DropdownMenuItem(text = { Text("Aucun dépôt créé pour ce client", fontSize = 11.sp) }, onClick = {}, enabled = false)
                                    } else {
                                        availableDepots.forEach { d ->
                                            DropdownMenuItem(text = { Text(d.nom, fontSize = 12.sp) }, onClick = { selectedDepot = d; expDepot = false })
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Article Selection
                    Column {
                        Text("3. Article Réceptionné *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        ExposedDropdownMenuBox(expanded = expArticle, onExpandedChange = { expArticle = !expArticle }) {
                            OutlinedTextField(
                                value = selectedArticle?.designation ?: "Sélectionner Article",
                                onValueChange = {}, readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expArticle) },
                                modifier = Modifier.menuAnchor().fillMaxWidth().height(44.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                            ExposedDropdownMenu(expanded = expArticle, onDismissRequest = { expArticle = false }) {
                                articles.forEach { a ->
                                    DropdownMenuItem(text = { Text("${a.designation} (${a.unite})", fontSize = 12.sp) }, onClick = { selectedArticle = a; expArticle = false })
                                }
                            }
                        }
                    }

                    // 4. Quantity & Lot
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantiteStr,
                            onValueChange = { quantiteStr = it },
                            label = { Text("Quantité *", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).height(48.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                        OutlinedTextField(
                            value = numeroLot,
                            onValueChange = { numeroLot = it },
                            label = { Text("Numéro Lot", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                    }

                    OutlinedTextField(
                        value = observations,
                        onValueChange = { observations = it },
                        label = { Text("Observations", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Submit action button
                    Button(
                        onClick = {
                            val qty = quantiteStr.toIntOrNull() ?: 0
                            if (qty <= 0) return@Button
                            val now = System.currentTimeMillis()

                            viewModel.enregistrerEntreeStock(
                                dateEntree = now,
                                fournisseurId = null,
                                clientId = selectedClient!!.id,
                                depotId = selectedDepot!!.id,
                                articleId = selectedArticle!!.id,
                                quantite = qty,
                                numeroLot = numeroLot.trim(),
                                observations = observations.trim(),
                                onSuccess = {
                                    val row = listOf(
                                        selectedClient?.nom ?: "",
                                        selectedDepot?.nom ?: "",
                                        selectedArticle?.designation ?: "",
                                        "$qty ${selectedArticle?.unite ?: ""}",
                                        numeroLot.ifEmpty { "N/A" }
                                    )
                                    onTriggerPrint(
                                        "BON D'ENTRÉE EN STOCK",
                                        listOf("Client", "Dépôt Réception", "Article", "Quantité", "Numéro de Lot"),
                                        listOf(row)
                                    )

                                    // Reset fields
                                    quantiteStr = ""
                                    numeroLot = ""
                                    observations = ""
                                    selectedSupplier = null
                                }
                            )
                        },
                        enabled = selectedClient != null && selectedDepot != null && selectedArticle != null && (quantiteStr.toIntOrNull() ?: 0) > 0,
                        modifier = Modifier.fillMaxWidth().height(42.dp).testTag("entree_submit_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = StockGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(" Enregistrer & Générer Bon", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// --- 2. SORTIES STOCK ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortiesStockScreen(
    viewModel: MainViewModel,
    onTriggerPrint: (title: String, headers: List<String>, rows: List<List<String>>) -> Unit
) {
    val clients by viewModel.clients.collectAsState()
    val depots by viewModel.depots.collectAsState()
    val articles by viewModel.articles.collectAsState()
    val stockDepots by viewModel.stockDepots.collectAsState()
    val currentUserByRole = viewModel.currentUser.collectAsState().value
    val isConsultant = currentUserByRole?.role == "consultation"

    var selectedClient by remember { mutableStateOf<Client?>(null) }
    var selectedDepot by remember { mutableStateOf<Depot?>(null) }
    var selectedArticle by remember { mutableStateOf<Article?>(null) }

    var quantiteStr by remember { mutableStateOf("") }
    var bonSortie by remember { mutableStateOf("") }
    var dateChargement by remember { mutableStateOf("") }
    var matricule by remember { mutableStateOf("") }
    var nomCariste by remember { mutableStateOf("") }
    var controle by remember { mutableStateOf("") }
    var observations by remember { mutableStateOf("") }

    var expClient by remember { mutableStateOf(false) }
    var expDepot by remember { mutableStateOf(false) }
    var expArticle by remember { mutableStateOf(false) }

    // Dépôts cascade filtering
    val availableDepots = depots.filter { it.clientId == selectedClient?.id }

    // Live display of stock level in that specific depot
    val stockInDepot = remember(selectedDepot, selectedArticle, stockDepots) {
        if (selectedDepot != null && selectedArticle != null) {
            stockDepots.find { it.depotId == selectedDepot!!.id && it.articleId == selectedArticle!!.id }?.quantite ?: 0
        } else {
            0
        }
    }

    LaunchedEffect(selectedClient) {
        selectedDepot = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp).testTag("sortie_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // --- COMPACT HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Sortie de Stock", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Expédition, Vente & Consommation", fontSize = 11.sp, color = Color.Gray)
                }
                Surface(
                    color = StockRed.copy(0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(12.dp), tint = StockRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prélèvement Stock", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StockRed)
                    }
                }
            }
        }

        if (isConsultant) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StockRed.copy(0.12f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Accès Refusé. Le profil consultant ne possède pas les permissions d'écrire ou de modifier les stocks.", fontSize = 11.sp, color = StockRed, modifier = Modifier.padding(10.dp), fontWeight = FontWeight.SemiBold)
                }
            }
        } else if (clients.isEmpty() || articles.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StockOrange.copy(0.12f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Attention : Veuillez vous assurer d'avoir des clients et des articles enregistrés.", fontSize = 11.sp, color = StockOrange, modifier = Modifier.padding(10.dp), fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            // Form Cards
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        
                        // Client & Depot row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("1. Client *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                ExposedDropdownMenuBox(expanded = expClient, onExpandedChange = { expClient = !expClient }) {
                                    OutlinedTextField(
                                        value = selectedClient?.nom ?: "Client",
                                        onValueChange = {}, readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expClient) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth().height(44.dp),
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                    )
                                    ExposedDropdownMenu(expanded = expClient, onDismissRequest = { expClient = false }) {
                                        clients.forEach { c ->
                                            DropdownMenuItem(text = { Text(c.nom, fontSize = 12.sp) }, onClick = { selectedClient = c; expClient = false })
                                        }
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("2. Dépôt Prélèvement *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                ExposedDropdownMenuBox(
                                    expanded = expDepot,
                                    onExpandedChange = { if (selectedClient != null) expDepot = !expDepot }
                                ) {
                                    OutlinedTextField(
                                        value = selectedDepot?.nom ?: if (selectedClient == null) "Sélectionner Client" else "Dépôt",
                                        onValueChange = {}, readOnly = true,
                                        enabled = selectedClient != null,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expDepot) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth().height(44.dp),
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                        shape = RoundedCornerShape(8.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                    )
                                    ExposedDropdownMenu(expanded = expDepot, onDismissRequest = { expDepot = false }) {
                                        if (availableDepots.isEmpty()) {
                                            DropdownMenuItem(text = { Text("Aucun dépôt disponible", fontSize = 11.sp) }, onClick = {}, enabled = false)
                                        } else {
                                            availableDepots.forEach { d ->
                                                DropdownMenuItem(text = { Text(d.nom, fontSize = 12.sp) }, onClick = { selectedDepot = d; expDepot = false })
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Article Selection
                        Column {
                            Text("3. Article *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            ExposedDropdownMenuBox(expanded = expArticle, onExpandedChange = { expArticle = !expArticle }) {
                                OutlinedTextField(
                                    value = selectedArticle?.designation ?: "Sélectionner Article",
                                    onValueChange = {}, readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expArticle) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth().height(44.dp),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                                ExposedDropdownMenu(expanded = expArticle, onDismissRequest = { expArticle = false }) {
                                    articles.forEach { a ->
                                        DropdownMenuItem(text = { Text("${a.designation} (${a.unite})", fontSize = 12.sp) }, onClick = { selectedArticle = a; expArticle = false })
                                    }
                                }
                            }
                        }

                        // Display active stock level indicator in selected depot
                        if (selectedDepot != null && selectedArticle != null) {
                            Surface(
                                color = if (stockInDepot > 0) StockGreen.copy(0.12f) else StockRed.copy(0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Disponible en dépôt :", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        "$stockInDepot ${selectedArticle!!.unite}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (stockInDepot > 0) StockGreen else StockRed
                                    )
                                }
                            }
                        }

                        // Quantity, Bon/Ref, Obs
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = quantiteStr,
                                onValueChange = { quantiteStr = it },
                                label = { Text("Quantité *", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).height(48.dp),
                                isError = (quantiteStr.toIntOrNull() ?: 0) > stockInDepot,
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                            OutlinedTextField(
                                value = bonSortie,
                                onValueChange = { bonSortie = it },
                                label = { Text("N° Bon / Facture", fontSize = 11.sp) },
                                placeholder = { Text("ex: BS-2026-001", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                        }

                        // Date de Chargement & Matricule
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = dateChargement,
                                onValueChange = { dateChargement = it },
                                label = { Text("Date Chargement", fontSize = 11.sp) },
                                placeholder = { Text("ex: 30/06/2026", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                            OutlinedTextField(
                                value = matricule,
                                onValueChange = { matricule = it },
                                label = { Text("Matricule Camion", fontSize = 11.sp) },
                                placeholder = { Text("ex: 12-A-3456", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                        }

                        // Nom Cariste & Contrôle
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = nomCariste,
                                onValueChange = { nomCariste = it },
                                label = { Text("Nom Cariste", fontSize = 11.sp) },
                                placeholder = { Text("ex: Ahmed", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                            OutlinedTextField(
                                value = controle,
                                onValueChange = { controle = it },
                                label = { Text("Contrôle Qualité", fontSize = 11.sp) },
                                placeholder = { Text("ex: Conforme", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                        }

                        if (quantiteStr.isNotEmpty() && (quantiteStr.toIntOrNull() ?: 0) > stockInDepot) {
                            Text(
                                text = "Quantité supérieure au stock disponible ($stockInDepot) !",
                                color = StockRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = observations,
                            onValueChange = { observations = it },
                            label = { Text("Observations", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            shape = RoundedCornerShape(8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val qty = quantiteStr.toIntOrNull() ?: 0
                                if (qty <= 0 || qty > stockInDepot) return@Button
                                val now = System.currentTimeMillis()

                                viewModel.enregistrerSortieStock(
                                    dateSortie = now,
                                    clientId = selectedClient!!.id,
                                    depotId = selectedDepot!!.id,
                                    articleId = selectedArticle!!.id,
                                    quantite = qty,
                                    bonSortie = bonSortie.trim(),
                                    observations = observations.trim(),
                                    dateChargement = dateChargement.trim(),
                                    matricule = matricule.trim(),
                                    nomCariste = nomCariste.trim(),
                                    controle = controle.trim(),
                                    onSuccess = {
                                        val row = listOf(
                                            selectedClient?.nom ?: "",
                                            selectedDepot?.nom ?: "",
                                            selectedArticle?.designation ?: "",
                                            "$qty ${selectedArticle?.unite ?: ""}",
                                            bonSortie.ifEmpty { "N/A" },
                                            dateChargement.ifEmpty { "N/A" },
                                            matricule.ifEmpty { "N/A" },
                                            nomCariste.ifEmpty { "N/A" },
                                            controle.ifEmpty { "N/A" }
                                        )
                                        onTriggerPrint(
                                            "BON DE SORTIE DE STOCK CONSOLIDÉ",
                                            listOf("Client", "Dépôt", "Article", "Quantité", "Réf/Bon", "Date Charg.", "Matricule", "Cariste", "Contrôle"),
                                            listOf(row)
                                        )

                                        // Reset
                                        quantiteStr = ""
                                        bonSortie = ""
                                        observations = ""
                                        dateChargement = ""
                                        matricule = ""
                                        nomCariste = ""
                                        controle = ""
                                    }
                                )
                            },
                            enabled = selectedClient != null && selectedDepot != null && selectedArticle != null && (quantiteStr.toIntOrNull() ?: 0) > 0 && (quantiteStr.toIntOrNull() ?: 0) <= stockInDepot,
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("sortie_submit_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = StockRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Upload, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(" Déduire du Stock & Imprimer Bon", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


// --- 3. CONSULTATION STOCK ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationStockScreen(
    viewModel: MainViewModel,
    onTriggerPrint: ((title: String, headers: List<String>, rows: List<List<String>>) -> Unit)? = null
) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState()
    val depots by viewModel.depots.collectAsState()
    val articles by viewModel.articles.collectAsState()
    val stockDepots by viewModel.stockDepots.collectAsState()

    val currentUser by viewModel.currentUser.collectAsState()
    val assignedDepotId = currentUser?.assignedDepotId

    // Filters
    val selectedClient by viewModel.selectedConsultClient.collectAsState()
    val selectedDepot by viewModel.selectedConsultDepot.collectAsState()
    val selectedArticle by viewModel.selectedConsultArticle.collectAsState()
    val searchText by viewModel.queryConsultSearch.collectAsState()

    var isTableView by remember { mutableStateOf(false) }

    var expClient by remember { mutableStateOf(false) }
    var expDepot by remember { mutableStateOf(false) }
    var expArticle by remember { mutableStateOf(false) }

    val clientsMap = clients.associateBy { it.id }
    val depotsMap = depots.associateBy { it.id }
    val articlesMap = articles.associateBy { it.id }

    val activeClientsForFilter = clients.filter { c ->
        if (assignedDepotId != null) {
            val assignedDepotObj = depots.find { it.id == assignedDepotId }
            assignedDepotObj?.clientId == c.id
        } else {
            true
        }
    }

    val activeDepotsForFilter = depots.filter { d -> 
        val clientCondition = selectedClient == null || d.clientId == selectedClient!!.id
        val assignedCondition = assignedDepotId == null || d.id == assignedDepotId
        clientCondition && assignedCondition
    }

    // Double check cascade & auto pre-select assigned depot
    LaunchedEffect(selectedClient, assignedDepotId, depots) {
        if (assignedDepotId != null) {
            val assignedDepotObj = depots.find { it.id == assignedDepotId }
            if (assignedDepotObj != null) {
                viewModel.selectedConsultDepot.value = assignedDepotObj
                val clientObj = clients.find { it.id == assignedDepotObj.clientId }
                if (clientObj != null) {
                    viewModel.selectedConsultClient.value = clientObj
                }
            }
        } else if (selectedClient != null && selectedDepot != null && selectedDepot!!.clientId != selectedClient!!.id) {
            viewModel.selectedConsultDepot.value = null
        }
    }

    // Filter results based on conditions
    val filteredStocks = stockDepots.filter { item ->
        if (assignedDepotId != null && item.depotId != assignedDepotId) {
            return@filter false
        }

        val clientMatch = selectedClient == null || (depotsMap[item.depotId]?.clientId == selectedClient!!.id)
        val depotMatch = selectedDepot == null || item.depotId == selectedDepot!!.id
        val articleMatch = selectedArticle == null || item.articleId == selectedArticle!!.id

        val articleDesignation = articlesMap[item.articleId]?.designation ?: ""
        val depotName = depotsMap[item.depotId]?.nom ?: ""
        val textMatch = searchText.isEmpty() || articleDesignation.contains(searchText, true) || depotName.contains(searchText, true)

        item.quantite >= 0 && clientMatch && depotMatch && articleMatch && textMatch
    }

    // Calculated summary stats
    val totalVolume = filteredStocks.sumOf { it.quantite }
    val totalAlertes = filteredStocks.count { item ->
        val art = articlesMap[item.articleId]
        art != null && item.quantite <= art.stockMinimum
    }
    val hasActiveFilter = selectedClient != null || selectedDepot != null || selectedArticle != null || searchText.isNotBlank()

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        
        // --- COMPACT HEADER WITH COMPACT ACTION BUTTONS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Consultation Stock", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (assignedDepotId != null) {
                        val dNom = depots.find { it.id == assignedDepotId }?.nom ?: "votre dépôt"
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = StockBlue.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = dNom,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StockBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text("État des volumes par dépôt & client", fontSize = 11.sp, color = Color.Gray)
            }

            // Compact Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (hasActiveFilter) {
                    IconButton(
                        onClick = {
                            viewModel.queryConsultSearch.value = ""
                            viewModel.selectedConsultClient.value = null
                            viewModel.selectedConsultDepot.value = null
                            viewModel.selectedConsultArticle.value = null
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Réinitialiser", tint = StockRed, modifier = Modifier.size(16.dp))
                    }
                }

                OutlinedButton(
                    onClick = {
                        val shareText = buildString {
                            append("📊 *ÉTAT DE STOCK - GESTISTOCK*\n")
                            append("Nombre de références : ${filteredStocks.size}\n")
                            append("Volume total : $totalVolume\n\n")
                            filteredStocks.take(20).forEach { item ->
                                val art = articlesMap[item.articleId]
                                val dep = depotsMap[item.depotId]
                                append("• ${art?.designation ?: "Art"}: ${item.quantite} ${art?.unite ?: ""} (${dep?.nom ?: "Dépôt"})\n")
                            }
                        }
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(intent, "Partager État Stock"))
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = StockGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Partager", fontSize = 11.sp, color = StockGreen)
                }

                if (onTriggerPrint != null) {
                    Button(
                        onClick = {
                            val printRows = filteredStocks.map { item ->
                                val art = articlesMap[item.articleId]
                                val dep = depotsMap[item.depotId]
                                val cli = dep?.let { clientsMap[it.clientId] }
                                listOf(
                                    art?.designation ?: "Inconnu",
                                    dep?.nom ?: "Inconnu",
                                    cli?.nom ?: "Inconnu",
                                    "${art?.stockMinimum ?: 0}",
                                    "${item.quantite} ${art?.unite ?: ""}"
                                )
                            }
                            onTriggerPrint(
                                "CONSULTATION DES STOCKS GÉNÉRAUX",
                                listOf("Article", "Dépôt", "Client", "Stock Min", "Quantité En Stock"),
                                printRows
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StockBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp).testTag("print_consultation_btn")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Imprimer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- KPI METRICS & VIEW MODE TOGGLE ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // KPI Badges
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(12.dp), tint = StockBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${filteredStocks.size} Ref(s)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    color = StockGreen.copy(0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(12.dp), tint = StockGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Vol: $totalVolume", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StockGreen)
                    }
                }

                if (totalAlertes > 0) {
                    Surface(
                        color = StockRed.copy(0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(12.dp), tint = StockRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$totalAlertes Alerte(s)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StockRed)
                        }
                    }
                }
            }

            // View mode switch (Table vs Card)
            FilterChip(
                selected = isTableView,
                onClick = { isTableView = !isTableView },
                label = { Text(if (isTableView) "Tableau" else "Cartes", fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = if (isTableView) Icons.Default.TableChart else Icons.Default.ViewAgenda,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                modifier = Modifier.height(28.dp)
            )
        }

        // --- COMPACT FILTERS CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                
                // Search field (compact)
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { viewModel.queryConsultSearch.value = it },
                    placeholder = { Text("Rechercher article ou dépôt...", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.queryConsultSearch.value = "" }) {
                                Icon(Icons.Default.Clear, null, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                )

                // 3 Compact Dropdowns in 1 Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Client filter
                    Column(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(
                            expanded = if (assignedDepotId != null) false else expClient,
                            onExpandedChange = { if (assignedDepotId == null) expClient = !expClient }
                        ) {
                            OutlinedTextField(
                                value = selectedClient?.nom ?: if (assignedDepotId != null) (activeClientsForFilter.firstOrNull()?.nom ?: "Client") else "Tous Clients",
                                onValueChange = {}, readOnly = true,
                                trailingIcon = { if (assignedDepotId == null) ExposedDropdownMenuDefaults.TrailingIcon(expClient) },
                                modifier = Modifier.menuAnchor().fillMaxWidth().height(44.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = RoundedCornerShape(8.dp),
                                enabled = assignedDepotId == null,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                            )
                            if (assignedDepotId == null) {
                                ExposedDropdownMenu(expanded = expClient, onDismissRequest = { expClient = false }) {
                                    DropdownMenuItem(text = { Text("Tous les clients", fontSize = 12.sp) }, onClick = { viewModel.selectedConsultClient.value = null; expClient = false })
                                    activeClientsForFilter.forEach { c ->
                                        DropdownMenuItem(text = { Text(c.nom, fontSize = 12.sp) }, onClick = { viewModel.selectedConsultClient.value = c; expClient = false })
                                    }
                                }
                            }
                        }
                    }

                    // Depot filter
                    Column(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(
                            expanded = if (assignedDepotId != null) false else expDepot,
                            onExpandedChange = { if (assignedDepotId == null) expDepot = !expDepot }
                        ) {
                            OutlinedTextField(
                                value = selectedDepot?.nom ?: if (assignedDepotId != null) "Dépôt" else "Tous Dépôts",
                                onValueChange = {}, readOnly = true,
                                trailingIcon = { if (assignedDepotId == null) ExposedDropdownMenuDefaults.TrailingIcon(expDepot) },
                                modifier = Modifier.menuAnchor().fillMaxWidth().height(44.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = RoundedCornerShape(8.dp),
                                enabled = assignedDepotId == null,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                            )
                            if (assignedDepotId == null) {
                                ExposedDropdownMenu(expanded = expDepot, onDismissRequest = { expDepot = false }) {
                                    DropdownMenuItem(text = { Text("Tous les dépôts", fontSize = 12.sp) }, onClick = { viewModel.selectedConsultDepot.value = null; expDepot = false })
                                    activeDepotsForFilter.forEach { d ->
                                        DropdownMenuItem(text = { Text(d.nom, fontSize = 12.sp) }, onClick = { viewModel.selectedConsultDepot.value = d; expDepot = false })
                                    }
                                }
                            }
                        }
                    }

                    // Article filter
                    Column(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(expanded = expArticle, onExpandedChange = { expArticle = !expArticle }) {
                            OutlinedTextField(
                                value = selectedArticle?.designation ?: "Tous Articles",
                                onValueChange = {}, readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expArticle) },
                                modifier = Modifier.menuAnchor().fillMaxWidth().height(44.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                            )
                            ExposedDropdownMenu(expanded = expArticle, onDismissRequest = { expArticle = false }) {
                                DropdownMenuItem(text = { Text("Tous les articles", fontSize = 12.sp) }, onClick = { viewModel.selectedConsultArticle.value = null; expArticle = false })
                                articles.forEach { a ->
                                    DropdownMenuItem(text = { Text(a.designation, fontSize = 12.sp) }, onClick = { viewModel.selectedConsultArticle.value = a; expArticle = false })
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DATA DISPLAY SECTION (TABLE OR CARDS) ---
        if (filteredStocks.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inbox, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Aucun stock correspondant aux filtres actifs", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else if (isTableView) {
            // --- DENSE COMPACT TABLE VIEW ---
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Table Header
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ARTICLE", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2f))
                            Text("DÉPÔT", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                            Text("CLIENT", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                            Text("STOCK", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredStocks) { item ->
                            val articleObj = articlesMap[item.articleId]
                            val depotObj = depotsMap[item.depotId]
                            val clientObj = depotObj?.let { clientsMap[it.clientId] }
                            val isLowStock = articleObj != null && item.quantite <= articleObj.stockMinimum

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isLowStock) StockOrange.copy(0.08f) else Color.Transparent)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(articleObj?.designation ?: "Art #${item.articleId}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    if (isLowStock) {
                                        Text("⚠️ Stock Min (${articleObj?.stockMinimum})", fontSize = 9.sp, color = StockOrange, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(depotObj?.nom ?: "Inconnu", fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.weight(1.5f))
                                Text(clientObj?.nom ?: "Inconnu", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1.5f))

                                Surface(
                                    color = if (isLowStock) StockOrange.copy(0.2f) else StockGreen.copy(0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${item.quantite} ${articleObj?.unite ?: ""}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isLowStock) StockOrange else StockGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                        }
                    }
                }
            }
        } else {
            // --- COMPACT CARDS VIEW ---
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredStocks) { item ->
                    val articleObj = articlesMap[item.articleId]
                    val depotObj = depotsMap[item.depotId]
                    val clientObj = depotObj?.let { clientsMap[it.clientId] }
                    val isLowStock = articleObj != null && item.quantite <= articleObj.stockMinimum

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    articleObj?.designation ?: "Article #${item.articleId}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HomeWork, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("${depotObj?.nom ?: "Dépôt"}", fontSize = 11.sp, color = Color.DarkGray)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("${clientObj?.nom ?: "Client"}", fontSize = 11.sp, color = Color.Gray)
                                }

                                if (isLowStock) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Surface(
                                        color = StockOrange.copy(0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "ALERTE RESSERRE : <= Min (${articleObj?.stockMinimum})",
                                            color = StockOrange,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = if (isLowStock) StockRed.copy(0.12f) else StockGreen.copy(0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${item.quantite} ${articleObj?.unite ?: ""}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (isLowStock) StockRed else StockGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- WHATSAPP SCHEDULE & SHARING HELPERS ---
data class WhatsAppSchedule(
    val id: String,
    val depotName: String,
    val phoneNumber: String,
    val frequency: String,
    val time: String,
    val isActive: Boolean
)

fun loadWhatsAppSchedules(context: Context): List<WhatsAppSchedule> {
    val prefs = context.getSharedPreferences("gestistock_whatsapp", Context.MODE_PRIVATE)
    val jsonStr = prefs.getString("schedules", "[]") ?: "[]"
    val list = mutableListOf<WhatsAppSchedule>()
    try {
        val arr = JSONArray(jsonStr)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                WhatsAppSchedule(
                    id = obj.getString("id"),
                    depotName = obj.getString("depotName"),
                    phoneNumber = obj.getString("phoneNumber"),
                    frequency = obj.getString("frequency"),
                    time = obj.getString("time"),
                    isActive = obj.getBoolean("isActive")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

fun saveWhatsAppSchedules(context: Context, list: List<WhatsAppSchedule>) {
    val prefs = context.getSharedPreferences("gestistock_whatsapp", Context.MODE_PRIVATE)
    val arr = JSONArray()
    list.forEach { item ->
        val obj = JSONObject().apply {
            put("id", item.id)
            put("depotName", item.depotName)
            put("phoneNumber", item.phoneNumber)
            put("frequency", item.frequency)
            put("time", item.time)
            put("isActive", item.isActive)
        }
        arr.put(obj)
    }
    prefs.edit().putString("schedules", arr.toString()).apply()
}

fun shareDepotStockOnWhatsApp(context: Context, depotName: String, rows: List<StockFinalViewRow>, targetPhone: String = "") {
    val stringBuilder = StringBuilder()
    stringBuilder.append("📊 *Rapport d'État de Stock* \n")
    stringBuilder.append("🏭 *Dépôt : ${depotName.uppercase()}*\n")
    val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
    stringBuilder.append("📅 Généré le : $nowStr\n")
    stringBuilder.append("-------------------------------------------\n\n")
    
    val depotRows = rows.filter { it.depotName.equals(depotName, ignoreCase = true) }
    
    if (depotRows.isEmpty()) {
        stringBuilder.append("Aucun article en stock pour le moment.\n")
    } else {
        depotRows.forEach { row ->
            stringBuilder.append("📦 *${row.articleName}*\n")
            stringBuilder.append("   • Réf : `${row.articleCode}`\n")
            if (row.moule.isNotBlank()) stringBuilder.append("   • Moule : ${row.moule}\n")
            if (row.qualite.isNotBlank()) stringBuilder.append("   • Qualité : ${row.qualite}\n")
            stringBuilder.append("   • Entrées : ${row.quantiteEntree} ${row.unite}\n")
            stringBuilder.append("   • Sorties : ${row.quantiteSortie} ${row.unite}\n")
            stringBuilder.append("   • Stock Final : *${row.quantiteFinale} ${row.unite}*\n\n")
        }
    }
    stringBuilder.append("-------------------------------------------\n")
    stringBuilder.append("📱 _GestiStock - Gestion de Stock Intelligente_")

    val message = stringBuilder.toString()
    val uriString = if (targetPhone.isNotBlank()) {
        val cleanedPhone = targetPhone.replace("+", "").replace(" ", "").trim()
        "https://api.whatsapp.com/send?phone=$cleanedPhone&text=" + Uri.encode(message)
    } else {
        "https://api.whatsapp.com/send?text=" + Uri.encode(message)
    }
    
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(uriString)
    }
    
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Partager avec"))
    }
}

// --- 4. STOCK FINAL SCREEN ---
data class StockADateRow(
    val clientName: String,
    val depotName: String,
    val articleCode: String,
    val articleName: String,
    val moule: String,
    val qualite: String,
    val supplierName: String,
    val quantiteEntree: Int,
    val quantiteSortie: Int,
    val quantiteStockAtDate: Int,
    val unite: String,
    val articleId: Int,
    val depotId: Int,
    val clientId: Int?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockFinalScreen(
    viewModel: MainViewModel,
    onTriggerPrint: (title: String, headers: List<String>, rows: List<List<String>>) -> Unit
) {
    val clients by viewModel.clients.collectAsState()
    val depots by viewModel.depots.collectAsState()
    val articles by viewModel.articles.collectAsState()
    val stockDepots by viewModel.stockDepots.collectAsState()
    val entrees by viewModel.entrees.collectAsState()
    val sorties by viewModel.sorties.collectAsState()
    val fournisseurs by viewModel.fournisseurs.collectAsState()

    val clientsMap = clients.associateBy { it.id }
    val depotsMap = depots.associateBy { it.id }
    val articlesMap = articles.associateBy { it.id }
    val fournisseursMap = fournisseurs.associateBy { it.id }

    val context = LocalContext.current

    // Mode Selector: 0 = Stock Actuel (Temps Réel), 1 = Stock à Date
    var activeTabMode by remember { mutableStateOf(0) }

    var showWhatsAppPanel by remember { mutableStateOf(false) }
    var showAdvancedFilters by remember { mutableStateOf(false) }
    
    // For WhatsApp scheduling
    var schedules by remember { mutableStateOf(loadWhatsAppSchedules(context)) }
    var selectedDepotForSchedule by remember { mutableStateOf<Depot?>(null) }
    var depotScheduleDropdownExpanded by remember { mutableStateOf(false) }
    var schedulePhoneNumber by remember { mutableStateOf("") }
    var scheduleFrequency by remember { mutableStateOf("Tous les jours") }
    var scheduleFrequencyDropdownExpanded by remember { mutableStateOf(false) }
    var scheduleTime by remember { mutableStateOf("18:00") }

    // Common filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedClientFilter by remember { mutableStateOf<Client?>(null) }
    var selectedDepotFilter by remember { mutableStateOf<Depot?>(null) }
    var clientDropdownExpanded by remember { mutableStateOf(false) }
    var depotDropdownExpanded by remember { mutableStateOf(false) }

    // Stock à Date specific filter states
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedFournisseurFilter by remember { mutableStateOf<Fournisseur?>(null) }
    var selectedArticleFilter by remember { mutableStateOf<Article?>(null) }
    var selectedMouleFilter by remember { mutableStateOf<String?>(null) }

    var fournisseurDropdownExpanded by remember { mutableStateOf(false) }
    var articleDropdownExpanded by remember { mutableStateOf(false) }
    var mouleDropdownExpanded by remember { mutableStateOf(false) }

    val availableMoules = remember(articles) {
        articles.map { it.moule }.filter { it.isNotBlank() }.distinct()
    }

    // Date Picker Dialog setup
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newCal = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 23, 59, 59)
            }
            selectedDateMillis = newCal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val formattedSelectedDate = remember(selectedDateMillis) {
        SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(selectedDateMillis))
    }

    // --- MODE 0: STOCK ACTUEL CALCULATION ---
    val resultsRealtime = stockDepots.map { item ->
        val dep = depotsMap[item.depotId]
        val cli = dep?.let { clientsMap[it.clientId] }
        val art = articlesMap[item.articleId]
        
        val totalEntree = entrees.filter { it.articleId == item.articleId && it.depotId == item.depotId }.sumOf { it.quantite }
        val totalSortie = sorties.filter { it.articleId == item.articleId && it.depotId == item.depotId }.sumOf { it.quantite }

        StockFinalViewRow(
            clientName = cli?.nom ?: "Inconnu",
            depotName = dep?.nom ?: "Inconnu",
            articleCode = art?.code ?: "Inconnu",
            articleName = art?.designation ?: "Inconnu",
            moule = art?.moule ?: "",
            qualite = art?.qualite ?: "",
            quantiteEntree = totalEntree,
            quantiteSortie = totalSortie,
            quantiteFinale = item.quantite,
            unite = art?.unite ?: "unité"
        )
    }.filter { it.quantiteFinale >= 0 }

    val filteredRealtimeResults = resultsRealtime.filter { row ->
        val matchesSearch = searchQuery.isBlank() || 
                row.articleName.contains(searchQuery, ignoreCase = true) ||
                row.articleCode.contains(searchQuery, ignoreCase = true) ||
                row.moule.contains(searchQuery, ignoreCase = true) ||
                row.qualite.contains(searchQuery, ignoreCase = true) ||
                row.clientName.contains(searchQuery, ignoreCase = true) ||
                row.depotName.contains(searchQuery, ignoreCase = true)
        
        val matchesClient = selectedClientFilter == null || row.clientName == selectedClientFilter?.nom
        val matchesDepot = selectedDepotFilter == null || row.depotName == selectedDepotFilter?.nom
        
        matchesSearch && matchesClient && matchesDepot
    }

    val totalEntreesGlobalRealtime = filteredRealtimeResults.sumOf { it.quantiteEntree }
    val totalSortiesGlobalRealtime = filteredRealtimeResults.sumOf { it.quantiteSortie }
    val totalStockGlobalRealtime = filteredRealtimeResults.sumOf { it.quantiteFinale }

    // --- MODE 1: STOCK À DATE CALCULATION ---
    val targetEndTimestamp = remember(selectedDateMillis) {
        Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    val allDepotArticlePairs = remember(stockDepots, entrees, sorties) {
        (stockDepots.map { Pair(it.depotId, it.articleId) } +
                entrees.map { Pair(it.depotId, it.articleId) } +
                sorties.map { Pair(it.depotId, it.articleId) }).distinct()
    }

    val stockADateResults = remember(
        allDepotArticlePairs, entrees, sorties, targetEndTimestamp,
        selectedFournisseurFilter, selectedArticleFilter, selectedMouleFilter,
        selectedClientFilter, selectedDepotFilter, searchQuery,
        articlesMap, depotsMap, clientsMap, fournisseursMap
    ) {
        allDepotArticlePairs.mapNotNull { (depotId, articleId) ->
            val dep = depotsMap[depotId]
            val cli = dep?.let { clientsMap[it.clientId] }
            val art = articlesMap[articleId] ?: return@mapNotNull null

            val itemEntrees = entrees.filter { e ->
                e.depotId == depotId &&
                e.articleId == articleId &&
                e.dateEntree <= targetEndTimestamp &&
                (selectedFournisseurFilter == null || e.fournisseurId == selectedFournisseurFilter?.id)
            }

            val itemSorties = sorties.filter { s ->
                s.depotId == depotId &&
                s.articleId == articleId &&
                s.dateSortie <= targetEndTimestamp
            }

            val totalEntreesAtDate = itemEntrees.sumOf { it.quantite }
            val totalSortiesAtDate = itemSorties.sumOf { it.quantite }
            val stockAtDate = totalEntreesAtDate - totalSortiesAtDate

            val supplierNames = itemEntrees.mapNotNull { e -> e.fournisseurId?.let { fournisseursMap[it]?.nom } }.distinct().joinToString(", ")

            StockADateRow(
                clientName = cli?.nom ?: "Inconnu",
                depotName = dep?.nom ?: "Inconnu",
                articleCode = art.code,
                articleName = art.designation,
                moule = art.moule,
                qualite = art.qualite,
                supplierName = if (supplierNames.isNotBlank()) supplierNames else "—",
                quantiteEntree = totalEntreesAtDate,
                quantiteSortie = totalSortiesAtDate,
                quantiteStockAtDate = stockAtDate,
                unite = art.unite,
                articleId = art.id,
                depotId = depotId,
                clientId = cli?.id
            )
        }.filter { row ->
            val matchesSearch = searchQuery.isBlank() ||
                    row.articleName.contains(searchQuery, ignoreCase = true) ||
                    row.articleCode.contains(searchQuery, ignoreCase = true) ||
                    row.moule.contains(searchQuery, ignoreCase = true) ||
                    row.qualite.contains(searchQuery, ignoreCase = true) ||
                    row.clientName.contains(searchQuery, ignoreCase = true) ||
                    row.supplierName.contains(searchQuery, ignoreCase = true) ||
                    row.depotName.contains(searchQuery, ignoreCase = true)

            val matchesFournisseur = selectedFournisseurFilter == null || row.supplierName.contains(selectedFournisseurFilter!!.nom, ignoreCase = true)
            val matchesArticle = selectedArticleFilter == null || row.articleId == selectedArticleFilter!!.id
            val matchesMoule = selectedMouleFilter == null || row.moule.equals(selectedMouleFilter, ignoreCase = true)
            val matchesClient = selectedClientFilter == null || row.clientId == selectedClientFilter!!.id
            val matchesDepot = selectedDepotFilter == null || row.depotId == selectedDepotFilter!!.id

            matchesSearch && matchesFournisseur && matchesArticle && matchesMoule && matchesClient && matchesDepot &&
                    !(row.quantiteEntree == 0 && row.quantiteSortie == 0)
        }
    }

    val totalEntreesADateGlobal = stockADateResults.sumOf { it.quantiteEntree }
    val totalSortiesADateGlobal = stockADateResults.sumOf { it.quantiteSortie }
    val totalStockADateGlobal = stockADateResults.sumOf { it.quantiteStockAtDate }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- 1. BRANDING ERP TOP BANNER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Slate800)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = null,
                    tint = StockCyanAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "REALX ERP",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(StockGreen.copy(0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("MODULE STOCKS", color = StockGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "v2.5.4",
                color = Color.White.copy(0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // --- 2. HEADER TITLE & ACTIONS TOOLBAR ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "État de Stock Général",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900
                )
                Text(
                    text = "Consultation instantanée des volumes par client & dépôt",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // WhatsApp options toggle
                Button(
                    onClick = { showWhatsAppPanel = !showWhatsAppPanel },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showWhatsAppPanel) StockGreen.copy(0.12f) else Slate100,
                        contentColor = if (showWhatsAppPanel) StockGreen else OnSlate100
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Share, "WhatsApp", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Envois", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Print Trigger
                Button(
                    onClick = {
                        if (activeTabMode == 0) {
                            val printRows = filteredRealtimeResults.map { 
                                listOf(
                                    it.articleName,
                                    if (it.moule.isNotBlank()) it.moule else "—",
                                    if (it.qualite.isNotBlank()) it.qualite else "—",
                                    it.clientName,
                                    "${it.quantiteEntree} ${it.unite}",
                                    "${it.quantiteSortie} ${it.unite}",
                                    "${it.quantiteFinale} ${it.unite}"
                                ) 
                            }
                            onTriggerPrint(
                                "ÉTAT DE STOCK GÉNÉRAL (TEMPS RÉEL)",
                                listOf("Article", "Moule", "Qualité", "Client", "Qté Entrée", "Qté Sortie", "Stock Final"),
                                printRows
                            )
                        } else {
                            val printRows = stockADateResults.map { 
                                listOf(
                                    it.articleName,
                                    if (it.moule.isNotBlank()) it.moule else "—",
                                    it.supplierName,
                                    it.clientName,
                                    "${it.quantiteEntree} ${it.unite}",
                                    "${it.quantiteSortie} ${it.unite}",
                                    "${it.quantiteStockAtDate} ${it.unite}"
                                ) 
                            }
                            onTriggerPrint(
                                "STOCK À DATE : $formattedSelectedDate",
                                listOf("Article", "Moule", "Fournisseur", "Client", "Qté Entrée", "Qté Sortie", "Stock au $formattedSelectedDate"),
                                printRows
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate100,
                        contentColor = StockBlue
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Print, "Imprimer", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Imprimer", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- 3. PREMIUM TABS / SEGMENTED CONTROL ---
        Surface(
            color = Slate100,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(3.dp).fillMaxWidth()) {
                Button(
                    onClick = { activeTabMode = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTabMode == 0) StockBlue else Color.Transparent,
                        contentColor = if (activeTabMode == 0) Color.White else OnSlate100
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Storage, null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stock Actuel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { activeTabMode = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTabMode == 1) StockBlue else Color.Transparent,
                        contentColor = if (activeTabMode == 1) Color.White else OnSlate100
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Event, null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stock à Date", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- 4. DYNAMIC DASHBOARD KPI METRICS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Entrées KPI Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate50),
                modifier = Modifier
                    .weight(1f)
                    .border(0.5.dp, StockBlue.copy(0.2f), RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ENTRÉES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Icon(Icons.Default.ArrowDownward, null, tint = StockBlue, modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${if (activeTabMode == 0) totalEntreesGlobalRealtime else totalEntreesADateGlobal}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                    Text("unités reçues", fontSize = 8.sp, color = Color.Gray)
                }
            }

            // Sorties KPI Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate50),
                modifier = Modifier
                    .weight(1f)
                    .border(0.5.dp, StockOrange.copy(0.2f), RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SORTIES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Icon(Icons.Default.ArrowUpward, null, tint = StockOrange, modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${if (activeTabMode == 0) totalSortiesGlobalRealtime else totalSortiesADateGlobal}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate900
                    )
                    Text("unités livrées", fontSize = 8.sp, color = Color.Gray)
                }
            }

            // Solde Stock Restant Card
            Card(
                colors = CardDefaults.cardColors(containerColor = StockGreen.copy(0.06f)),
                modifier = Modifier
                    .weight(1.1f)
                    .border(1.2.dp, StockGreen.copy(0.25f), RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("STOCK NET", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = StockGreen)
                        Icon(Icons.Default.Inventory, null, tint = StockGreen, modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${if (activeTabMode == 0) totalStockGlobalRealtime else totalStockADateGlobal}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StockGreen
                    )
                    Text("disponible net", fontSize = 8.sp, color = Color.Gray)
                }
            }
        }

        // --- 5. COLLAPSIBLE WHATSAPP SCHEDULER PANEL ---
        AnimatedVisibility(
            visible = showWhatsAppPanel,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, StockGreen.copy(0.3f), RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StockGreen))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PARTAGE AUTOMATIQUE WHATSAPP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StockGreen)
                    }

                    Text("1. Partage Instantané par Dépôt :", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    if (depots.isEmpty()) {
                        Text("Aucun dépôt configuré.", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            depots.forEach { d ->
                                val clientName = clients.find { it.id == d.clientId }?.nom ?: "Inconnu"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Slate50)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(d.nom, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                        Text("Client : $clientName", fontSize = 9.sp, color = Color.Gray)
                                    }
                                    
                                    Button(
                                        onClick = { shareDepotStockOnWhatsApp(context, d.nom, resultsRealtime) },
                                        colors = ButtonDefaults.buttonColors(containerColor = StockGreen),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Icon(Icons.Default.Send, null, modifier = Modifier.size(10.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Envoyer", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.2f))

                    Text("2. Planifier un Rapport Périodique :", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Depot Selector
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { depotScheduleDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth().height(32.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            selectedDepotForSchedule?.nom ?: "Choisir dépôt",
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp))
                                    }
                                    DropdownMenu(
                                        expanded = depotScheduleDropdownExpanded,
                                        onDismissRequest = { depotScheduleDropdownExpanded = false }
                                    ) {
                                        depots.forEach { d ->
                                            DropdownMenuItem(
                                                text = { Text(d.nom, fontSize = 11.sp) },
                                                onClick = {
                                                    selectedDepotForSchedule = d
                                                    depotScheduleDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Phone input
                                OutlinedTextField(
                                    value = schedulePhoneNumber,
                                    onValueChange = { schedulePhoneNumber = it },
                                    placeholder = { Text("+212...", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1.2f).height(36.dp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(6.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Frequency
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { scheduleFrequencyDropdownExpanded = true },
                                        modifier = Modifier.fillMaxWidth().height(32.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text(scheduleFrequency, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp))
                                    }
                                    DropdownMenu(
                                        expanded = scheduleFrequencyDropdownExpanded,
                                        onDismissRequest = { scheduleFrequencyDropdownExpanded = false }
                                    ) {
                                        val freqOptions = listOf("Tous les jours", "Chaque Vendredi", "Chaque Lundi", "En temps réel")
                                        freqOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt, fontSize = 11.sp) },
                                                onClick = {
                                                    scheduleFrequency = opt
                                                    scheduleFrequencyDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Time
                                OutlinedTextField(
                                    value = scheduleTime,
                                    onValueChange = { scheduleTime = it },
                                    placeholder = { Text("18:00", fontSize = 11.sp) },
                                    modifier = Modifier.weight(0.8f).height(32.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                    singleLine = true
                                )
                            }

                            Button(
                                onClick = {
                                    val dep = selectedDepotForSchedule
                                    if (dep == null) {
                                        Toast.makeText(context, "Veuillez sélectionner un dépôt !", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (schedulePhoneNumber.isBlank()) {
                                        Toast.makeText(context, "Veuillez saisir un numéro !", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val newSchedule = WhatsAppSchedule(
                                        id = UUID.randomUUID().toString(),
                                        depotName = dep.nom,
                                        phoneNumber = schedulePhoneNumber.trim(),
                                        frequency = scheduleFrequency,
                                        time = scheduleTime.trim(),
                                        isActive = true
                                    )
                                    val updated = schedules + newSchedule
                                    saveWhatsAppSchedules(context, updated)
                                    schedules = updated
                                    selectedDepotForSchedule = null
                                    schedulePhoneNumber = ""
                                    Toast.makeText(context, "Rapport programmé !", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StockGreen),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().height(30.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.AlarmAdd, null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Activer la Planification", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Schedules List
                    if (schedules.isNotEmpty()) {
                        Text("Plannings actifs :", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            schedules.forEach { sch ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(0.5.dp, if (sch.isActive) StockGreen.copy(0.4f) else Color.LightGray, RoundedCornerShape(6.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1.2f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (sch.isActive) StockGreen else Color.Gray))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(sch.depotName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text("Vers : ${sch.phoneNumber} | ${sch.frequency} à ${sch.time}", fontSize = 9.sp, color = Color.Gray)
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = { shareDepotStockOnWhatsApp(context, sch.depotName, resultsRealtime, sch.phoneNumber) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.PlayArrow, null, tint = StockBlue, modifier = Modifier.size(14.dp))
                                            }
                                            
                                            Switch(
                                                checked = sch.isActive,
                                                onCheckedChange = { isChecked ->
                                                    val updated = schedules.map {
                                                        if (it.id == sch.id) it.copy(isActive = isChecked) else it
                                                    }
                                                    saveWhatsAppSchedules(context, updated)
                                                    schedules = updated
                                                },
                                                thumbContent = {
                                                    Icon(
                                                        imageVector = if (sch.isActive) Icons.Default.Check else Icons.Default.Close,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(SwitchDefaults.IconSize / 1.6f)
                                                    )
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = StockGreen,
                                                    uncheckedThumbColor = Color.White,
                                                    uncheckedTrackColor = Color.LightGray
                                                ),
                                                modifier = Modifier.scale(0.6f)
                                            )

                                            IconButton(
                                                onClick = {
                                                    val updated = schedules.filter { it.id != sch.id }
                                                    saveWhatsAppSchedules(context, updated)
                                                    schedules = updated
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, null, tint = StockRed, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 6. CRISP SEARCH & ADVANCED FILTER TOGGLE ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filtre textuel (code, article, moule...)", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp), tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StockBlue,
                    unfocusedBorderColor = Color.LightGray.copy(0.7f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            )

            // Dynamic filters count badge
            val activeFiltersCount = listOfNotNull(
                selectedClientFilter,
                selectedDepotFilter,
                if (activeTabMode == 1) selectedFournisseurFilter else null,
                if (activeTabMode == 1) selectedArticleFilter else null,
                if (activeTabMode == 1) selectedMouleFilter else null
            ).size

            Button(
                onClick = { showAdvancedFilters = !showAdvancedFilters },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showAdvancedFilters || activeFiltersCount > 0) StockBlue.copy(0.12f) else Slate100,
                    contentColor = if (showAdvancedFilters || activeFiltersCount > 0) StockBlue else OnSlate100
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(44.dp),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Icon(Icons.Default.FilterList, null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (activeFiltersCount > 0) "Filtres ($activeFiltersCount)" else "Filtres",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- 7. ACTIVE FILTER PILLS ROW (IMPROVES ERP TRANSPARENCY) ---
        val activeFiltersList = remember(selectedClientFilter, selectedDepotFilter, selectedFournisseurFilter, selectedArticleFilter, selectedMouleFilter, activeTabMode) {
            listOfNotNull(
                selectedClientFilter?.let { "Client: ${it.nom}" to { selectedClientFilter = null } },
                selectedDepotFilter?.let { "Dépôt: ${it.nom}" to { selectedDepotFilter = null } },
                if (activeTabMode == 1) selectedFournisseurFilter?.let { "Fournisseur: ${it.nom}" to { selectedFournisseurFilter = null } } else null,
                if (activeTabMode == 1) selectedArticleFilter?.let { "Article: ${it.designation}" to { selectedArticleFilter = null } } else null,
                if (activeTabMode == 1) selectedMouleFilter?.let { "Moule: $it" to { selectedMouleFilter = null } } else null
            )
        }

        if (activeFiltersList.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sélections :", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(activeFiltersList) { (label, onClear) ->
                        Surface(
                            color = Slate100,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { onClear() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = OnSlate100)
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                            }
                        }
                    }
                    item {
                        TextButton(
                            onClick = {
                                selectedClientFilter = null
                                selectedDepotFilter = null
                                selectedFournisseurFilter = null
                                selectedArticleFilter = null
                                selectedMouleFilter = null
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.height(20.dp)
                        ) {
                            Text("Reset", fontSize = 9.sp, color = StockRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- 8. COLLAPSIBLE ADVANCED FILTERS PANEL ---
        AnimatedVisibility(
            visible = showAdvancedFilters,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate50),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, Color.LightGray.copy(0.5f), RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Consulter par critères précis :", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Client Selector
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { clientDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = selectedClientFilter?.nom ?: "Tous les Clients",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp))
                            }
                            DropdownMenu(
                                expanded = clientDropdownExpanded,
                                onDismissRequest = { clientDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tous les Clients", fontSize = 11.sp) },
                                    onClick = {
                                        selectedClientFilter = null
                                        clientDropdownExpanded = false
                                    }
                                )
                                clients.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.nom, fontSize = 11.sp) },
                                        onClick = {
                                            selectedClientFilter = c
                                            clientDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Depot Selector
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { depotDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = selectedDepotFilter?.nom ?: "Tous les Dépôts",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp))
                            }
                            DropdownMenu(
                                expanded = depotDropdownExpanded,
                                onDismissRequest = { depotDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tous les Dépôts", fontSize = 11.sp) },
                                    onClick = {
                                        selectedDepotFilter = null
                                        depotDropdownExpanded = false
                                    }
                                )
                                depots.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d.nom, fontSize = 11.sp) },
                                        onClick = {
                                            selectedDepotFilter = d
                                            depotDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Stock à Date filters
                    if (activeTabMode == 1) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Date
                            Button(
                                onClick = { datePickerDialog.show() },
                                colors = ButtonDefaults.buttonColors(containerColor = StockBlue.copy(0.12f), contentColor = StockBlue),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(formattedSelectedDate, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            // Fournisseur Selector
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { fournisseurDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = selectedFournisseurFilter?.nom ?: "Tous Fournisseurs",
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp))
                                }
                                DropdownMenu(
                                    expanded = fournisseurDropdownExpanded,
                                    onDismissRequest = { fournisseurDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Tous Fournisseurs", fontSize = 11.sp) },
                                        onClick = {
                                            selectedFournisseurFilter = null
                                            fournisseurDropdownExpanded = false
                                        }
                                    )
                                    fournisseurs.forEach { f ->
                                        DropdownMenuItem(
                                            text = { Text(f.nom, fontSize = 11.sp) },
                                            onClick = {
                                                selectedFournisseurFilter = f
                                                fournisseurDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Article
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { articleDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = selectedArticleFilter?.designation ?: "Tous Articles",
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp))
                                }
                                DropdownMenu(
                                    expanded = articleDropdownExpanded,
                                    onDismissRequest = { articleDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Tous Articles", fontSize = 11.sp) },
                                        onClick = {
                                            selectedArticleFilter = null
                                            articleDropdownExpanded = false
                                        }
                                    )
                                    articles.forEach { art ->
                                        DropdownMenuItem(
                                            text = { Text("${art.code} - ${art.designation}", fontSize = 11.sp) },
                                            onClick = {
                                                selectedArticleFilter = art
                                                articleDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Moule
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { mouleDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = selectedMouleFilter ?: "Tous Moules",
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(14.dp))
                                }
                                DropdownMenu(
                                    expanded = mouleDropdownExpanded,
                                    onDismissRequest = { mouleDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Tous Moules", fontSize = 11.sp) },
                                        onClick = {
                                            selectedMouleFilter = null
                                            mouleDropdownExpanded = false
                                        }
                                    )
                                    availableMoules.forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(m, fontSize = 11.sp) },
                                            onClick = {
                                                selectedMouleFilter = m
                                                mouleDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 9. RESULTS LIST (STUNNING LEDGER STYLING) ---
        if (activeTabMode == 0) {
            // STOCK ACTUEL
            if (filteredRealtimeResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inbox, null, modifier = Modifier.size(40.dp), tint = Color.Gray.copy(0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Aucun solde trouvé pour ces critères", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredRealtimeResults) { row ->
                        val hasStock = row.quantiteFinale > 0
                        val statusColor = if (hasStock) StockGreen else Color.Gray

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, Color.LightGray.copy(0.4f), RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                // Dynamic ERP status bar
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(statusColor)
                                )

                                Column(modifier = Modifier.padding(10.dp).weight(1f)) {
                                    // Row 1: Code tag & Designation
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                color = Slate800,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = row.articleCode,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = row.articleName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Slate900,
                                                maxLines = 1
                                            )
                                        }

                                        // Quick indicators for categories
                                        if (row.moule.isNotBlank()) {
                                            Surface(
                                                color = Slate100,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "M: ${row.moule}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = OnSlate100,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Row 2: Client & Depot Details
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Person, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "Client: ${row.clientName}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.Gray,
                                                    maxLines = 1
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.HomeWork, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "Dépôt: ${row.depotName}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate800,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        if (row.qualite.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Star, null, modifier = Modifier.size(11.dp), tint = StockOrange)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = row.qualite,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StockOrange
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Row 3: Professional ERP Ledger values
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Slate100.copy(0.4f))
                                            .border(0.5.dp, Slate100, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ArrowDownward, null, tint = StockBlue, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Recu: ", fontSize = 10.sp, color = Color.Gray)
                                            Text("${row.quantiteEntree}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                        }

                                        Box(modifier = Modifier.width(1.dp).height(10.dp).background(Color.LightGray))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ArrowUpward, null, tint = StockOrange, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Exp: ", fontSize = 10.sp, color = Color.Gray)
                                            Text("${row.quantiteSortie}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                        }

                                        Box(modifier = Modifier.width(1.dp).height(10.dp).background(Color.LightGray))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Inventory2, null, tint = if (hasStock) StockGreen else Color.Gray, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Solde: ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                            Text(
                                                text = "${row.quantiteFinale} ${row.unite}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (hasStock) StockGreen else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // STOCK À DATE
            if (stockADateResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inbox, null, modifier = Modifier.size(40.dp), tint = Color.Gray.copy(0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Aucun mouvement enregistré pour cette période", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(stockADateResults) { row ->
                        val hasStock = row.quantiteStockAtDate > 0
                        val statusColor = if (hasStock) StockGreen else Color.Gray

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, Color.LightGray.copy(0.4f), RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                // Dynamic ERP status bar
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(statusColor)
                                )

                                Column(modifier = Modifier.padding(10.dp).weight(1f)) {
                                    // Row 1: Code tag & Designation
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                color = Slate800,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = row.articleCode,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = row.articleName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Slate900,
                                                maxLines = 1
                                            )
                                        }

                                        if (row.moule.isNotBlank()) {
                                            Surface(
                                                color = Slate100,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "M: ${row.moule}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = OnSlate100,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Row 2: Client, Depot, Supplier Details
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = "Client: ${row.clientName}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color.Gray,
                                                        maxLines = 1
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.HomeWork, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = "Dépôt: ${row.depotName}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Slate800,
                                                        maxLines = 1
                                                    )
                                                }
                                            }

                                            if (row.qualite.isNotBlank()) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Star, null, modifier = Modifier.size(11.dp), tint = StockOrange)
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(
                                                        text = row.qualite,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = StockOrange
                                                    )
                                                }
                                            }
                                        }

                                        if (row.supplierName.isNotBlank() && row.supplierName != "—") {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "Forn: ${row.supplierName}",
                                                    fontSize = 9.sp,
                                                    color = Color.Gray,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Row 3: Professional ERP Ledger values
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Slate100.copy(0.4f))
                                            .border(0.5.dp, Slate100, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ArrowDownward, null, tint = StockBlue, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Recu: ", fontSize = 10.sp, color = Color.Gray)
                                            Text("${row.quantiteEntree}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                        }

                                        Box(modifier = Modifier.width(1.dp).height(10.dp).background(Color.LightGray))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ArrowUpward, null, tint = StockOrange, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Exp: ", fontSize = 10.sp, color = Color.Gray)
                                            Text("${row.quantiteSortie}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                        }

                                        Box(modifier = Modifier.width(1.dp).height(10.dp).background(Color.LightGray))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Inventory2, null, tint = if (hasStock) StockGreen else Color.Gray, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Stock à Date: ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                            Text(
                                                text = "${row.quantiteStockAtDate} ${row.unite}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (hasStock) StockGreen else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // --- 10. SOLID DARK BOTTOM ACCENT LEDGER PANEL ---
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SOLDE CONSOLIDÉ",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StockCyanAccent,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (activeTabMode == 0) "Stock Disponible Général" else "Stock Général au $formattedSelectedDate",
                        fontSize = 10.sp,
                        color = Color.White.copy(0.7f)
                    )
                }
                
                Surface(
                    color = StockGreen.copy(0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${if (activeTabMode == 0) totalStockGlobalRealtime else totalStockADateGlobal} unités",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = StockGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

data class StockFinalViewRow(
    val clientName: String,
    val depotName: String,
    val articleCode: String,
    val articleName: String,
    val moule: String,
    val qualite: String,
    val quantiteEntree: Int,
    val quantiteSortie: Int,
    val quantiteFinale: Int,
    val unite: String
)

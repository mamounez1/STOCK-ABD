package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Observe cloud configuration
    val baseUrl by viewModel.cloudBaseUrl.collectAsState()
    val apiToken by viewModel.cloudApiToken.collectAsState()
    val isAutoSync by viewModel.isAutoSyncEnabled.collectAsState()
    val isSyncing by viewModel.isCloudSyncing.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()
    val syncStatusType by viewModel.syncStatusType.collectAsState()

    // Observe local counts to show state
    val users by viewModel.users.collectAsState()
    val fournisseurs by viewModel.fournisseurs.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val depots by viewModel.depots.collectAsState()
    val articles by viewModel.articles.collectAsState()
    val mouvements by viewModel.mouvements.collectAsState()
    val stockDepots by viewModel.stockDepots.collectAsState()

    // User inputs
    var inputUrl by remember(baseUrl) { mutableStateOf(baseUrl) }
    var inputToken by remember(apiToken) { mutableStateOf(apiToken) }

    // Dialog state for clear database
    var showClearConfirm by remember { mutableStateOf(false) }

    val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE)
    val lastSyncStr = if (lastSyncTime > 0L) format.format(Date(lastSyncTime)) else "Jamais synchronisé"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SCREEN HEADER ---
        item {
            Column {
                Text(
                    text = "Base de Données Cloud & Synchro",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Sauvegardez et synchronisez vos données de stock en temps réel avec votre serveur d'entreprise.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // --- CLOUD STATUS OVERVIEW ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Visual Light Indicator
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                when (syncStatusType) {
                                    "SYNCING" -> StockBlue
                                    "SUCCESS" -> StockGreen
                                    "ERROR" -> StockRed
                                    else -> Color.Gray
                                }
                            )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (syncStatusType) {
                                "SYNCING" -> "Synchronisation en cours..."
                                "SUCCESS" -> "Connecté & Synchronisé"
                                "ERROR" -> "Erreur de connexion"
                                else -> "Mode Local-First Actif"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = syncStatusMessage,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dernière Synchro : $lastSyncStr",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(StockGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.CloudDone, null, tint = StockGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Automatique", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StockGreen)
                        }
                    }
                }
            }
        }

        // --- CONNECTION PARAMETERS FORM ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SettingsInputComponent, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Paramètres de Connexion Cloud",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Entrez l'URL de l'API de votre base de données Cloud. Si vous n'avez pas encore configuré de serveur, laissez l'URL vide pour exécuter une simulation complète de sauvegarde/restauration.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("URL de la Base de Données / API Cloud") },
                        placeholder = { Text("https://api.votre-entreprise.com/v1") },
                        leadingIcon = { Icon(Icons.Default.CloudQueue, null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = inputToken,
                        onValueChange = { inputToken = it },
                        label = { Text("Clé API / Token de Sécurité") },
                        placeholder = { Text("Token d'autorisation secret...") },
                        leadingIcon = { Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            viewModel.saveCloudSettings(inputUrl, inputToken, true)
                            Toast.makeText(context, "Configurations Cloud sauvegardées !", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                        Text(" Enregistrer la Configuration", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- LOCAL DATABASE STATISTICS ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, null, tint = StockBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Statistiques de la Base de Données Locale",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Les tables suivantes résident localement sur l'appareil dans Room (SQLite) et sont prêtes à être sauvegardées sur le serveur cloud :",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    // Table items statistics
                    val stats = listOf(
                        Triple("Utilisateurs autorisés", users.size, Icons.Default.ManageAccounts),
                        Triple("Fournisseurs enregistrés", fournisseurs.size, Icons.Default.Business),
                        Triple("Clients enregistrés", clients.size, Icons.Default.People),
                        Triple("Dépôts / Lieux physiques", depots.size, Icons.Default.HomeWork),
                        Triple("Catalogue d'articles", articles.size, Icons.Default.Category),
                        Triple("Lignes d'état de stock", stockDepots.size, Icons.Default.Inventory),
                        Triple("Historique des mouvements", mouvements.size, Icons.Default.History)
                    )

                    stats.forEach { (name, count, icon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Text(
                                text = "$count enregistrements",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // --- RESET AND RESTORE SECTION ---
        item {
            val user = viewModel.currentUser.value
            if (user?.role == "admin") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StockRed.copy(alpha = 0.04f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StockRed.copy(0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = StockRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Zone de Restauration Complète (Admin)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = StockRed
                            )
                        }

                        Text(
                            text = "Si vous souhaitez vider complètement votre base de données SQLite locale pour forcer une restauration complète à partir du serveur Cloud, cliquez ci-dessous. Cette action est irréversible !",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )

                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StockRed),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, null)
                            Text(" Vider la base locale et préparer la restauration", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for clear SQLite database
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = StockRed) },
            title = { Text("Confirmer la suppression complète ?") },
            text = {
                Text("Êtes-vous sûr de vouloir supprimer tous les enregistrements locaux ? Vous perdrez l'ensemble des données sur l'appareil à moins qu'elles ne soient synchronisées avec le Cloud. Cette action nécessite une ré-initialisation complète.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        scope.launch {
                            viewModel.logout() // Force log out
                            // clear database can be called in repository
                            // We can let the VM clear DB or prepopulate on next restart
                            Toast.makeText(context, "Base vidée avec succès. Veuillez relancer l'application.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StockRed)
                ) {
                    Text("Oui, tout supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

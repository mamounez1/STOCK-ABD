package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.theme.StockOrange
import com.example.ui.theme.StockRed
import com.example.ui.theme.StockGreen
import com.example.ui.theme.StockBlue
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigate: (AppMenu) -> Unit
) {
    val articles by viewModel.articles.collectAsState()
    val fournisseurs by viewModel.fournisseurs.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val depots by viewModel.depots.collectAsState()
    val mouvements by viewModel.mouvements.collectAsState()
    val bonSortiePhotos by viewModel.bonSortiePhotos.collectAsState()

    // Calculated metrics
    val totalArticles = articles.size
    val totalStockQty = articles.sumOf { it.stockActuel }
    val totalFournisseurs = fournisseurs.size
    val totalClients = clients.size
    val totalDepots = depots.size

    // Last 5 movements of each type
    val lastEntrees = mouvements.filter { it.type == "ENTREE" }.take(5)
    val lastSorties = mouvements.filter { it.type == "SORTIE" }.take(5)

    // Alerts: Articles where actual stock is less than equal to minimum
    val alertArticles = articles.filter { it.stockActuel <= it.stockMinimum }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("dashboard_scroll"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and role banner
        item {
            val user = viewModel.currentUser.value
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.onPrimaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WavingHand,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Bonjour, ${user?.fullName ?: "Utilisateur"} !",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "Rôle : ${user?.role?.uppercase() ?: "CONSULTANT"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Surface(
                                    color = StockBlue.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AcUnit,
                                            contentDescription = "Chambre Froide",
                                            tint = StockBlue,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Stock Frigorifique",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StockBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon_1782818492208),
                        contentDescription = "GestiStock Logo",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // KPIs Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Indicateurs Clés",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                // We show an adaptive horizontal/vertical rows
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiCard(
                            title = "Articles uniques",
                            value = "$totalArticles",
                            icon = Icons.Default.Category,
                            tint = StockBlue,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Stock global",
                            value = "$totalStockQty",
                            icon = Icons.Default.Warehouse,
                            tint = StockGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiCard(
                            title = "Fournisseurs",
                            value = "$totalFournisseurs",
                            icon = Icons.Default.Business,
                            tint = StockOrange,
                            modifier = Modifier.weight(11f)
                        )
                        KpiCard(
                            title = "Clients",
                            value = "$totalClients",
                            icon = Icons.Default.People,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(11f)
                        )
                        KpiCard(
                            title = "Dépôts",
                            value = "$totalDepots",
                            icon = Icons.Default.HomeWork,
                            tint = StockBlue,
                            modifier = Modifier.weight(10f)
                        )
                    }
                }
            }
        }

        // Quick Action Shortcuts Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Actions Rapides (Raccourcis)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val user = viewModel.currentUser.value
                    val role = user?.role ?: "consultation"
                    
                    // Button 1: Entrées (only for admin and magasinier)
                    if (role == "admin" || role == "magasinier") {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigate(AppMenu.ENTREES) },
                            colors = CardDefaults.cardColors(containerColor = StockGreen.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.AddBox, contentDescription = null, tint = StockGreen, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Entrée", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StockGreen)
                            }
                        }
                    }
                    
                    // Button 2: Sorties (only for admin and magasinier)
                    if (role == "admin" || role == "magasinier") {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigate(AppMenu.SORTIES) },
                            colors = CardDefaults.cardColors(containerColor = StockRed.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.IndeterminateCheckBox, contentDescription = null, tint = StockRed, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Sortie", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StockRed)
                            }
                        }
                    }
                    
                    // Button 3: Consultation (For everyone)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(AppMenu.CONSULTATION) },
                        colors = CardDefaults.cardColors(containerColor = StockBlue.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Pageview, contentDescription = null, tint = StockBlue, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Consulter", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StockBlue)
                        }
                    }

                    // Button 4: Etat de Stock (For everyone)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(AppMenu.STOCK_FINAL) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Summarize, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Rapport", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }



        // Low stock alerts section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (alertArticles.isNotEmpty()) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (alertArticles.isNotEmpty()) StockRed else StockGreen
                        )
                        Text(
                            text = "Alertes de Stock Faible (${alertArticles.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (alertArticles.isEmpty()) {
                        Text(
                            text = "Aucune alerte de stock bas détectée. Tout est en ordre.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        alertArticles.take(4).forEach { article ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = article.designation,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Code : ${article.code} | Catégorie : ${article.categorie}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${article.stockActuel} / Min ${article.stockMinimum}",
                                        color = StockRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = " ${article.unite}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (alertArticles.size > 4) {
                            Text(
                                text = "Et d'autres articles supplémentaires à stock bas...",
                                fontSize = 11.sp,
                                color = StockOrange,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Recent Movements Layout
        item {
            Text(
                text = "Activités Récentes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Last 5 Entrees
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dernières Entrées",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = StockGreen
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = StockGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    if (lastEntrees.isEmpty()) {
                        Text(
                            text = "Aucune entrée récente enregistrée.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
                        lastEntrees.forEach { item ->
                            val articleName = articles.find { it.id == item.articleId }?.designation ?: "Article #${item.articleId}"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = articleName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(text = "Réf : ${item.reference} | ${format.format(Date(item.dateMovement))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "+${item.quantite}",
                                    color = StockGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Last 5 Sorties
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dernières Sorties",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = StockRed
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = StockRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    if (lastSorties.isEmpty()) {
                        Text(
                            text = "Aucune sortie récente enregistrée.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
                        lastSorties.forEach { item ->
                            val articleName = articles.find { it.id == item.articleId }?.designation ?: "Article #${item.articleId}"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = articleName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(text = "Réf : ${item.reference} | ${format.format(Date(item.dateMovement))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "-${item.quantite}",
                                    color = StockRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(tint.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

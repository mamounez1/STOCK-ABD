package com.example.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Screen navigation tabs enumeration
enum class AppMenu(val label: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Tableau de Bord", Icons.Default.Dashboard, "menu_dash"),
    FOURNISSEURS("Fournisseurs", Icons.Default.Business, "menu_fourn"),
    CLIENTS("Clients", Icons.Default.People, "menu_client"),
    DEPOTS("Dépôts / Lieux", Icons.Default.HomeWork, "menu_depot"),
    ARTICLES("Articles", Icons.Default.Category, "menu_article"),
    ENTREES("Entrées de Stock", Icons.Default.AddBox, "menu_entrees"),
    SORTIES("Sorties de Stock", Icons.Default.IndeterminateCheckBox, "menu_sorties"),
    CONSULTATION("Consultation", Icons.Default.Pageview, "menu_consult"),
    STOCK_FINAL("État de Stock", Icons.Default.Summarize, "menu_final"),
    HISTORIQUE("Historique", Icons.Default.History, "menu_history"),
    UTILISATEURS("Utilisateurs", Icons.Default.ManageAccounts, "menu_users"),
    CLOUD_SYNC("Synchro Cloud", Icons.Default.CloudQueue, "menu_cloud")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Active View state
    var currentTab by remember { mutableStateOf(AppMenu.DASHBOARD) }

    // Print variables
    var showPrintDialog by remember { mutableStateOf(false) }
    var printTitle by remember { mutableStateOf("") }
    var printHeaders by remember { mutableStateOf<List<String>>(emptyList()) }
    var printRows by remember { mutableStateOf<List<List<String>>>(emptyList()) }

    // Drawer state (Mobile hamburgers)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Listen to messages from VM
    LaunchedEffect(Unit) {
        viewModel.statusMessage.collectLatest { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.errorMessage.collectLatest { err ->
            scope.launch { snackbarHostState.showSnackbar("Erreur : $err") }
        }
    }

    // Role-based visible menu filtering
    val visibleMenus = remember(currentUser) {
        val role = currentUser?.role ?: "consultation"
        AppMenu.values().filter { menu ->
            if (menu == AppMenu.FOURNISSEURS) return@filter false
            when (role) {
                "admin" -> true
                "magasinier" -> {
                    menu != AppMenu.CLIENTS &&
                    menu != AppMenu.DEPOTS &&
                    menu != AppMenu.UTILISATEURS
                }
                else -> { // consultation / lecturer sole
                    menu != AppMenu.CLIENTS &&
                    menu != AppMenu.DEPOTS &&
                    menu != AppMenu.ENTREES &&
                    menu != AppMenu.SORTIES &&
                    menu != AppMenu.UTILISATEURS
                }
            }
        }
    }

    // Double check active tab safety when logging in out
    LaunchedEffect(visibleMenus) {
        if (currentTab !in visibleMenus) {
            currentTab = AppMenu.DASHBOARD
        }
    }

    // Handle Authentication view vs Logged-In Scaffold
    if (currentUser == null) {
        AuthScreen(
            viewModel = viewModel,
            onLoginSuccess = {
                currentTab = AppMenu.DASHBOARD
            }
        )
    } else {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth > 750.dp

            if (isWideScreen) {
                // PC / Tablet layout: fixed Sidebar (left) + Screen content (right)
                Row(modifier = Modifier.fillMaxSize()) {
                    // LEFT SIDEBAR
                    Card(
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(0.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        SidebarContent(
                            visibleMenus = visibleMenus,
                            selectedTab = currentTab,
                            onTabSelected = { currentTab = it },
                            currentUser = currentUser!!,
                            onLogout = { viewModel.logout() }
                        )
                    }

                    // RIGHT CONTENT WINDOW
                    Scaffold(
                        modifier = Modifier.weight(1f),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(currentTab.label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("GestiStock • Gestion de Stock & Logistique Frigorifique", fontSize = 11.sp, color = StockBlue, fontWeight = FontWeight.Medium)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                                actions = {
                                    UserBriefBadge(currentUser!!)
                                    IconButton(
                                        onClick = { viewModel.logout() },
                                        modifier = Modifier.testTag("pc_logout_btn")
                                    ) {
                                        Icon(Icons.Default.ExitToApp, "Déconnexion", tint = StockRed)
                                    }
                                }
                            )
                        }
                    ) { padValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padValues)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            ActiveScreenContent(
                                tab = currentTab,
                                viewModel = viewModel,
                                onTriggerPrint = { title, headers, rows ->
                                    printTitle = title
                                    printHeaders = headers
                                    printRows = rows
                                    showPrintDialog = true
                                },
                                onNavigate = { currentTab = it }
                            )
                        }
                    }
                }
            } else {
                // MOBILE / Compact Layout: Highly polished scrollable Bottom Navigation Bar (No Drawer for ultimate user friendliness)
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(currentTab.label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("GestiStock • Logistique Frigorifique", fontSize = 10.sp, color = StockBlue, fontWeight = FontWeight.Medium)
                                }
                            },
                            navigationIcon = {
                                Image(
                                    painter = painterResource(id = R.drawable.img_app_icon_1782818492208),
                                    contentDescription = "GestiStock Logo",
                                    modifier = Modifier
                                        .padding(start = 12.dp, end = 8.dp)
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                            actions = {
                                IconButton(
                                    onClick = { viewModel.logout() },
                                    modifier = Modifier.testTag("mobile_logout_btn")
                                ) {
                                    Icon(Icons.Default.ExitToApp, "Déconnexion", tint = StockRed)
                                }
                            }
                        )
                    },
                    bottomBar = {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            tonalElevation = 8.dp,
                            shadowElevation = 16.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    .also { Log.d("MainAppContent", "Rendering bottom navigation bar with icons only") }
                            ) {
                                visibleMenus.forEach { menu ->
                                    val isSelected = menu == currentTab
                                    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(bgColor)
                                            .clickable { currentTab = menu }
                                            .padding(12.dp)
                                            .testTag("bottom_nav_${menu.tag}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = menu.icon,
                                            contentDescription = menu.label,
                                            tint = contentColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { padValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padValues)
                    ) {
                        ActiveScreenContent(
                            tab = currentTab,
                            viewModel = viewModel,
                            onTriggerPrint = { title, headers, rows ->
                                printTitle = title
                                printHeaders = headers
                                printRows = rows
                                showPrintDialog = true
                            },
                            onNavigate = { currentTab = it }
                        )
                    }
                }
            }
        }
    }

    // Shared visual print output dialogue
    if (showPrintDialog) {
        PrintDialog(
            title = printTitle,
            headers = printHeaders,
            rows = printRows,
            onDismiss = { showPrintDialog = false }
        )
    }
}

@Composable
fun SidebarContent(
    visibleMenus: List<AppMenu>,
    selectedTab: AppMenu,
    onTabSelected: (AppMenu) -> Unit,
    currentUser: User,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // App header / Logo
        Row(
            modifier = Modifier.padding(bottom = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_app_icon_1782818492208),
                contentDescription = "GestiStock Logo",
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "GestiStock",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Contrôle & Logistique",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }

        Divider(modifier = Modifier.padding(bottom = 12.dp))

        // Screen selection lists scroll. Safe spacing layout
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            visibleMenus.forEach { menu ->
                val isSelected = menu == selectedTab
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .clickable { onTabSelected(menu) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag(menu.tag),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = menu.icon,
                        contentDescription = menu.label,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = menu.label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        // Active context profile brief card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = currentUser.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Profil : ${currentUser.role.uppercase()}",
                    fontSize = 11.sp,
                    color = when (currentUser.role) {
                        "admin" -> StockRed
                        "magasinier" -> StockBlue
                        else -> Color.Gray
                    },
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = StockRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Logout, "Disconnect", modifier = Modifier.size(16.dp))
                    Text(" Déconnexion", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun UserBriefBadge(user: User) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        when (user.role) {
                            "admin" -> StockRed
                            "magasinier" -> StockBlue
                            else -> StockGreen
                        },
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${user.fullName} (${user.role.uppercase()})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Router switcher rendering active selected layout screen
@Composable
fun ActiveScreenContent(
    tab: AppMenu,
    viewModel: MainViewModel,
    onTriggerPrint: (title: String, headers: List<String>, rows: List<List<String>>) -> Unit,
    onNavigate: (AppMenu) -> Unit
) {
    Crossfade(targetState = tab, label = "Screen Transition") { activeTab ->
        when (activeTab) {
            AppMenu.DASHBOARD -> DashboardScreen(viewModel = viewModel, onNavigate = onNavigate)
            AppMenu.FOURNISSEURS -> FournisseursScreen(viewModel = viewModel)
            AppMenu.CLIENTS -> ClientsScreen(viewModel = viewModel)
            AppMenu.DEPOTS -> DepotsScreen(viewModel = viewModel)
            AppMenu.ARTICLES -> ArticlesScreen(viewModel = viewModel)
            AppMenu.ENTREES -> EntreesStockScreen(viewModel = viewModel, onTriggerPrint = onTriggerPrint)
            AppMenu.SORTIES -> SortiesStockScreen(viewModel = viewModel, onTriggerPrint = onTriggerPrint)
            AppMenu.CONSULTATION -> ConsultationStockScreen(viewModel = viewModel, onTriggerPrint = onTriggerPrint)
            AppMenu.STOCK_FINAL -> StockFinalScreen(viewModel = viewModel, onTriggerPrint = onTriggerPrint)
            AppMenu.HISTORIQUE -> HistoriqueMouvementsScreen(viewModel = viewModel, onTriggerPrint = onTriggerPrint)
            AppMenu.UTILISATEURS -> UsersManagementScreen(viewModel = viewModel)
            AppMenu.CLOUD_SYNC -> CloudSyncScreen(viewModel = viewModel)
        }
    }
}

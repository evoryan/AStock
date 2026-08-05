package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Product
import com.example.data.SalesTransaction
import com.example.data.TenantConfig
import com.example.ui.components.AppBottomNavbar
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PembukuanScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SetupGuideScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    Splash, Login, Dashboard, SetupGuide, Settings, Pembukuan, History
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.Splash) }
                var previousScreen by remember { mutableStateOf(AppScreen.Login) }
                
                // Active Tenant session configuration
                var loggedInTenant by remember { mutableStateOf<TenantConfig?>(null) }
                var isRealVps by remember { mutableStateOf(false) }
                var vpsUrl by remember { mutableStateOf("") }

                // Room Database for local persistence and tenant data isolation
                val appDb = remember { com.example.data.AppRoomDatabase.getDatabase(this@MainActivity) }

                // Lifted Settings States for synchronization
                var storeNameState by remember { mutableStateOf("") }
                var storeAddressState by remember { mutableStateOf("Jl. Telekomunikasi No. 1, Bandung") }
                var adminNameState by remember { mutableStateOf("") }
                var emailState by remember { mutableStateOf("satriaevo77@gmail.com") }
                var passwordState by remember { mutableStateOf("adminpassword123") }
                var globalMinStockAlert by remember { mutableStateOf(5) }
                var showCurrencySymbol by remember { mutableStateOf(true) }
                var invoiceHeaderState by remember { mutableStateOf("TERIMA KASIH TELAH BERBELANJA") }
                var invoiceFooterState by remember { mutableStateOf("Barang yang sudah dibeli tidak dapat ditukar/dikembalikan.") }
                var isSuperadminMode by remember { mutableStateOf(false) }
                var superadminInfo by remember { mutableStateOf("Pengumuman Penting: Seluruh tenant harap melakukan update stok secara berkala dan mencatat transaksi penjualan secara real-time ke VPS.") }

                var branchesList by remember {
                    mutableStateOf(emptyList<String>())
                }

                var categoriesList by remember {
                    mutableStateOf(listOf("HP", "Aksesoris"))
                }
                var areasListRaw by remember { mutableStateOf<List<com.example.data.AreaResponse>>(emptyList()) }

                var adminUsers by remember {
                    mutableStateOf(emptyList<Map<String, String>>())
                }

                var selectedDashboardTab by remember { mutableStateOf(1) }

                // Global state for Products & Transactions
                var globalProductsList by remember { mutableStateOf<List<Product>>(emptyList()) }
                var globalTransactionsList by remember { mutableStateOf<List<SalesTransaction>>(emptyList()) }

                // Global Barcode Scanner state
                var showGlobalScannerDialog by remember { mutableStateOf(false) }
                var scannedBarcodeResult by remember { mutableStateOf<String?>(null) }

                val coroutineScope = rememberCoroutineScope()

                // Complete Logout & Clean-Up Function to Isolate Tenant Data
                fun performLogout() {
                    val sharedPrefs = this@MainActivity.getSharedPreferences("app_session", android.content.Context.MODE_PRIVATE)
                    sharedPrefs.edit().clear().apply()

                    // Reset all tenant memory state variables to avoid data leaking between sessions
                    loggedInTenant = null
                    globalProductsList = emptyList()
                    globalTransactionsList = emptyList()
                    storeNameState = ""
                    adminNameState = ""
                    branchesList = emptyList()
                    areasListRaw = emptyList()
                    adminUsers = emptyList()
                    categoriesList = listOf("HP", "Aksesoris")
                    selectedDashboardTab = 1
                    currentScreen = AppScreen.Login
                }

                fun fetchGlobalData() {
                    loggedInTenant?.let { tenant ->
                        coroutineScope.launch {
                            // 1. Load instantly from local Room DB (Tenant Isolated)
                            try {
                                val localProducts = appDb.tenantDataDao().getProductsListByTenant(tenant.dbName)
                                if (localProducts.isNotEmpty()) {
                                    globalProductsList = localProducts.map { p ->
                                        Product(
                                            id = p.id,
                                            name = p.name,
                                            sku = p.sku,
                                            category = p.category,
                                            price = p.price,
                                            stock = p.stock,
                                            minStockAlert = p.minStockAlert
                                        )
                                    }
                                }
                                val localTx = appDb.tenantDataDao().getTransactionsListByTenant(tenant.dbName)
                                if (localTx.isNotEmpty()) {
                                    globalTransactionsList = localTx.map { t ->
                                        SalesTransaction(
                                            id = t.id,
                                            productName = t.productName,
                                            sku = t.sku,
                                            quantity = t.quantity,
                                            totalPrice = t.totalPrice,
                                            timestamp = t.timestamp,
                                            operator = t.operator
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            // 2. Fetch remote VPS API & update Room DB Cache
                            try {
                                val pList = com.example.data.ApiClient.getService().getProducts(tenant.dbName)
                                globalProductsList = pList.map { p ->
                                    Product(
                                        id = p.id,
                                        name = p.name,
                                        sku = p.sku,
                                        category = p.category,
                                        price = p.price,
                                        stock = p.stock,
                                        minStockAlert = p.minStockAlert
                                    )
                                }
                                val productEntities = pList.map { p ->
                                    com.example.data.LocalProductEntity(
                                        tenantDbName = tenant.dbName,
                                        id = p.id,
                                        name = p.name,
                                        sku = p.sku,
                                        category = p.category,
                                        price = p.price,
                                        stock = p.stock,
                                        minStockAlert = p.minStockAlert
                                    )
                                }
                                appDb.tenantDataDao().clearProductsByTenant(tenant.dbName)
                                appDb.tenantDataDao().insertProducts(productEntities)
                            } catch (e: Exception) {
                                // keep Room DB local data
                            }

                            try {
                                val txList = com.example.data.ApiClient.getService().getTransactions(tenant.dbName)
                                globalTransactionsList = txList.map { t ->
                                    val formattedTime = try {
                                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(t.timestamp))
                                    } catch (e: Exception) {
                                        t.timestamp.toString()
                                    }
                                    SalesTransaction(
                                        id = t.id,
                                        productName = t.productName,
                                        sku = t.sku,
                                        quantity = t.quantity,
                                        totalPrice = t.totalPrice,
                                        timestamp = formattedTime,
                                        operator = t.operator
                                    )
                                }
                                val txEntities = globalTransactionsList.map { t ->
                                    com.example.data.LocalTransactionEntity(
                                        tenantDbName = tenant.dbName,
                                        id = t.id,
                                        productName = t.productName,
                                        sku = t.sku,
                                        quantity = t.quantity,
                                        totalPrice = t.totalPrice,
                                        timestamp = t.timestamp,
                                        operator = t.operator
                                    )
                                }
                                appDb.tenantDataDao().clearTransactionsByTenant(tenant.dbName)
                                appDb.tenantDataDao().insertTransactions(txEntities)
                            } catch (e: Exception) {
                                // keep Room DB local data
                            }
                        }
                    }
                }

                // Whenever active tenant changes, reset and re-initialize tenant state
                LaunchedEffect(loggedInTenant) {
                    if (loggedInTenant == null) {
                        storeNameState = ""
                        adminNameState = ""
                        globalProductsList = emptyList()
                        globalTransactionsList = emptyList()
                        branchesList = emptyList()
                        areasListRaw = emptyList()
                        adminUsers = emptyList()
                    } else {
                        val tenant = loggedInTenant!!
                        storeNameState = tenant.name
                        adminNameState = tenant.ownerName
                        
                        // Clear old memory lists to ensure no previous tenant data leaks
                        globalProductsList = emptyList()
                        globalTransactionsList = emptyList()
                        branchesList = emptyList()
                        areasListRaw = emptyList()
                        adminUsers = emptyList()

                        // Load tenant-isolated categories from SharedPreferences / Room DB
                        val sharedPrefs = this@MainActivity.getSharedPreferences("app_session", android.content.Context.MODE_PRIVATE)
                        val savedCategories = sharedPrefs.getStringSet("categories_list_${tenant.dbName}", null)?.toList()
                        categoriesList = savedCategories ?: listOf("HP", "Aksesoris")

                        fetchGlobalData()
                    }
                }

                val showNavbar = loggedInTenant != null && currentScreen != AppScreen.Splash && currentScreen != AppScreen.Login
                val activeTenantAccent = Color(android.graphics.Color.parseColor(loggedInTenant?.accentColor ?: "#F59E0B"))

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showNavbar) {
                            AppBottomNavbar(
                                currentScreen = currentScreen,
                                dashboardTab = selectedDashboardTab,
                                tenantAccent = activeTenantAccent,
                                onNavigateTab = { targetScreen, targetTab ->
                                    if (targetScreen == AppScreen.Dashboard) {
                                        selectedDashboardTab = targetTab
                                    }
                                    currentScreen = targetScreen
                                },
                                onScannerClick = {
                                    showGlobalScannerDialog = true
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Main Page Content area
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                },
                                label = "ScreenTransition"
                            ) { screen ->
                                when (screen) {
                                    AppScreen.Splash -> {
                                        SplashScreen(
                                            onSplashFinished = {
                                                val sharedPrefs = this@MainActivity.getSharedPreferences("app_session", android.content.Context.MODE_PRIVATE)
                                                val savedId = sharedPrefs.getString("tenant_id", null)
                                                if (savedId != null) {
                                                    val savedName = sharedPrefs.getString("tenant_name", "") ?: ""
                                                    val savedOwner = sharedPrefs.getString("tenant_owner", "") ?: ""
                                                    val savedDbName = sharedPrefs.getString("tenant_dbname", "") ?: ""
                                                    val savedAccent = sharedPrefs.getString("tenant_accent", "#F59E0B") ?: "#F59E0B"
                                                    val savedBusinessType = sharedPrefs.getString("tenant_businesstype", "") ?: ""
                                                    val savedVpsUrl = sharedPrefs.getString("vps_url", "") ?: ""
                                                    val savedIsRealVps = sharedPrefs.getBoolean("is_real_vps", false)
                                                    
                                                    if (savedVpsUrl.isNotEmpty()) {
                                                        com.example.data.ApiClient.baseUrl = savedVpsUrl
                                                    }

                                                    loggedInTenant = TenantConfig(
                                                        id = savedId,
                                                        name = savedName,
                                                        ownerName = savedOwner,
                                                        dbName = savedDbName,
                                                        accentColor = savedAccent,
                                                        businessType = savedBusinessType,
                                                        initialProducts = emptyList()
                                                    )
                                                    isRealVps = savedIsRealVps
                                                    vpsUrl = savedVpsUrl
                                                    currentScreen = AppScreen.Dashboard
                                                } else {
                                                    currentScreen = AppScreen.Login
                                                }
                                            }
                                        )
                                    }
                                    AppScreen.Login -> {
                                        LoginScreen(
                                            onLoginSuccess = { tenant, realVps, url ->
                                                loggedInTenant = tenant
                                                isRealVps = realVps
                                                vpsUrl = url
                                                
                                                val sharedPrefs = this@MainActivity.getSharedPreferences("app_session", android.content.Context.MODE_PRIVATE)
                                                sharedPrefs.edit().apply {
                                                    putString("tenant_id", tenant.id)
                                                    putString("tenant_name", tenant.name)
                                                    putString("tenant_owner", tenant.ownerName)
                                                    putString("tenant_dbname", tenant.dbName)
                                                    putString("tenant_accent", tenant.accentColor)
                                                    putString("tenant_businesstype", tenant.businessType)
                                                    putString("vps_url", url)
                                                    putBoolean("is_real_vps", realVps)
                                                    apply()
                                                }
                                                
                                                currentScreen = AppScreen.Dashboard
                                            }
                                        )
                                    }
                                    AppScreen.Dashboard -> {
                                        loggedInTenant?.let { tenant ->
                                            DashboardScreen(
                                                tenant = tenant,
                                                isRealVps = isRealVps,
                                                vpsUrl = vpsUrl,
                                                initialTab = selectedDashboardTab,
                                                onTabChange = { selectedDashboardTab = it },
                                                onNavigateToPembukuan = { currentScreen = AppScreen.Pembukuan },
                                                onNavigateToHistory = { currentScreen = AppScreen.History },
                                                
                                                storeNameState = storeNameState,
                                                onStoreNameChange = { storeNameState = it },
                                                storeAddressState = storeAddressState,
                                                onStoreAddressChange = { storeAddressState = it },
                                                adminNameState = adminNameState,
                                                onAdminNameChange = { adminNameState = it },
                                                emailState = emailState,
                                                onEmailChange = { emailState = it },
                                                passwordState = passwordState,
                                                onPasswordChange = { passwordState = it },
                                                globalMinStockAlert = globalMinStockAlert,
                                                onGlobalMinStockAlertChange = { globalMinStockAlert = it },
                                                showCurrencySymbol = showCurrencySymbol,
                                                onShowCurrencySymbolChange = { showCurrencySymbol = it },
                                                invoiceHeaderState = invoiceHeaderState,
                                                onInvoiceHeaderChange = { invoiceHeaderState = it },
                                                invoiceFooterState = invoiceFooterState,
                                                onInvoiceFooterChange = { invoiceFooterState = it },
                                                isSuperadminMode = isSuperadminMode,
                                                onIsSuperadminModeChange = { isSuperadminMode = it },
                                                superadminInfo = superadminInfo,
                                                onSuperadminInfoChange = { superadminInfo = it },
                                                
                                                branchesList = branchesList,
                                                onBranchesListChange = { branchesList = it },
                                                areasListRaw = areasListRaw,
                                                onAreasListRawChange = { areasListRaw = it },
                                                adminUsers = adminUsers,
                                                onAdminUsersChange = { adminUsers = it },
                                                
                                                categoriesList = categoriesList,
                                                onCategoriesListChange = { newCategories ->
                                                    categoriesList = newCategories
                                                    val sharedPrefs = this@MainActivity.getSharedPreferences("app_session", android.content.Context.MODE_PRIVATE)
                                                    sharedPrefs.edit().putStringSet("categories_list_${tenant.dbName}", newCategories.toSet()).apply()
                                                },

                                                onLogoutClick = {
                                                    performLogout()
                                                },
                                                onShowGuideClick = {
                                                    previousScreen = AppScreen.Dashboard
                                                    currentScreen = AppScreen.SetupGuide
                                                },
                                                onSettingsClick = {
                                                    previousScreen = AppScreen.Dashboard
                                                    currentScreen = AppScreen.Settings
                                                }
                                            )
                                        }
                                    }
                                    AppScreen.Pembukuan -> {
                                        loggedInTenant?.let { tenant ->
                                            PembukuanScreen(
                                                tenant = tenant,
                                                transactions = globalTransactionsList,
                                                products = globalProductsList,
                                                onBackClick = {
                                                    currentScreen = AppScreen.Dashboard
                                                },
                                                refreshData = {
                                                    fetchGlobalData()
                                                }
                                            )
                                        }
                                    }
                                    AppScreen.History -> {
                                        loggedInTenant?.let { tenant ->
                                            HistoryScreen(
                                                tenant = tenant,
                                                transactions = globalTransactionsList,
                                                onBackClick = {
                                                    currentScreen = AppScreen.Dashboard
                                                },
                                                refreshData = {
                                                    fetchGlobalData()
                                                }
                                            )
                                        }
                                    }
                                    AppScreen.Settings -> {
                                        loggedInTenant?.let { tenant ->
                                            SettingsScreen(
                                                tenant = tenant,
                                                isRealVps = isRealVps,
                                                vpsUrl = vpsUrl,
                                                onNavigateTab = { tab ->
                                                    selectedDashboardTab = tab
                                                    currentScreen = AppScreen.Dashboard
                                                },
                                                
                                                storeNameState = storeNameState,
                                                onStoreNameChange = { storeNameState = it },
                                                storeAddressState = storeAddressState,
                                                onStoreAddressChange = { storeAddressState = it },
                                                adminNameState = adminNameState,
                                                onAdminNameChange = { adminNameState = it },
                                                emailState = emailState,
                                                onEmailChange = { emailState = it },
                                                passwordState = passwordState,
                                                onPasswordChange = { passwordState = it },
                                                globalMinStockAlert = globalMinStockAlert,
                                                onGlobalMinStockAlertChange = { globalMinStockAlert = it },
                                                showCurrencySymbol = showCurrencySymbol,
                                                onShowCurrencySymbolChange = { showCurrencySymbol = it },
                                                invoiceHeaderState = invoiceHeaderState,
                                                onInvoiceHeaderChange = { invoiceHeaderState = it },
                                                invoiceFooterState = invoiceFooterState,
                                                onInvoiceFooterChange = { invoiceFooterState = it },
                                                isSuperadminMode = isSuperadminMode,
                                                onIsSuperadminModeChange = { isSuperadminMode = it },
                                                superadminInfo = superadminInfo,
                                                onSuperadminInfoChange = { superadminInfo = it },
                                                
                                                branchesList = branchesList,
                                                onBranchesListChange = { branchesList = it },
                                                areasListRaw = areasListRaw,
                                                onAreasListRawChange = { areasListRaw = it },
                                                adminUsers = adminUsers,
                                                onAdminUsersChange = { adminUsers = it },
                                                
                                                categoriesList = categoriesList,
                                                onCategoriesListChange = { newCategories ->
                                                    categoriesList = newCategories
                                                    val sharedPrefs = this@MainActivity.getSharedPreferences("app_session", android.content.Context.MODE_PRIVATE)
                                                    sharedPrefs.edit().putStringSet("categories_list_${tenant.dbName}", newCategories.toSet()).apply()
                                                },
                                                
                                                onBackClick = {
                                                    currentScreen = previousScreen
                                                },
                                                onLogoutClick = {
                                                    performLogout()
                                                },
                                                refreshData = {
                                                    fetchGlobalData()
                                                }
                                            )
                                        }
                                    }
                                    AppScreen.SetupGuide -> {
                                        SetupGuideScreen(
                                            onBackClick = {
                                                currentScreen = previousScreen
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Global Barcode Scanner Dialog
                    if (showGlobalScannerDialog) {
                        BarcodeScannerDialog(
                            onDismissRequest = { showGlobalScannerDialog = false },
                            onBarcodeScanned = { barcode ->
                                showGlobalScannerDialog = false
                                scannedBarcodeResult = barcode
                            }
                        )
                    }

                    // Scanned Barcode Result Alert
                    if (scannedBarcodeResult != null) {
                        AlertDialog(
                            onDismissRequest = { scannedBarcodeResult = null },
                            title = {
                                Text(
                                    text = "Barcode Terdeteksi",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Text(
                                    text = "Hasil Scan Barcode: $scannedBarcodeResult",
                                    color = Color.LightGray
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = { scannedBarcodeResult = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = activeTenantAccent)
                                ) {
                                    Text("OK")
                                }
                            },
                            containerColor = Color(0xFF1E293B)
                        )
                    }
                }
            }
        }
    }
}

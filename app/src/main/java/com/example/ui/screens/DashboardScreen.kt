package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.SalesTransaction
import com.example.data.TenantConfig
import com.example.data.ApiClient
import com.example.data.StockUpdateRequest
import com.example.data.ProductAddRequest
import com.example.data.TransactionAddRequest
import com.example.ui.components.FlowDiagram
import com.example.ui.components.BarcodeScannerDialog
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    tenant: TenantConfig,
    isRealVps: Boolean,
    vpsUrl: String,
    
    // Lifted settings states passed from MainActivity
    storeNameState: String,
    onStoreNameChange: (String) -> Unit,
    storeAddressState: String,
    onStoreAddressChange: (String) -> Unit,
    adminNameState: String,
    onAdminNameChange: (String) -> Unit,
    emailState: String,
    onEmailChange: (String) -> Unit,
    passwordState: String,
    onPasswordChange: (String) -> Unit,
    globalMinStockAlert: Int,
    onGlobalMinStockAlertChange: (Int) -> Unit,
    showCurrencySymbol: Boolean,
    onShowCurrencySymbolChange: (Boolean) -> Unit,
    invoiceHeaderState: String,
    onInvoiceHeaderChange: (String) -> Unit,
    invoiceFooterState: String,
    onInvoiceFooterChange: (String) -> Unit,
    isSuperadminMode: Boolean,
    onIsSuperadminModeChange: (Boolean) -> Unit,
    superadminInfo: String,
    onSuperadminInfoChange: (String) -> Unit,
    
    categoriesList: List<String>,
    onCategoriesListChange: (List<String>) -> Unit,
    
    branchesList: List<String>,
    onBranchesListChange: (List<String>) -> Unit,
    areasListRaw: List<com.example.data.AreaResponse>,
    onAreasListRawChange: (List<com.example.data.AreaResponse>) -> Unit,
    adminUsers: List<Map<String, String>>,
    onAdminUsersChange: (List<Map<String, String>>) -> Unit,

    onLogoutClick: () -> Unit,
    onShowGuideClick: () -> Unit,
    onSettingsClick: () -> Unit,
    initialTab: Int = 1,
    onTabChange: ((Int) -> Unit)? = null,
    onNavigateToPembukuan: (() -> Unit)? = null,
    onNavigateToHistory: (() -> Unit)? = null
) {
    // Current Active Tenant color token from config
    val tenantAccent = Color(android.graphics.Color.parseColor(tenant.accentColor))

    // State for Products and Transactions loaded from Simulated database (with real dynamic operations)
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var transactions by remember { mutableStateOf<List<SalesTransaction>>(emptyList()) }
    var isLoadingData by remember { mutableStateOf(false) }
    var dashboardError by remember { mutableStateOf<String?>(null) }

    // Dashboard Sub-navigation Tabs: 1: Beranda, 2: Stok, 3: Kasir/POS, 4: Pengaturan
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }

    // New Dropdown Filters for Beranda (date, month, year) in 1 row
    // Default to current local time (2026-08-03)
    var filterDay by remember { mutableStateOf("03") } // "Semua", "01", "02", ..., "31"
    var filterMonthName by remember { mutableStateOf("Agustus") } // "Semua", "Januari", ..., "Desember"
    var filterYear by remember { mutableStateOf("2026") } // "Semua", "2026", "2025", ...
    
    // Category filter for Stock tab
    var selectedCategoryFilter by remember { mutableStateOf("Semua") } // "Semua", "HP", "Aksesoris"

    val dayOptions = remember { listOf("Semua") + (1..31).map { String.format("%02d", it) } }
    val monthOptions = remember {
        listOf(
            "Semua", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
    }
    val yearOptions = remember { listOf("Semua", "2026", "2025", "2024", "2023") }

    val monthMap = remember {
        mapOf(
            "Januari" to "01", "Februari" to "02", "Maret" to "03", "April" to "04",
            "Mei" to "05", "Juni" to "06", "Juli" to "07", "Agustus" to "08",
            "September" to "09", "Oktober" to "10", "November" to "11", "Desember" to "12"
        )
    }

    var showHistoryDialog by remember { mutableStateOf(false) }
    var showAccountingDialog by remember { mutableStateOf(false) }

    // Printer States
    var isPrinterConnected by remember { mutableStateOf(false) }
    var selectedPrinter by remember { mutableStateOf("MP-58 (Bluetooth Thermal)") }

    // Backup & Restore States
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    // Update States
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateCheckedMessage by remember { mutableStateOf<String?>(null) }

    val currentDateTimeString = remember {
        try {
            SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
        } catch (e: Exception) {
            SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
        }
    }

    val filteredTransactions = remember(transactions, filterDay, filterMonthName, filterYear) {
        transactions.filter { tx ->
            // tx.timestamp is "yyyy-MM-dd HH:mm"
            val parts = tx.timestamp.split(" ")
            if (parts.isEmpty()) return@filter false
            val datePart = parts[0] // "yyyy-MM-dd"
            val dateSplit = datePart.split("-")
            if (dateSplit.size < 3) return@filter false
            
            val txYear = dateSplit[0] // "yyyy"
            val txMonth = dateSplit[1] // "MM"
            val txDay = dateSplit[2] // "dd"

            val dayMatch = if (filterDay == "Semua") true else txDay == filterDay
            val monthCode = monthMap[filterMonthName]
            val monthMatch = if (filterMonthName == "Semua" || monthCode == null) true else txMonth == monthCode
            val yearMatch = if (filterYear == "Semua") true else txYear == filterYear

            dayMatch && monthMatch && yearMatch
        }
    }

    // Search query for stocks
    var searchQuery by remember { mutableStateOf("") }

    // Add Product Modal State
    var showAddProductModal by remember { mutableStateOf(false) }
    var newProdName by remember { mutableStateOf("") }
    var newProdSku by remember { mutableStateOf("") }
    var newProdCategory by remember { mutableStateOf("") }
    var newProdModal by remember { mutableStateOf("") }
    var newProdPrice by remember { mutableStateOf("") }
    var newProdStock by remember { mutableStateOf("") }
    var addProductError by remember { mutableStateOf<String?>(null) }

    // POS Checkout State
    var selectedProductForCart by remember { mutableStateOf<Product?>(null) }
    var cartQuantity by remember { mutableStateOf(1) }
    var cartDiscount by remember { mutableStateOf("") }
    var checkoutSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Barcode Scanner States
    var showStockSearchScanDialog by remember { mutableStateOf(false) }
    var showAddProductScanDialog by remember { mutableStateOf(false) }
    var showCashierScanDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Refresh products and transactions
    fun refreshData() {
        coroutineScope.launch {
            isLoadingData = true
            dashboardError = null
            try {
                // Fetch products from production VPS
                val productsRes = ApiClient.getService().getProducts(tenant.dbName)
                products = productsRes.map { p ->
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

                // Fetch transactions from production VPS
                val txRes = ApiClient.getService().getTransactions(tenant.dbName)
                transactions = txRes.map { t ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val dateStr = sdf.format(Date(t.timestamp))
                    SalesTransaction(
                        id = t.id,
                        productName = t.productName,
                        sku = t.sku,
                        quantity = t.quantity,
                        totalPrice = t.totalPrice,
                        timestamp = dateStr,
                        operator = t.operator
                    )
                }

                // Fetch areas from VPS
                try {
                    val areasRes = ApiClient.getService().getAreas(tenant.dbName)
                    if (areasRes.isNotEmpty()) {
                        onAreasListRawChange(areasRes)
                        onBranchesListChange(areasRes.map { it.name })
                    }
                } catch (e: Exception) {
                    // Silently ignore or fall back to local defaults
                }

                // Fetch admins from VPS
                try {
                    val adminsRes = ApiClient.getService().getAdmins(tenant.dbName)
                    if (adminsRes.isNotEmpty()) {
                        onAdminUsersChange(adminsRes.map {
                            mapOf(
                                "id" to it.id,
                                "name" to it.name,
                                "username" to it.username,
                                "password" to it.password,
                                "role" to it.role,
                                "area" to it.area
                            )
                        })
                    }
                } catch (e: Exception) {
                    // Silently ignore or fall back to local defaults
                }
            } catch (e: Exception) {
                dashboardError = "Koneksi VPS gagal: ${e.localizedMessage ?: "Network Error"}"
            } finally {
                isLoadingData = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    // Helper: Format Rupiah Currency
    fun formatRupiah(amount: Double): String {
        val prefix = if (showCurrencySymbol) "Rp " else ""
        return prefix + String.format("%,.0f", amount).replace(',', '.')
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = storeNameState,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(tenantAccent, RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Admin: ${tenant.ownerName} | $currentDateTimeString",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    // Node.JS VPS connection mode indicator dot
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(10.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (isRealVps) Color(0xFF10B981) else Color(0xFFF59E0B))
                    )

                    // Logout Button
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Keluar",
                            tint = Color.LightGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF0F172A),
        floatingActionButton = {
            if (activeTab == 2) {
                FloatingActionButton(
                    onClick = {
                        addProductError = null
                        newProdName = ""
                        newProdSku = "SKU-${(100..999).random()}"
                        newProdCategory = ""
                        newProdModal = ""
                        newProdPrice = ""
                        newProdStock = ""
                        showAddProductModal = true
                    },
                    containerColor = tenantAccent,
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Produk")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // TAB 1: BERANDA SCREEN
            if (activeTab == 1) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. INFORMASI DARI SUPERADMIN
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, tenantAccent.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = "Info",
                                    tint = tenantAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Informasi Superadmin",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = superadminInfo,
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // 2. FILTER TANGGAL, BULAN, DAN TAHUN (1 ROW DROPDOWNS)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FilterAlt,
                                        contentDescription = "Filter",
                                        tint = tenantAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Filter Periode Laporan",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Text(
                                    text = "Hari Ini: 2026-08-03",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Day Dropdown
                                Box(modifier = Modifier.weight(1f)) {
                                    var dayExpanded by remember { mutableStateOf(false) }
                                    Column {
                                        Text("Tanggal", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF334155))
                                                .clickable { dayExpanded = true }
                                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(filterDay, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = dayExpanded,
                                            onDismissRequest = { dayExpanded = false },
                                            modifier = Modifier.background(Color(0xFF1E293B)).heightIn(max = 240.dp)
                                        ) {
                                            dayOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option, color = Color.White, fontSize = 13.sp) },
                                                    onClick = {
                                                        filterDay = option
                                                        dayExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Month Dropdown
                                Box(modifier = Modifier.weight(1.2f)) {
                                    var monthExpanded by remember { mutableStateOf(false) }
                                    Column {
                                        Text("Bulan", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF334155))
                                                .clickable { monthExpanded = true }
                                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(filterMonthName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = monthExpanded,
                                            onDismissRequest = { monthExpanded = false },
                                            modifier = Modifier.background(Color(0xFF1E293B)).heightIn(max = 240.dp)
                                        ) {
                                            monthOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option, color = Color.White, fontSize = 13.sp) },
                                                    onClick = {
                                                        filterMonthName = option
                                                        monthExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Year Dropdown
                                Box(modifier = Modifier.weight(1f)) {
                                    var yearExpanded by remember { mutableStateOf(false) }
                                    Column {
                                        Text("Tahun", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF334155))
                                                .clickable { yearExpanded = true }
                                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(filterYear, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = yearExpanded,
                                            onDismissRequest = { yearExpanded = false },
                                            modifier = Modifier.background(Color(0xFF1E293B)).heightIn(max = 240.dp)
                                        ) {
                                            yearOptions.forEach { option ->
                                                DropdownMenuItem(
                                                    text = { Text(option, color = Color.White, fontSize = 13.sp) },
                                                    onClick = {
                                                        filterYear = option
                                                        yearExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. STATS ROW (1 Row 2 Cards): JUMLAH STOK HP & JUMLAH PEMASUKAN HARI INI
                    val filteredRevenue = filteredTransactions.sumOf { it.totalPrice }
                    val totalUnitsStock = products.sumOf { it.stock }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1: Jumlah Stok HP / Barang
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = "Stok HP",
                                    tint = tenantAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Jumlah Stok HP/Barang", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$totalUnitsStock Unit",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Dari ${products.size} varian produk",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // Card 2: Jumlah Pemasukan
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Pemasukan",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (filterDay == "03" && filterMonthName == "Agustus" && filterYear == "2026") "Pemasukan Hari Ini" else "Pemasukan Terfilter",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatRupiah(filteredRevenue),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFFF59E0B),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Dari ${filteredTransactions.size} transaksi",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // 4. MENU GRID CARDS (Stock HP, Stock Aksesoris, Riwayat Transaksi, Kasir, Pembukuan, Pengaturan)
                    Text(
                        text = "Menu Layanan",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Row 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MenuGridItem(
                                title = "Stok HP",
                                icon = Icons.Default.PhoneAndroid,
                                iconColor = tenantAccent,
                                onClick = {
                                    selectedCategoryFilter = "HP"
                                    activeTab = 2
                                },
                                modifier = Modifier.weight(1f)
                            )
                            MenuGridItem(
                                title = "Stok Aksesoris",
                                icon = Icons.Default.Headphones,
                                iconColor = Color(0xFF10B981),
                                onClick = {
                                    selectedCategoryFilter = "Aksesoris"
                                    activeTab = 2
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Row 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MenuGridItem(
                                title = "Riwayat Transaksi",
                                icon = Icons.Default.History,
                                iconColor = Color(0xFF38BDF8),
                                onClick = { onNavigateToHistory?.invoke() ?: run { showHistoryDialog = true } },
                                modifier = Modifier.weight(1f)
                            )
                            MenuGridItem(
                                title = "POS Kasir",
                                icon = Icons.Default.PointOfSale,
                                iconColor = Color(0xFFF59E0B),
                                onClick = { activeTab = 3; onTabChange?.invoke(3) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Row 3
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MenuGridItem(
                                title = "Pembukuan",
                                icon = Icons.Default.BarChart,
                                iconColor = Color(0xFFEC4899),
                                onClick = { onNavigateToPembukuan?.invoke() ?: run { showAccountingDialog = true } },
                                modifier = Modifier.weight(1f)
                            )
                            MenuGridItem(
                                title = "Pengaturan",
                                icon = Icons.Default.Settings,
                                iconColor = Color(0xFFA855F7),
                                onClick = onSettingsClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // TAB 2: INVENTORY STOCK MANAGEMENT
            if (activeTab == 2) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("stok_search_bar"),
                        placeholder = { Text("Cari Produk", color = Color.Gray, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                            } else {
                                IconButton(onClick = { showStockSearchScanDialog = true }) {
                                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = tenantAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tenantAccent,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        textStyle = TextStyle(fontSize = 12.sp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category Selection Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (listOf("Semua") + categoriesList).forEach { categoryOpt ->
                            val isSelected = selectedCategoryFilter == categoryOpt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) tenantAccent else Color(0xFF1E293B))
                                    .clickable { selectedCategoryFilter = categoryOpt }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (categoryOpt == "Semua") "Semua Kategori" else categoryOpt,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val filteredProducts = products.filter { p ->
                        val matchesSearch = p.name.contains(searchQuery, ignoreCase = true) ||
                                p.sku.contains(searchQuery, ignoreCase = true)
                        val matchesCategory = when (selectedCategoryFilter) {
                            "Semua" -> true
                            "HP" -> p.category.contains("HP", ignoreCase = true) || p.category.contains("Phone", ignoreCase = true) || p.category.contains("Smartphone", ignoreCase = true)
                            "Aksesoris" -> p.category.contains("Aksesoris", ignoreCase = true) || p.category.contains("Accessory", ignoreCase = true) || p.category.contains("Casing", ignoreCase = true) || p.category.contains("Charger", ignoreCase = true) || p.category.contains("Earphone", ignoreCase = true) || p.category.contains("Headset", ignoreCase = true)
                            else -> p.category.contains(selectedCategoryFilter, ignoreCase = true)
                        }
                        matchesSearch && matchesCategory
                    }

                    if (filteredProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isEmpty()) "Belum ada produk." else "Produk tidak ditemukan.",
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredProducts) { item ->
                                val isLowStock = item.stock <= globalMinStockAlert
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("product_card_${item.id}"),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = item.name,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 15.sp
                                                )
                                                if (isLowStock) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0x33EF4444))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "LOW",
                                                            color = Color(0xFFEF4444),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "SKU: ${item.sku} | ${item.category}",
                                                color = Color.Gray,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                            Text(
                                                text = formatRupiah(item.price),
                                                color = tenantAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }

                                        // Stock control row
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Minus stock
                                            IconButton(
                                                onClick = {
                                                    if (item.stock > 0) {
                                                        coroutineScope.launch {
                                                            try {
                                                                ApiClient.getService().updateStock(
                                                                    item.id,
                                                                    StockUpdateRequest(tenant.dbName, item.stock - 1)
                                                                )
                                                                checkoutSuccessMessage = "✓ Stok ${item.name} berkurang menjadi ${item.stock - 1}"
                                                                refreshData()
                                                            } catch (e: Exception) {
                                                                dashboardError = "Gagal memperbarui stok: ${e.localizedMessage}"
                                                            }
                                                        }
                                                    }
                                                },
                                                colors = IconButtonDefaults.iconButtonColors(
                                                    containerColor = Color(0xFF0F172A),
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(14.dp))
                                            }

                                            // Stock Count text
                                            Text(
                                                text = item.stock.toString(),
                                                color = if (isLowStock) Color(0xFFEF4444) else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                modifier = Modifier.widthIn(min = 24.dp),
                                                textAlign = TextAlign.Center
                                            )

                                            // Plus stock
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        try {
                                                            ApiClient.getService().updateStock(
                                                                item.id,
                                                                StockUpdateRequest(tenant.dbName, item.stock + 1)
                                                            )
                                                            checkoutSuccessMessage = "✓ Stok ${item.name} bertambah menjadi ${item.stock + 1}"
                                                            refreshData()
                                                        } catch (e: Exception) {
                                                            dashboardError = "Gagal memperbarui stok: ${e.localizedMessage}"
                                                        }
                                                    }
                                                },
                                                colors = IconButtonDefaults.iconButtonColors(
                                                    containerColor = Color(0xFF0F172A),
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(14.dp))
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Delete Product
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        try {
                                                            ApiClient.getService().deleteProduct(item.id, tenant.dbName)
                                                            checkoutSuccessMessage = "✓ Produk '${item.name}' berhasil dihapus!"
                                                            refreshData()
                                                        } catch (e: Exception) {
                                                            dashboardError = "Gagal menghapus produk: ${e.localizedMessage}"
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Hapus",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(16.dp)
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

            // TAB 3: POS CHECKOUT CASHIER
            if (activeTab == 3) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Kasir POS (Point of Sales)",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    // Barcode scanning feature for Cashier
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, tenantAccent.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCashierScanDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode Kasir",
                                tint = tenantAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "PINDAI BARCODE PRODUK",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Checkout Transaction Success Banner
                    if (checkoutSuccessMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0x26F59E0B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Transaksi Selesai!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = checkoutSuccessMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }
                                IconButton(onClick = { checkoutSuccessMessage = null }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Product Selection for POS Checkout
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "1. Pilih Produk:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            // Horizontal scroll or grid of products to select
                            val availableProducts = products.filter { it.stock > 0 }
                            if (availableProducts.isEmpty()) {
                                Text(
                                    text = "Tidak ada produk dengan stok tersedia.",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                availableProducts.forEach { item ->
                                    val isSelected = selectedProductForCart?.id == item.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) tenantAccent.copy(alpha = 0.15f) else Color(0xFF0F172A))
                                            .border(1.dp, if (isSelected) tenantAccent else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedProductForCart = item
                                                cartQuantity = 1
                                                cartDiscount = ""
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                selectedProductForCart = item
                                                cartQuantity = 1
                                                cartDiscount = ""
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = tenantAccent)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("SKU: ${item.sku} | Stok: ${item.stock}", color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Text(
                                            text = formatRupiah(item.price),
                                            color = if (isSelected) tenantAccent else Color.LightGray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quantity Selection & Checkout Details
                    AnimatedVisibility(visible = selectedProductForCart != null) {
                        selectedProductForCart?.let { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "2. Atur Jumlah, Diskon & Checkout:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Jumlah Pembelian:", color = Color.LightGray, fontSize = 13.sp)

                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            IconButton(
                                                onClick = { if (cartQuantity > 1) cartQuantity-- },
                                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF0F172A))
                                            ) {
                                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Kurang", tint = Color.White)
                                            }
                                            Text(text = cartQuantity.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            IconButton(
                                                onClick = { if (cartQuantity < item.stock) cartQuantity++ },
                                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF0F172A))
                                            ) {
                                                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah", tint = Color.White)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = cartDiscount,
                                        onValueChange = { cartDiscount = it },
                                        label = { Text("Diskon (Rp)") },
                                        placeholder = { Text("0", color = Color.Gray) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = tenantAccent,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.LightGray
                                        ),
                                        singleLine = true
                                    )

                                    Divider(modifier = Modifier.padding(vertical = 14.dp), color = Color(0x1AFFFFFF))

                                    val rawTotal = item.price * cartQuantity
                                    val discountVal = cartDiscount.toDoubleOrNull() ?: 0.0
                                    val finalTotal = maxOf(0.0, rawTotal - discountVal)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Subtotal:", color = Color.Gray, fontSize = 12.sp)
                                        Text(formatRupiah(rawTotal), color = Color.LightGray, fontSize = 12.sp)
                                    }

                                    if (discountVal > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Diskon:", color = Color(0xFFEF4444), fontSize = 12.sp)
                                            Text("- ${formatRupiah(discountVal)}", color = Color(0xFFEF4444), fontSize = 12.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Total Pembayaran:", color = Color.LightGray, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = formatRupiah(finalTotal),
                                            color = tenantAccent,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (item.stock < cartQuantity) {
                                                return@Button
                                            }
                                            coroutineScope.launch {
                                                try {
                                                     val finalStock = item.stock - cartQuantity
                                                     ApiClient.getService().updateStock(
                                                         item.id,
                                                         StockUpdateRequest(tenant.dbName, finalStock)
                                                     )

                                                     val txId = "TX-${tenant.id.uppercase()}-${(1000..9999).random()}"
                                                     val nowMs = System.currentTimeMillis()
                                                     ApiClient.getService().addTransaction(
                                                         TransactionAddRequest(
                                                             db_name = tenant.dbName,
                                                             id = txId,
                                                             productName = item.name,
                                                             sku = item.sku,
                                                             quantity = cartQuantity,
                                                             totalPrice = finalTotal,
                                                             timestamp = nowMs,
                                                             operator = tenant.ownerName
                                                         )
                                                     )

                                                     checkoutSuccessMessage = "$cartQuantity x ${item.name} berhasil dibeli."
                                                     selectedProductForCart = null
                                                     cartQuantity = 1
                                                     cartDiscount = ""
                                                     refreshData()
                                                } catch (e: Exception) {
                                                    dashboardError = "Transaksi POS gagal: ${e.localizedMessage}"
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = tenantAccent),
                                        enabled = item.stock >= cartQuantity && item.stock > 0
                                    ) {
                                        Icon(imageVector = Icons.Default.ShoppingCartCheckout, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("PROSES KASIR & CETAK", fontWeight = FontWeight.Bold)
                                    }

                                    if (item.stock <= 0) {
                                        Text(
                                            text = "Stok habis! Tidak dapat memproses transaksi.",
                                            color = Color(0xFFEF4444),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }

        // TAMBAH PRODUK MODAL SHEET
        if (showAddProductModal) {
            AlertDialog(
                onDismissRequest = { showAddProductModal = false },
                title = {
                    Text(
                        text = "Tambah Stok / Produk Baru",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.width(300.dp)
                    ) {
                        if (addProductError != null) {
                            Text(
                                text = addProductError ?: "",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = newProdName,
                            onValueChange = { newProdName = it },
                            label = { Text("Nama Produk") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tenantAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            )
                        )

                        OutlinedTextField(
                            value = newProdSku,
                            onValueChange = { newProdSku = it },
                            label = { Text("SKU") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { showAddProductScanDialog = true }) {
                                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = tenantAccent)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tenantAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            )
                        )

                        OutlinedTextField(
                            value = newProdCategory,
                            onValueChange = { newProdCategory = it },
                            label = { Text("Kategori") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tenantAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categoriesList.forEach { category ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF334155))
                                        .clickable { newProdCategory = category }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(text = category, color = Color.LightGray, fontSize = 11.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = newProdModal,
                            onValueChange = { newProdModal = it },
                            label = { Text("Harga Modal (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tenantAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            )
                        )

                        OutlinedTextField(
                            value = newProdPrice,
                            onValueChange = { newProdPrice = it },
                            label = { Text("Harga Jual (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tenantAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            )
                        )

                        OutlinedTextField(
                            value = newProdStock,
                            onValueChange = { newProdStock = it },
                            label = { Text("Stok Awal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tenantAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val priceParsed = newProdPrice.toDoubleOrNull()
                            val modalParsed = newProdModal.toDoubleOrNull() ?: 0.0
                            val stockParsed = newProdStock.toIntOrNull()

                            if (newProdName.isBlank() || newProdSku.isBlank() || newProdCategory.isBlank() || priceParsed == null || stockParsed == null) {
                                addProductError = "Semua kolom wajib diisi dengan benar!"
                                return@Button
                            }

                            coroutineScope.launch {
                                try {
                                    val response = ApiClient.getService().addProduct(
                                        ProductAddRequest(
                                            db_name = tenant.dbName,
                                            name = newProdName,
                                            sku = newProdSku,
                                            category = newProdCategory,
                                            price = priceParsed,
                                            modal_price = modalParsed,
                                            stock = stockParsed,
                                            min_stock_alert = 5
                                        )
                                    )
                                    if (response.isSuccessful && response.body()?.get("success") == true) {
                                        val addedName = newProdName
                                        showAddProductModal = false
                                        newProdName = ""
                                        newProdSku = ""
                                        newProdCategory = ""
                                        newProdModal = ""
                                        newProdPrice = ""
                                        newProdStock = ""
                                        checkoutSuccessMessage = "✓ Produk '$addedName' berhasil ditambahkan ke database!"
                                        refreshData()
                                    } else {
                                        addProductError = "Gagal menyimpan produk ke database VPS."
                                    }
                                } catch (e: Exception) {
                                    addProductError = "Koneksi Error: ${e.localizedMessage}"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tenantAccent)
                    ) {
                        Text("Simpan", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddProductModal = false }) {
                        Text("Batal", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1E293B),
                textContentColor = Color.LightGray,
                titleContentColor = Color.White
            )
        }

        // RIWAYAT TRANSAKSI MODAL
        if (showHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showHistoryDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Riwayat Transaksi",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        IconButton(onClick = { showHistoryDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.LightGray)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp)
                    ) {
                        Text(
                            text = "Menampilkan ${filteredTransactions.size} transaksi (Filter: Tgl $filterDay, Bln $filterMonthName, Thn $filterYear)",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        if (filteredTransactions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Belum ada transaksi terfilter.", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(filteredTransactions) { tx ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = tx.id,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = tx.timestamp,
                                                    color = Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = tx.productName,
                                                        color = Color.LightGray,
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 14.sp
                                                    )
                                                    Text(
                                                        text = "${tx.quantity} pcs | SKU: ${tx.sku}",
                                                        color = Color.Gray,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    )
                                                }
                                                Text(
                                                    text = formatRupiah(tx.totalPrice),
                                                    color = Color(0xFFF59E0B),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                            }

                                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0x0FFFFFFF))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Kasir: ${tx.operator}",
                                                    color = Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0x1AF59E0B))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Lunas",
                                                        color = Color(0xFFF59E0B),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showHistoryDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = tenantAccent)
                    ) {
                        Text("Tutup")
                    }
                },
                containerColor = Color(0xFF0F172A)
            )
        }

        // PEMBUKUAN MODAL
        if (showAccountingDialog) {
            AlertDialog(
                onDismissRequest = { showAccountingDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Laporan Pembukuan",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        IconButton(onClick = { showAccountingDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.LightGray)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Analisis Keuangan Tenant (Filter: Tgl $filterDay, Bln $filterMonthName, Thn $filterYear)",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )

                        val totalRevenueVal = filteredTransactions.sumOf { it.totalPrice }
                        val totalQtySold = filteredTransactions.sumOf { it.quantity }
                        val avgSalesVal = if (filteredTransactions.isNotEmpty()) totalRevenueVal / filteredTransactions.size else 0.0
                        
                        // Card 1: Ringkasan Pendapatan
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Total Omset Penjualan", color = Color.Gray, fontSize = 12.sp)
                                Text(
                                    text = formatRupiah(totalRevenueVal),
                                    color = Color(0xFF10B981),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0x0FFFFFFF))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Produk Terjual", color = Color.Gray, fontSize = 12.sp)
                                    Text("$totalQtySold pcs", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Rata-rata Penjualan", color = Color.Gray, fontSize = 12.sp)
                                    Text(formatRupiah(avgSalesVal), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Card 2: Estimasi Keuntungan Bersih
                        val estimatedProfit = totalRevenueVal * 0.35 // 35% margin
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Estimasi Keuntungan Bersih", color = Color.Gray, fontSize = 12.sp)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x1A10B981))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Margin 35%", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = formatRupiah(estimatedProfit),
                                    color = Color(0xFF38BDF8),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                                Text(
                                    text = "Dihitung dari rata-rata harga grosir produk smartphone dan aksesoris.",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Card 3: Perbandingan Pemasukan Berdasarkan Kasir
                        val operatorSales = filteredTransactions.groupBy { it.operator }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Kontribusi Penjualan Kasir", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                if (operatorSales.isEmpty()) {
                                    Text("Belum ada kontribusi kasir terdeteksi.", color = Color.Gray, fontSize = 12.sp)
                                } else {
                                    operatorSales.forEach { (operator, txs) ->
                                        val opTotal = txs.sumOf { it.totalPrice }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = tenantAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(operator, color = Color.LightGray, fontSize = 12.sp)
                                            }
                                            Text(
                                                text = "${txs.size} tx | ${formatRupiah(opTotal)}",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAccountingDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = tenantAccent)
                    ) {
                        Text("Selesai")
                    }
                },
                containerColor = Color(0xFF0F172A)
            )
        }

        // Render Barcode Scanner Dialogs
        if (showStockSearchScanDialog) {
            BarcodeScannerDialog(
                onDismissRequest = { showStockSearchScanDialog = false },
                onBarcodeScanned = { barcode ->
                    searchQuery = barcode
                    showStockSearchScanDialog = false
                }
            )
        }

        if (showAddProductScanDialog) {
            BarcodeScannerDialog(
                onDismissRequest = { showAddProductScanDialog = false },
                onBarcodeScanned = { barcode ->
                    newProdSku = barcode
                    showAddProductScanDialog = false
                }
            )
        }

        if (showCashierScanDialog) {
            BarcodeScannerDialog(
                onDismissRequest = { showCashierScanDialog = false },
                onBarcodeScanned = { barcode ->
                    val foundProduct = products.find { it.sku.equals(barcode, ignoreCase = true) }
                    if (foundProduct != null) {
                        selectedProductForCart = foundProduct
                        cartQuantity = 1
                    } else {
                        checkoutSuccessMessage = "Produk dengan SKU $barcode tidak ditemukan di server real-time."
                    }
                    showCashierScanDialog = false
                }
            )
        }
    }
}

@Composable
fun MenuGridItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


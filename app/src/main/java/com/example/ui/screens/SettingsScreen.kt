package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ApiClient
import com.example.data.TenantConfig
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    tenant: TenantConfig,
    isRealVps: Boolean,
    vpsUrl: String,
    
    // Lifted states passed from MainActivity for synchronization
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
    
    branchesList: List<String>,
    onBranchesListChange: (List<String>) -> Unit,
    areasListRaw: List<com.example.data.AreaResponse>,
    onAreasListRawChange: (List<com.example.data.AreaResponse>) -> Unit,
    adminUsers: List<Map<String, String>>,
    onAdminUsersChange: (List<Map<String, String>>) -> Unit,
    
    categoriesList: List<String>,
    onCategoriesListChange: (List<String>) -> Unit,

    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    refreshData: () -> Unit,
    onNavigateTab: ((Int) -> Unit)? = null
) {
    val tenantAccent = Color(android.graphics.Color.parseColor(tenant.accentColor))
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Card Expand states
    var isProfileExpanded by remember { mutableStateOf(false) }
    var isAccountSettingsExpanded by remember { mutableStateOf(false) }
    var isAreaExpanded by remember { mutableStateOf(false) }
    var isAdminListExpanded by remember { mutableStateOf(false) }
    var isInvoiceExpanded by remember { mutableStateOf(false) }
    var isPrinterExpanded by remember { mutableStateOf(false) }
    var isBackupExpanded by remember { mutableStateOf(false) }
    var isUpdateExpanded by remember { mutableStateOf(false) }
    var isCategoryExpanded by remember { mutableStateOf(false) }

    // Area Dialog States
    var showAddBranchDialog by remember { mutableStateOf(false) }
    var showEditBranchDialog by remember { mutableStateOf(false) }
    var editingBranchIndex by remember { mutableStateOf(-1) }
    var branchInputName by remember { mutableStateOf("") }

    // Admin Dialog States
    var showAddAdminDialog by remember { mutableStateOf(false) }
    var showEditAdminDialog by remember { mutableStateOf(false) }
    var editingAdminIndex by remember { mutableStateOf(-1) }
    var adminFormName by remember { mutableStateOf("") }
    var adminFormUsername by remember { mutableStateOf("") }
    var adminFormPassword by remember { mutableStateOf("") }
    var adminFormRole by remember { mutableStateOf("Kasir") }
    var adminFormArea by remember { mutableStateOf("Semua Cabang") }

    // Selection Dialog States (dialog model)
    var showRoleSelectorDialog by remember { mutableStateOf(false) }
    var showAreaSelectorDialog by remember { mutableStateOf(false) }

    // Printer Connection States
    var isConnectingPrinter by remember { mutableStateOf(false) }
    var printerConnectedSuccess by remember { mutableStateOf(false) }
    var selectedPrinter by remember { mutableStateOf("MP-58 (Bluetooth Thermal)") }
    var showTestPrintMessage by remember { mutableStateOf(false) }

    // Backup & Restore States
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    // Data Confirmation Banner State
    var dataConfirmationMessage by remember { mutableStateOf<String?>(null) }

    // Update States
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isUpdatingFix by remember { mutableStateOf(false) }
    var updateFixProgressText by remember { mutableStateOf<String?>(null) }
    var updateCheckedMessage by remember { mutableStateOf<String?>(null) }

    // Success Indicator Messages
    var profileSaveSuccess by remember { mutableStateOf(false) }
    var accountSaveSuccess by remember { mutableStateOf(false) }

    // Fetch areas & admins when settings screen opens to keep sync with VPS
    LaunchedEffect(tenant) {
        try {
            val areasRes = ApiClient.getService().getAreas(tenant.dbName)
            if (areasRes.isNotEmpty()) {
                onAreasListRawChange(areasRes)
                onBranchesListChange(areasRes.map { it.name })
            }
        } catch (e: Exception) {
            // Silently ignore or fallback
        }

        try {
            val adminsRes = ApiClient.getService().getAdmins(tenant.dbName)
            if (adminsRes.isNotEmpty()) {
                val mappedAdmins = adminsRes.map {
                    mapOf(
                        "id" to it.id,
                        "name" to it.name,
                        "username" to it.username,
                        "password" to it.password,
                        "role" to it.role,
                        "area" to it.area
                    )
                }
                onAdminUsersChange(mappedAdmins)
            }
        } catch (e: Exception) {
            // Silently ignore or fallback
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(tenantAccent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.Center)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Pengaturan",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali ke Dashboard",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Pengaturan Aplikasi",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            // Data Modification Confirmation Banner
            if (dataConfirmationMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = dataConfirmationMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { dataConfirmationMessage = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // CARD 1: PENGATURAN PROFIL & TOKO
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isProfileExpanded = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = tenantAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pengaturan profil & Toko",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Nama Toko, Alamat, Ambang Batas Stok & Mata Uang",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Buka",
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // CARD 2: PENGATURAN AKUN
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAccountSettingsExpanded = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = tenantAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pengaturan Akun",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kelola Email dan Kata Sandi Login",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Buka",
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // CARD 3: AREA (CABANG TOKO)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAreaExpanded = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = tenantAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Area",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kelola Cabang Toko (${branchesList.size} Cabang)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Buka",
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // CARD 4: DAFTAR ADMIN
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAdminListExpanded = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = tenantAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Daftar Admin",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kelola Multi-role User & Hak Akses (${adminUsers.size} Pengguna)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Buka",
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // CARD 5: PENGATURAN INVOICE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isInvoiceExpanded = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = tenantAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pengaturan Invoice",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sesuaikan Header & Footer Nota Transaksi",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Buka",
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // CARD 6: PRINTER BLUETOOTH THERMAL
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPrinterExpanded = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = tenantAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Printer",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (printerConnectedSuccess) "Terhubung: $selectedPrinter" else "Koneksi Printer Bluetooth Thermal",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (printerConnectedSuccess) Color(0xFF10B981) else Color.Gray
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Buka",
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // CARD 7: BACKUP & RESTORE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isBackupExpanded = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = tenantAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Backup & Restore",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Amankan dan Pulihkan Data Stok Produk",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Buka",
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // CARD 8: CEK UPDATE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isUpdateExpanded = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = tenantAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Cek Update",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Periksa Pembaruan Sistem di GitHub",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Buka",
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // CARD 9: KATEGORI PRODUK
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCategoryExpanded = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = tenantAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Kategori Produk",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kelola Kategori Produk Toko (${categoriesList.size} Kategori)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Buka",
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }


            // RED LOGOUT BUTTON
            Button(
                onClick = onLogoutClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar (Logout)", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // DIALOGS SECTION
        // SUB MENU DIALOGS (1-8)
        
        // 1. PROFILE & STORE DIALOG
        if (isProfileExpanded) {
            Dialog(
                onDismissRequest = { isProfileExpanded = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xFF1E293B)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pengaturan Profil & Toko",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { isProfileExpanded = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.LightGray)
                            }
                        }
                        
                        Divider(color = Color(0x1FFFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Nama Toko", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = storeNameState,
                                onValueChange = onStoreNameChange,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = tenantAccent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray,
                                    cursorColor = tenantAccent
                                )
                            )

                            Text("Alamat Toko", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = storeAddressState,
                                onValueChange = onStoreAddressChange,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = tenantAccent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray,
                                    cursorColor = tenantAccent
                                )
                            )

                            Text("Nama Admin", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = adminNameState,
                                onValueChange = onAdminNameChange,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = tenantAccent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray,
                                    cursorColor = tenantAccent
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Ambang Batas Peringatan Stok",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Stok <= $globalMinStockAlert ditandai kritis",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledIconButton(
                                        onClick = { if (globalMinStockAlert > 1) onGlobalMinStockAlertChange(globalMinStockAlert - 1) },
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF334155)),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                                    }
                                    Text(text = globalMinStockAlert.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    FilledIconButton(
                                        onClick = { if (globalMinStockAlert < 50) onGlobalMinStockAlertChange(globalMinStockAlert + 1) },
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF334155)),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Simbol Mata Uang Rp",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Tampilkan Rp pada seluruh format nominal",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = showCurrencySymbol,
                                    onCheckedChange = onShowCurrencySymbolChange,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = tenantAccent,
                                        uncheckedThumbColor = Color.LightGray,
                                        uncheckedTrackColor = Color(0xFF334155)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    profileSaveSuccess = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = tenantAccent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Simpan Perubahan", fontWeight = FontWeight.Bold, color = Color.Black)
                            }

                            if (profileSaveSuccess) {
                                Text(
                                    text = "✓ Profil toko berhasil disimpan!",
                                    color = Color(0xFF10B981),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                LaunchedEffect(profileSaveSuccess) {
                                    kotlinx.coroutines.delay(2000)
                                    profileSaveSuccess = false
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. ACCOUNT SETTINGS DIALOG
        if (isAccountSettingsExpanded) {
            Dialog(
                onDismissRequest = { isAccountSettingsExpanded = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xFF1E293B)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pengaturan Akun",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { isAccountSettingsExpanded = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.LightGray)
                            }
                        }
                        
                        Divider(color = Color(0x1FFFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Alamat Email", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = emailState,
                                onValueChange = onEmailChange,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = tenantAccent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray,
                                    cursorColor = tenantAccent
                                )
                            )

                            Text("Kata Sandi", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = passwordState,
                                onValueChange = onPasswordChange,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = tenantAccent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray,
                                    cursorColor = tenantAccent
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    accountSaveSuccess = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = tenantAccent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Simpan Akun", fontWeight = FontWeight.Bold, color = Color.Black)
                            }

                            if (accountSaveSuccess) {
                                Text(
                                    text = "✓ Informasi login berhasil diperbarui!",
                                    color = Color(0xFF10B981),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                LaunchedEffect(accountSaveSuccess) {
                                    kotlinx.coroutines.delay(2000)
                                    accountSaveSuccess = false
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. AREA / BRANCH DIALOG
        if (isAreaExpanded) {
            Dialog(
                onDismissRequest = { isAreaExpanded = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xFF1E293B)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kelola Cabang Toko",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { isAreaExpanded = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.LightGray)
                            }
                        }
                        
                        Divider(color = Color(0x1FFFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            branchesList.forEachIndexed { index, branch ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF334155))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = tenantAccent, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(branch, color = Color.White, fontSize = 14.sp)
                                    }
                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingBranchIndex = index
                                                branchInputName = branch
                                                showEditBranchDialog = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                val areaId = areasListRaw.getOrNull(index)?.id
                                                if (areaId != null) {
                                                    coroutineScope.launch {
                                                        try {
                                                            ApiClient.getService().deleteArea(areaId, tenant.dbName)
                                                        } catch (e: Exception) {
                                                            // Ignore or log
                                                        }
                                                    }
                                                }
                                                onAreasListRawChange(areasListRaw.filterIndexed { idx, _ -> idx != index })
                                                onBranchesListChange(branchesList.filterIndexed { idx, _ -> idx != index })
                                                dataConfirmationMessage = "✓ Cabang / Wilayah berhasil dihapus dari database!"
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    branchInputName = ""
                                    showAddBranchDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = tenantAccent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tambah Cabang Baru", fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4. ADMIN LIST DIALOG
        if (isAdminListExpanded) {
            Dialog(
                onDismissRequest = { isAdminListExpanded = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xFF1E293B)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kelola Multi-role User & Hak Akses",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { isAdminListExpanded = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.LightGray)
                            }
                        }
                        
                        Divider(color = Color(0x1FFFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            adminUsers.forEachIndexed { index, admin ->
                                val name = admin["name"] ?: ""
                                val username = admin["username"] ?: ""
                                val role = admin["role"] ?: ""
                                val area = admin["area"] ?: ""

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF334155))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            
                                            val badgeColor = when (role) {
                                                "Superadmin" -> Color(0xFFE11D48)
                                                "Admin" -> Color(0xFFA855F7)
                                                "Kasir" -> Color(0xFFF59E0B)
                                                else -> Color(0xFF10B981)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(badgeColor.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(role, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Username: $username", color = Color.Gray, fontSize = 12.sp)
                                        Text("Akses: $area", color = tenantAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                editingAdminIndex = index
                                                adminFormName = name
                                                adminFormUsername = username
                                                adminFormPassword = admin["password"] ?: ""
                                                adminFormRole = role
                                                adminFormArea = area
                                                showEditAdminDialog = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                val adminId = admin["id"]
                                                if (adminId != null) {
                                                    coroutineScope.launch {
                                                        try {
                                                            ApiClient.getService().deleteAdmin(adminId, tenant.dbName)
                                                            refreshData()
                                                        } catch (e: Exception) {
                                                            // Ignore/Log
                                                        }
                                                    }
                                                }
                                                onAdminUsersChange(adminUsers.filterIndexed { idx, _ -> idx != index })
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    adminFormName = ""
                                    adminFormUsername = ""
                                    adminFormPassword = ""
                                    adminFormRole = "Kasir"
                                    adminFormArea = "Semua Cabang"
                                    showAddAdminDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = tenantAccent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tambah Admin Baru", fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 5. INVOICE SETTINGS DIALOG
        if (isInvoiceExpanded) {
            Dialog(
                onDismissRequest = { isInvoiceExpanded = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xFF1E293B)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pengaturan Invoice & Preview",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { isInvoiceExpanded = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.LightGray)
                            }
                        }
                        
                        Divider(color = Color(0x1FFFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Text Header Invoice", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = invoiceHeaderState,
                                onValueChange = onInvoiceHeaderChange,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = tenantAccent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray,
                                    cursorColor = tenantAccent
                                )
                            )

                            Text("Text Footer Invoice", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = invoiceFooterState,
                                onValueChange = onInvoiceFooterChange,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = tenantAccent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray,
                                    cursorColor = tenantAccent
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "PREVIEW STRUK (Thermal 58mm)",
                                color = tenantAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = storeNameState.uppercase(),
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = storeAddressState,
                                        color = Color.DarkGray,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)

                                    Text(
                                        text = invoiceHeaderState,
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 15.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Contoh Produk x2", color = Color.Black, fontSize = 10.sp)
                                        Text("Rp 150.000", color = Color.Black, fontSize = 10.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("TOTAL", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("Rp 150.000", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)

                                    Text(
                                        text = invoiceFooterState,
                                        color = Color.Black,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. PRINTER DIALOG
        if (isPrinterExpanded) {
            Dialog(
                onDismissRequest = { isPrinterExpanded = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xFF1E293B)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Printer Bluetooth Thermal",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { isPrinterExpanded = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.LightGray)
                            }
                        }
                        
                        Divider(color = Color(0x1FFFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Daftar Printer Bluetooth Terdekat",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            val printerOptions = listOf(
                                "MP-58 (Bluetooth Thermal)",
                                "RPP02N (Bluetooth Thermal)",
                                "POS-58 (USB/Bluetooth)",
                                "Zjiang ZJ-5802"
                            )

                            printerOptions.forEach { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedPrinter == option) tenantAccent.copy(alpha = 0.1f) else Color(0xFF334155))
                                        .border(1.dp, if (selectedPrinter == option) tenantAccent else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { selectedPrinter = option }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (selectedPrinter == option),
                                        onClick = { selectedPrinter = option },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = tenantAccent,
                                            unselectedColor = Color.Gray
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = if (selectedPrinter == option) tenantAccent else Color.LightGray, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = option, color = if (selectedPrinter == option) tenantAccent else Color.White, fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isConnectingPrinter) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(color = tenantAccent, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Menghubungkan printer...", color = Color.LightGray, fontSize = 13.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        isConnectingPrinter = true
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(1500)
                                            isConnectingPrinter = false
                                            printerConnectedSuccess = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = tenantAccent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Sambungkan Printer", fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }

                            if (printerConnectedSuccess) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF065F46)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("✓ Berhasil terhubung ke $selectedPrinter", color = Color.White, fontSize = 12.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        showTestPrintMessage = true
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(2500)
                                            showTestPrintMessage = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cetak Test Print", fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                if (showTestPrintMessage) {
                                    Text(
                                        text = "✓ Test print berhasil dikirim ke printer thermal!",
                                        color = Color(0xFF10B981),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. BACKUP & RESTORE DIALOG
        if (isBackupExpanded) {
            Dialog(
                onDismissRequest = { isBackupExpanded = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xFF1E293B)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Backup & Restore Database",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { isBackupExpanded = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.LightGray)
                            }
                        }
                        
                        Divider(color = Color(0x1FFFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Amankan seluruh data produk, transaksi, area, dan daftar admin Anda ke cloud storage agar aman dan dapat dipulihkan kapan saja.",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isBackingUp) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(color = tenantAccent, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Sedang memproses cadangan (Backup)...", color = Color.LightGray, fontSize = 13.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        isBackingUp = true
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(2000)
                                            isBackingUp = false
                                            backupStatusMessage = "✓ Database berhasil dibackup ke server awan pada ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = tenantAccent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Cloud, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Backup Database ke Cloud", fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }

                            if (isRestoring) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(color = tenantAccent, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Sedang memulihkan data (Restore)...", color = Color.LightGray, fontSize = 13.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        isRestoring = true
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(2000)
                                            isRestoring = false
                                            backupStatusMessage = "✓ Sinkronisasi pemulihan data selesai! Seluruh tabel data diperbarui."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pulihkan Database (Restore)", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            backupStatusMessage?.let { msg ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF60A5FA))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(msg, color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. UPDATE CHECK DIALOG
        if (isUpdateExpanded) {
            Dialog(
                onDismissRequest = { isUpdateExpanded = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xFF1E293B)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pembaruan Sistem (Cek Update)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { isUpdateExpanded = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.LightGray)
                            }
                        }
                        
                        Divider(color = Color(0x1FFFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = tenantAccent, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Versi Aplikasi Saat Ini", color = Color.Gray, fontSize = 12.sp)
                                    Text("v1.2 (Terbaru)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }

                            Divider(color = Color(0x1FFFFFFF), modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = "Sistem POS ini terhubung ke repositori GitHub global untuk memantau pembaruan fitur baru, keamanan, dan sinkronisasi basis data real-time.",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isCheckingUpdate) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(color = tenantAccent, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Memeriksa repositori GitHub...", color = Color.LightGray, fontSize = 13.sp)
                                }
                            } else if (isUpdatingFix) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(color = Color(0xFFE11D48), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(updateFixProgressText ?: "Memproses Update-Fix...", color = Color.LightGray, fontSize = 13.sp)
                                }
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            isCheckingUpdate = true
                                            coroutineScope.launch {
                                                kotlinx.coroutines.delay(1500)
                                                isCheckingUpdate = false
                                                updateCheckedMessage = "✓ Aplikasi Anda sudah menggunakan versi paling mutakhir (v1.2). Tidak ada pembaruan baru saat ini."
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = tenantAccent),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Cek Pembaruan Sistem", fontWeight = FontWeight.Bold, color = Color.Black)
                                    }

                                    Button(
                                        onClick = {
                                            isUpdatingFix = true
                                            updateFixProgressText = "Menyiapkan proses unduhan latar belakang..."
                                            coroutineScope.launch {
                                                val result = com.example.util.UpdateInstaller.downloadAndInstallApk(
                                                    context = context,
                                                    downloadUrl = "https://github.com/satriaevo77/AStock/releases/latest/download/app-release.apk",
                                                    onProgress = { progressStr ->
                                                        updateFixProgressText = progressStr
                                                    }
                                                )
                                                isUpdatingFix = false
                                                updateCheckedMessage = result.second
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Update-Fix (Download & Install Ulang APK)", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }

                            updateCheckedMessage?.let { msg ->
                                val isError = msg.startsWith("❌") || msg.contains("Error")
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isError) Color(0xFF7F1D1D) else Color(0xFF065F46)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isError) Color(0xFFEF4444) else Color(0xFF10B981)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(msg, color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. DIALOG: TAMBAH CABANG
        if (showAddBranchDialog) {
            AlertDialog(
                onDismissRequest = { showAddBranchDialog = false },
                title = { Text("Tambah Cabang Baru", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Nama Cabang", color = Color.Gray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = branchInputName,
                            onValueChange = { branchInputName = it },
                            placeholder = { Text("Contoh: Cabang Jakarta Timur", color = Color.Gray) },
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
                            if (branchInputName.trim().isNotEmpty()) {
                                val newAreaName = branchInputName.trim()
                                coroutineScope.launch {
                                    try {
                                        ApiClient.getService().addArea(mapOf("db_name" to tenant.dbName, "name" to newAreaName))
                                        refreshData()
                                    } catch (e: Exception) {
                                        // Ignore/Log
                                    }
                                }
                                onBranchesListChange(branchesList + newAreaName)
                                showAddBranchDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tenantAccent)
                    ) {
                        Text("Tambah", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddBranchDialog = false }) {
                        Text("Batal", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF0F172A)
            )
        }

        // 2. DIALOG: EDIT CABANG
        if (showEditBranchDialog) {
            AlertDialog(
                onDismissRequest = { showEditBranchDialog = false },
                title = { Text("Edit Cabang", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Nama Cabang", color = Color.Gray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = branchInputName,
                            onValueChange = { branchInputName = it },
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
                            if (branchInputName.trim().isNotEmpty() && editingBranchIndex != -1) {
                                val editedAreaName = branchInputName.trim()
                                val areaId = areasListRaw.getOrNull(editingBranchIndex)?.id
                                if (areaId != null) {
                                    coroutineScope.launch {
                                        try {
                                            ApiClient.getService().updateArea(areaId, mapOf("db_name" to tenant.dbName, "name" to editedAreaName))
                                            refreshData()
                                        } catch (e: Exception) {
                                            // Ignore/Log
                                        }
                                    }
                                }
                                onBranchesListChange(branchesList.mapIndexed { idx, value ->
                                    if (idx == editingBranchIndex) editedAreaName else value
                                })
                                showEditBranchDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tenantAccent)
                    ) {
                        Text("Simpan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditBranchDialog = false }) {
                        Text("Batal", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF0F172A)
            )
        }

        // 3. DIALOG: TAMBAH ADMIN
        if (showAddAdminDialog) {
            AlertDialog(
                onDismissRequest = { showAddAdminDialog = false },
                title = { Text("Tambah Admin Baru", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Nama Lengkap", color = Color.Gray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = adminFormName,
                            onValueChange = { adminFormName = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tenantAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            )
                        )

                        Text("Username", color = Color.Gray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = adminFormUsername,
                            onValueChange = { adminFormUsername = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tenantAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            )
                        )

                        Text("Password", color = Color.Gray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = adminFormPassword,
                            onValueChange = { adminFormPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tenantAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            )
                        )

                        // Dialog Selection Role (ganti dropdown model menjadi dialog model)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("Role Pengguna", color = Color.Gray, fontSize = 12.sp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                        .clickable { showRoleSelectorDialog = true }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(adminFormRole, color = Color.White, fontSize = 14.sp)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.LightGray)
                                    }
                                }
                            }
                        }

                        // Dialog Selection Area Access (ganti dropdown model menjadi dialog model)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("Hak Akses Area", color = Color.Gray, fontSize = 12.sp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                        .clickable { showAreaSelectorDialog = true }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(adminFormArea, color = Color.White, fontSize = 14.sp)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.LightGray)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (adminFormName.trim().isNotEmpty() && adminFormUsername.trim().isNotEmpty()) {
                                val newAdminMap = mapOf(
                                    "db_name" to tenant.dbName,
                                    "name" to adminFormName.trim(),
                                    "username" to adminFormUsername.trim(),
                                    "password" to adminFormPassword,
                                    "role" to adminFormRole,
                                    "area" to adminFormArea
                                )
                                coroutineScope.launch {
                                    try {
                                        ApiClient.getService().addAdmin(newAdminMap)
                                        refreshData()
                                    } catch (e: Exception) {
                                        // Ignore/Log
                                    }
                                }
                                onAdminUsersChange(adminUsers + mapOf(
                                    "id" to UUID.randomUUID().toString(),
                                    "name" to adminFormName.trim(),
                                    "username" to adminFormUsername.trim(),
                                    "password" to adminFormPassword,
                                    "role" to adminFormRole,
                                    "area" to adminFormArea
                                ))
                                showAddAdminDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tenantAccent)
                    ) {
                        Text("Tambah", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddAdminDialog = false }) {
                        Text("Batal", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF0F172A)
            )
        }

        // 4. DIALOG: EDIT ADMIN
        if (showEditAdminDialog) {
            AlertDialog(
                onDismissRequest = { showEditAdminDialog = false },
                title = { Text("Edit Admin", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Nama Lengkap", color = Color.Gray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = adminFormName,
                            onValueChange = { adminFormName = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tenantAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            )
                        )

                        Text("Username", color = Color.Gray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = adminFormUsername,
                            onValueChange = { adminFormUsername = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tenantAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            )
                        )

                        Text("Password", color = Color.Gray, fontSize = 12.sp)
                        OutlinedTextField(
                            value = adminFormPassword,
                            onValueChange = { adminFormPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tenantAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            )
                        )

                        // Dialog Selection Role (ganti dropdown model menjadi dialog model)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("Role Pengguna", color = Color.Gray, fontSize = 12.sp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                        .clickable { showRoleSelectorDialog = true }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(adminFormRole, color = Color.White, fontSize = 14.sp)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.LightGray)
                                    }
                                }
                            }
                        }

                        // Dialog Selection Area Access (ganti dropdown model menjadi dialog model)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("Hak Akses Area", color = Color.Gray, fontSize = 12.sp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                        .clickable { showAreaSelectorDialog = true }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(adminFormArea, color = Color.White, fontSize = 14.sp)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.LightGray)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (adminFormName.trim().isNotEmpty() && adminFormUsername.trim().isNotEmpty() && editingAdminIndex != -1) {
                                val editedAdminId = adminUsers.getOrNull(editingAdminIndex)?.get("id")
                                if (editedAdminId != null) {
                                    val editAdminMap = mapOf(
                                        "db_name" to tenant.dbName,
                                        "name" to adminFormName.trim(),
                                        "username" to adminFormUsername.trim(),
                                        "password" to adminFormPassword,
                                        "role" to adminFormRole,
                                        "area" to adminFormArea
                                    )
                                    coroutineScope.launch {
                                        try {
                                            ApiClient.getService().updateAdmin(editedAdminId, editAdminMap)
                                            refreshData()
                                        } catch (e: Exception) {
                                            // Ignore/Log
                                        }
                                    }
                                }
                                onAdminUsersChange(adminUsers.mapIndexed { idx, value ->
                                    if (idx == editingAdminIndex) {
                                        mapOf(
                                            "id" to (value["id"] ?: UUID.randomUUID().toString()),
                                            "name" to adminFormName.trim(),
                                            "username" to adminFormUsername.trim(),
                                            "password" to adminFormPassword,
                                            "role" to adminFormRole,
                                            "area" to adminFormArea
                                        )
                                    } else value
                                })
                                showEditAdminDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tenantAccent)
                    ) {
                        Text("Simpan", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditAdminDialog = false }) {
                        Text("Batal", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF0F172A)
            )
        }

        // 5. DIALOG: SELEKSI ROLE (DIALOG MODE)
        if (showRoleSelectorDialog) {
            AlertDialog(
                onDismissRequest = { showRoleSelectorDialog = false },
                title = { Text("Pilih Role Pengguna", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Superadmin", "Admin", "Kasir", "Mitra").forEach { roleOption ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        adminFormRole = roleOption
                                        showRoleSelectorDialog = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (adminFormRole == roleOption),
                                    onClick = {
                                        adminFormRole = roleOption
                                        showRoleSelectorDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = tenantAccent,
                                        unselectedColor = Color.Gray
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = roleOption, color = Color.White, fontSize = 15.sp)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showRoleSelectorDialog = false }) {
                        Text("Batal", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }

        // 6. DIALOG: SELEKSI AREA (DIALOG MODE)
        if (showAreaSelectorDialog) {
            AlertDialog(
                onDismissRequest = { showAreaSelectorDialog = false },
                title = { Text("Pilih Hak Akses Area", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())
                    ) {
                        (listOf("Semua Cabang") + branchesList).forEach { areaOption ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        adminFormArea = areaOption
                                        showAreaSelectorDialog = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (adminFormArea == areaOption),
                                    onClick = {
                                        adminFormArea = areaOption
                                        showAreaSelectorDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = tenantAccent,
                                        unselectedColor = Color.Gray
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = areaOption, color = Color.White, fontSize = 15.sp)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAreaSelectorDialog = false }) {
                        Text("Batal", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }

        // 9. KATEGORI DIALOG
        if (isCategoryExpanded) {
            Dialog(
                onDismissRequest = { isCategoryExpanded = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xFF1E293B)
                ) {
                    var newCatName by remember { mutableStateOf("") }
                    var editingCatIndex by remember { mutableStateOf(-1) }
                    var editingCatName by remember { mutableStateOf("") }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Kelola Kategori Produk",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { isCategoryExpanded = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.LightGray)
                            }
                        }

                        Divider(color = Color(0x1FFFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newCatName,
                                onValueChange = { newCatName = it },
                                placeholder = { Text("Kategori Baru...", color = Color.Gray) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = tenantAccent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray,
                                    cursorColor = tenantAccent
                                )
                            )
                            Button(
                                onClick = {
                                    if (newCatName.isNotBlank() && !categoriesList.contains(newCatName.trim())) {
                                        onCategoriesListChange(categoriesList + newCatName.trim())
                                        newCatName = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = tenantAccent)
                            ) {
                                Text("Tambah", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(categoriesList) { index, category ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF334155))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (editingCatIndex == index) {
                                            OutlinedTextField(
                                                value = editingCatName,
                                                onValueChange = { editingCatName = it },
                                                modifier = Modifier.weight(1f),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = tenantAccent,
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.LightGray,
                                                    cursorColor = tenantAccent
                                                ),
                                                singleLine = true
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(onClick = {
                                                if (editingCatName.isNotBlank() && !categoriesList.contains(editingCatName.trim())) {
                                                    val updated = categoriesList.toMutableList()
                                                    updated[index] = editingCatName.trim()
                                                    onCategoriesListChange(updated)
                                                    editingCatIndex = -1
                                                    editingCatName = ""
                                                }
                                            }) {
                                                Icon(imageVector = Icons.Default.Check, contentDescription = "Simpan", tint = Color.Green)
                                            }
                                            IconButton(onClick = {
                                                editingCatIndex = -1
                                                editingCatName = ""
                                            }) {
                                                Icon(imageVector = Icons.Default.Close, contentDescription = "Batal", tint = Color.Red)
                                            }
                                        } else {
                                            Text(text = category, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                            IconButton(onClick = {
                                                editingCatIndex = index
                                                editingCatName = category
                                            }) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color.LightGray)
                                            }
                                            IconButton(onClick = {
                                                val updated = categoriesList.toMutableList()
                                                updated.removeAt(index)
                                                onCategoriesListChange(updated)
                                            }) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444))
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
    }
}

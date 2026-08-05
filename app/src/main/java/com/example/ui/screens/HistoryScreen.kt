package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SalesTransaction
import com.example.data.TenantConfig
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    tenant: TenantConfig,
    transactions: List<SalesTransaction>,
    onBackClick: () -> Unit = {},
    refreshData: () -> Unit = {}
) {
    val tenantAccent = Color(android.graphics.Color.parseColor(tenant.accentColor))
    val calendar = remember { Calendar.getInstance() }
    
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    val currentMonthIndex = calendar.get(Calendar.MONTH)
    val currentYearInt = calendar.get(Calendar.YEAR)

    var searchQuery by remember { mutableStateOf("") }
    var filterDay by remember { mutableStateOf(currentDay.toString()) }
    var filterMonth by remember { mutableStateOf((currentMonthIndex + 1).toString()) }
    var filterYear by remember { mutableStateOf(currentYearInt.toString()) }

    var expandedDay by remember { mutableStateOf(false) }
    var expandedMonth by remember { mutableStateOf(false) }
    var expandedYear by remember { mutableStateOf(false) }

    val monthsList = listOf(
        "Semua", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    val daysList = listOf("Semua") + (1..31).map { it.toString() }
    val yearsList = listOf("2024", "2025", "2026", "2027")

    fun formatRupiah(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return formatter.format(amount).replace(",00", "").replace("Rp", "Rp ")
    }

    val monthIndex = filterMonth.toIntOrNull() ?: (currentMonthIndex + 1)
    val filterMonthName = if (filterMonth == "Semua") "Semua" else monthsList.getOrElse(monthIndex) { "Agustus" }

    // Filter transactions based on date and search query
    val filteredTransactions = remember(transactions, searchQuery, filterDay, filterMonth, filterYear) {
        transactions.filter { tx ->
            var matches = true
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                val matchId = tx.id.lowercase().contains(q)
                val matchProd = tx.productName.lowercase().contains(q)
                val matchSku = tx.sku.lowercase().contains(q)
                val matchOp = tx.operator.lowercase().contains(q)
                if (!matchId && !matchProd && !matchSku && !matchOp) {
                    matches = false
                }
            }
            if (filterDay != "Semua") {
                val d = filterDay.padStart(2, '0')
                if (!tx.timestamp.contains("-$d ") && !tx.timestamp.contains("/$d/")) {
                    matches = false
                }
            }
            if (filterMonth != "Semua") {
                val m = filterMonth.padStart(2, '0')
                if (!tx.timestamp.contains("-$m-") && !tx.timestamp.contains("/$m/")) {
                    matches = false
                }
            }
            if (filterYear != "Semua") {
                if (!tx.timestamp.contains(filterYear)) {
                    matches = false
                }
            }
            matches
        }
    }

    val totalNominal = filteredTransactions.sumOf { it.totalPrice }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Riwayat Transaksi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${filteredTransactions.size} Transaksi Penjualan Terdaftar",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = refreshData) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Data",
                            tint = tenantAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E293B))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("history_search_bar"),
                placeholder = { Text("Cari Nota, Produk, SKU, Kasir...", color = Color.Gray, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tenantAccent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.LightGray
                ),
                textStyle = TextStyle(fontSize = 12.sp),
                singleLine = true
            )

            // DATE FILTER ROW
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = null,
                        tint = tenantAccent,
                        modifier = Modifier.size(16.dp)
                    )

                    // Dropdown Tgl
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { expandedDay = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text("Tgl: $filterDay", fontSize = 10.sp, color = Color.White)
                        }
                        DropdownMenu(
                            expanded = expandedDay,
                            onDismissRequest = { expandedDay = false }
                        ) {
                            daysList.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d) },
                                    onClick = {
                                        filterDay = d
                                        expandedDay = false
                                    }
                                )
                            }
                        }
                    }

                    // Dropdown Bulan
                    Box(modifier = Modifier.weight(1.3f)) {
                        OutlinedButton(
                            onClick = { expandedMonth = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(filterMonthName, fontSize = 10.sp, color = Color.White)
                        }
                        DropdownMenu(
                            expanded = expandedMonth,
                            onDismissRequest = { expandedMonth = false }
                        ) {
                            monthsList.forEachIndexed { index, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        filterMonth = if (name == "Semua") "Semua" else index.toString()
                                        expandedMonth = false
                                    }
                                )
                            }
                        }
                    }

                    // Dropdown Tahun
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { expandedYear = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text("Thn: $filterYear", fontSize = 10.sp, color = Color.White)
                        }
                        DropdownMenu(
                            expanded = expandedYear,
                            onDismissRequest = { expandedYear = false }
                        ) {
                            yearsList.forEach { y ->
                                DropdownMenuItem(
                                    text = { Text(y) },
                                    onClick = {
                                        filterYear = y
                                        expandedYear = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // SUMMARY INFO ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Nominal: ${formatRupiah(totalNominal)}",
                    color = tenantAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredTransactions.size} Transaksi Ditemukan",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            // TRANSACTIONS LIST
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
                        Text(
                            text = "Belum ada riwayat transaksi yang cocok.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        var isExpanded by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Header: Only Product Name and Total Price (with expand icon)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tx.productName,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = formatRupiah(tx.totalPrice),
                                            color = Color(0xFFF59E0B),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isExpanded) "Tutup Detail" else "Buka Detail",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Expanded details content
                                if (isExpanded) {
                                    Divider(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        color = Color(0x1AFFFFFF)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "No. Nota: ${tx.id}",
                                            fontWeight = FontWeight.Medium,
                                            color = Color.LightGray,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = tx.timestamp,
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${tx.quantity} pcs | SKU: ${tx.sku}",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Kasir: ${tx.operator}",
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x1AF59E0B))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "Lunas",
                                            color = Color(0xFFF59E0B),
                                            fontSize = 10.sp,
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
    }
}

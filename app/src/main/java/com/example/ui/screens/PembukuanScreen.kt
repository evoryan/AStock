package com.example.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.data.SalesTransaction
import com.example.data.TenantConfig
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class IncomeEntry(
    val id: String = System.currentTimeMillis().toString(),
    val amount: Double,
    val description: String,
    val timestamp: String
)

data class ExpenseEntry(
    val id: String = System.currentTimeMillis().toString(),
    val category: String, // "Belanja stok", "Gaji karyawan", "Operasional dan lainnya", "BON"
    val amount: Double,
    val description: String,
    val timestamp: String
)

private fun getCurrentTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date())
}

private fun loadIncomes(sharedPrefs: SharedPreferences): List<IncomeEntry> {
    val jsonString = sharedPrefs.getString("income_entries_json", null)
    if (jsonString == null) {
        val legacyVal = sharedPrefs.getFloat("other_income", 0f).toDouble()
        if (legacyVal > 0) {
            val initial = listOf(IncomeEntry(amount = legacyVal, description = "Saldo Awal Pemasukkan Lainnya", timestamp = getCurrentTimestamp()))
            saveIncomes(sharedPrefs, initial)
            return initial
        }
        return emptyList()
    }
    val list = mutableListOf<IncomeEntry>()
    try {
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                IncomeEntry(
                    id = obj.optString("id", System.currentTimeMillis().toString()),
                    amount = obj.optDouble("amount", 0.0),
                    description = obj.optString("description", ""),
                    timestamp = obj.optString("timestamp", "")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

private fun saveIncomes(sharedPrefs: SharedPreferences, list: List<IncomeEntry>) {
    val array = JSONArray()
    for (item in list) {
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("amount", item.amount)
        obj.put("description", item.description)
        obj.put("timestamp", item.timestamp)
        array.put(obj)
    }
    sharedPrefs.edit().putString("income_entries_json", array.toString()).apply()
}

private fun loadExpenses(sharedPrefs: SharedPreferences): List<ExpenseEntry> {
    val jsonString = sharedPrefs.getString("expense_entries_json", null)
    if (jsonString == null) {
        val legacyStock = sharedPrefs.getFloat("stock_expense", 0f).toDouble()
        val legacySalary = sharedPrefs.getFloat("salary_expense", 0f).toDouble()
        val legacyOp = sharedPrefs.getFloat("operational_expense", 0f).toDouble()
        val list = mutableListOf<ExpenseEntry>()
        if (legacyStock > 0) list.add(ExpenseEntry(category = "Belanja stok", amount = legacyStock, description = "Saldo Awal Belanja Stok", timestamp = getCurrentTimestamp()))
        if (legacySalary > 0) list.add(ExpenseEntry(category = "Gaji karyawan", amount = legacySalary, description = "Saldo Awal Gaji Karyawan", timestamp = getCurrentTimestamp()))
        if (legacyOp > 0) list.add(ExpenseEntry(category = "Operasional dan lainnya", amount = legacyOp, description = "Saldo Awal Operasional", timestamp = getCurrentTimestamp()))
        if (list.isNotEmpty()) saveExpenses(sharedPrefs, list)
        return list
    }
    val list = mutableListOf<ExpenseEntry>()
    try {
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ExpenseEntry(
                    id = obj.optString("id", System.currentTimeMillis().toString()),
                    category = obj.optString("category", "Belanja stok"),
                    amount = obj.optDouble("amount", 0.0),
                    description = obj.optString("description", ""),
                    timestamp = obj.optString("timestamp", "")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

private fun saveExpenses(sharedPrefs: SharedPreferences, list: List<ExpenseEntry>) {
    val array = JSONArray()
    for (item in list) {
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("category", item.category)
        obj.put("amount", item.amount)
        obj.put("description", item.description)
        obj.put("timestamp", item.timestamp)
        array.put(obj)
    }
    sharedPrefs.edit().putString("expense_entries_json", array.toString()).apply()
}

private fun matchesDateFilter(
    timestamp: String,
    selectedDay: String,
    selectedMonth: String,
    selectedYear: String
): Boolean {
    if (selectedDay == "Semua" && selectedMonth == "Semua" && selectedYear == "Semua") return true
    if (timestamp.isBlank()) return true

    val monthMap = mapOf(
        "Januari" to "01", "Februari" to "02", "Maret" to "03", "April" to "04",
        "Mei" to "05", "Juni" to "06", "Juli" to "07", "Agustus" to "08",
        "September" to "09", "Oktober" to "10", "November" to "11", "Desember" to "12"
    )

    val targetYear = if (selectedYear != "Semua") selectedYear else null
    val targetMonth = if (selectedMonth != "Semua") monthMap[selectedMonth] else null
    val targetDay = if (selectedDay != "Semua") selectedDay.padStart(2, '0') else null

    if (targetYear != null && !timestamp.contains(targetYear)) {
        return false
    }

    if (targetMonth != null) {
        val monthMatched = timestamp.contains("-$targetMonth-") ||
                timestamp.contains("/$targetMonth/") ||
                timestamp.contains("-$targetMonth ") ||
                timestamp.contains("/$targetMonth ")
        if (!monthMatched) return false
    }

    if (targetDay != null) {
        val dayMatched = timestamp.contains("-$targetDay") ||
                timestamp.contains("/$targetDay") ||
                timestamp.contains(" $targetDay ") ||
                timestamp.contains(" $targetDay:")
        if (!dayMatched) return false
    }

    return true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PembukuanScreen(
    tenant: TenantConfig,
    transactions: List<SalesTransaction>,
    products: List<Product>,
    onBackClick: () -> Unit = {},
    refreshData: () -> Unit = {}
) {
    val context = LocalContext.current
    val tenantAccent = Color(android.graphics.Color.parseColor(tenant.accentColor))
    val sharedPrefs = remember { context.getSharedPreferences("pembukuan_data_${tenant.dbName}", Context.MODE_PRIVATE) }

    // Persistent lists
    var incomeList by remember { mutableStateOf(loadIncomes(sharedPrefs)) }
    var expenseList by remember { mutableStateOf(loadExpenses(sharedPrefs)) }

    // Modal Awal (Initial Capital)
    var modalAwal by remember { mutableStateOf(sharedPrefs.getFloat("modal_awal", 0f).toDouble()) }

    // Date Filter State
    val daysList = remember { listOf("Semua") + (1..31).map { it.toString() } }
    val monthsList = remember {
        listOf(
            "Semua", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
    }
    val yearsList = remember { listOf("Semua", "2024", "2025", "2026", "2027", "2028") }

    var selectedDay by remember { mutableStateOf("Semua") }
    var selectedMonth by remember { mutableStateOf("Semua") }
    var selectedYear by remember { mutableStateOf("Semua") }

    var dayDropdownExpanded by remember { mutableStateOf(false) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }
    var yearDropdownExpanded by remember { mutableStateOf(false) }

    // Filtered data based on selected Date/Month/Year
    val filteredTransactions = remember(transactions, selectedDay, selectedMonth, selectedYear) {
        transactions.filter { tx ->
            matchesDateFilter(tx.timestamp, selectedDay, selectedMonth, selectedYear)
        }
    }

    val filteredIncomeList = remember(incomeList, selectedDay, selectedMonth, selectedYear) {
        incomeList.filter { inc ->
            matchesDateFilter(inc.timestamp, selectedDay, selectedMonth, selectedYear)
        }
    }

    val filteredExpenseList = remember(expenseList, selectedDay, selectedMonth, selectedYear) {
        expenseList.filter { exp ->
            matchesDateFilter(exp.timestamp, selectedDay, selectedMonth, selectedYear)
        }
    }

    // State for Pemasukkan (Income)
    var onlineIncome by remember { mutableStateOf(sharedPrefs.getFloat("online_income", 0f).toDouble()) }

    // Calculations based on filtered data
    val cashIncome = filteredTransactions.sumOf { it.totalPrice }
    val otherIncome = filteredIncomeList.sumOf { it.amount }
    val totalIncome = cashIncome + onlineIncome + otherIncome

    val stockExpense = filteredExpenseList.filter { it.category == "Belanja stok" }.sumOf { it.amount }
    val salaryExpense = filteredExpenseList.filter { it.category == "Gaji karyawan" }.sumOf { it.amount }
    val operationalExpense = filteredExpenseList.filter { it.category == "Operasional dan lainnya" }.sumOf { it.amount }
    val bonExpense = filteredExpenseList.filter { it.category == "BON" }.sumOf { it.amount }

    val totalExpense = stockExpense + salaryExpense + operationalExpense + bonExpense
    val netCash = totalIncome - totalExpense

    // Sisa Uang di Toko = Modal Awal + Cash Income + Other Income - Total Expense
    val sisaUangToko = modalAwal + cashIncome + otherIncome - totalExpense

    // Dialog States
    var showAddModalAwalDialog by remember { mutableStateOf(false) }
    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    var showIncomeListDialog by remember { mutableStateOf(false) }

    var showExpenseCategoryDialog by remember { mutableStateOf(false) }
    var selectedExpenseCategory by remember { mutableStateOf("Belanja stok") }

    // Add Modal Awal Form field
    var newModalAwalText by remember { mutableStateOf("") }

    // Add Income Form fields
    var newIncomeAmountText by remember { mutableStateOf("") }
    var newIncomeDescText by remember { mutableStateOf("") }

    // Add Expense Form fields
    var newExpenseAmountText by remember { mutableStateOf("") }
    var newExpenseCategory by remember { mutableStateOf("Belanja stok") }
    var newExpenseDescText by remember { mutableStateOf("") }
    var expenseCategoryDropdownExpanded by remember { mutableStateOf(false) }

    val expenseCategories = listOf("Belanja stok", "Gaji karyawan", "Operasional dan lainnya", "BON")

    fun formatRupiah(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return formatter.format(amount).replace(",00", "").replace("Rp", "Rp ")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pembukuan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E293B))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // FILTER SECTION: TANGGAL, BULAN, TAHUN (Di Paling Atas)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter Tanggal",
                            tint = tenantAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Filter Tanggal & Waktu",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Dropdown Tanggal
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { dayDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("Tgl", color = Color.Gray, fontSize = 9.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedDay,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = dayDropdownExpanded,
                                onDismissRequest = { dayDropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF1E293B)).heightIn(max = 260.dp)
                            ) {
                                daysList.forEach { day ->
                                    DropdownMenuItem(
                                        text = { Text(day, color = Color.White, fontSize = 13.sp) },
                                        onClick = {
                                            selectedDay = day
                                            dayDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Dropdown Bulan
                        Box(modifier = Modifier.weight(1.3f)) {
                            OutlinedButton(
                                onClick = { monthDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("Bulan", color = Color.Gray, fontSize = 9.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedMonth,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = monthDropdownExpanded,
                                onDismissRequest = { monthDropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF1E293B)).heightIn(max = 260.dp)
                            ) {
                                monthsList.forEach { month ->
                                    DropdownMenuItem(
                                        text = { Text(month, color = Color.White, fontSize = 13.sp) },
                                        onClick = {
                                            selectedMonth = month
                                            monthDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Dropdown Tahun
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { yearDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("Thn", color = Color.Gray, fontSize = 9.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedYear,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = yearDropdownExpanded,
                                onDismissRequest = { yearDropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF1E293B)).heightIn(max = 260.dp)
                            ) {
                                yearsList.forEach { yr ->
                                    DropdownMenuItem(
                                        text = { Text(yr, color = Color.White, fontSize = 13.sp) },
                                        onClick = {
                                            selectedYear = yr
                                            yearDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // MODAL AWAL CARD (Di bawah filter tanggal bulan tahun)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Modal Awal",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatRupiah(modalAwal),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = {
                            newModalAwalText = ""
                            showAddModalAwalDialog = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Modal Awal",
                            tint = tenantAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // CARD "Sisa Uang di toko"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2942)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sisa Uang di toko",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Modal Awal + Transaksi Cash/Lainnya - Total Pengeluaran",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = formatRupiah(sisaUangToko),
                        color = if (sisaUangToko >= 0) Color(0xFF38BDF8) else Color(0xFFEF4444),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // CARD 1: PEMASUKKAN
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pemasukkan",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatRupiah(totalIncome),
                                color = Color(0xFF10B981),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    newIncomeAmountText = ""
                                    newIncomeDescText = ""
                                    showAddIncomeDialog = true
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Tambah Pemasukkan",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0x33FFFFFF), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Inner Columns layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Transaksi Cash
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Transaksi Cash",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatRupiah(cashIncome),
                                    color = Color(0xFF10B981),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Transaksi Online
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Transaksi Online",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatRupiah(onlineIncome),
                                    color = Color(0xFF10B981),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Total Pemasukkan Lain2
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showIncomeListDialog = true
                            }
                    ) {
                        Text(
                            text = "Total Pemasukkan Lain2",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatRupiah(otherIncome),
                                color = Color(0xFF10B981),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Lihat List Pemasukkan",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // CARD 2: PENGELUARAN
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pengeluaran",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatRupiah(totalExpense),
                                color = Color(0xFFF87171),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    newExpenseAmountText = ""
                                    newExpenseCategory = "Belanja stok"
                                    newExpenseDescText = ""
                                    showAddExpenseDialog = true
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tambah Pengeluaran",
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0x33FFFFFF), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 1: Belanja Stok & Gaji Karyawan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Belanja Stok
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedExpenseCategory = "Belanja stok"
                                    showExpenseCategoryDialog = true
                                }
                        ) {
                            Text(
                                text = "Belanja Stok",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatRupiah(stockExpense),
                                    color = Color(0xFFF87171),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Gaji Karyawan
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedExpenseCategory = "Gaji karyawan"
                                    showExpenseCategoryDialog = true
                                }
                        ) {
                            Text(
                                text = "Gaji Karyawan",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatRupiah(salaryExpense),
                                    color = Color(0xFFF87171),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row 2: Operasional & Lainnya & BON
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Operasional & Lainnya
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedExpenseCategory = "Operasional dan lainnya"
                                    showExpenseCategoryDialog = true
                                }
                        ) {
                            Text(
                                text = "Operasional & Lainnya",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatRupiah(operationalExpense),
                                    color = Color(0xFFF87171),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // BON
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedExpenseCategory = "BON"
                                    showExpenseCategoryDialog = true
                                }
                        ) {
                            Text(
                                text = "BON",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatRupiah(bonExpense),
                                    color = Color(0xFFF87171),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // SUMMARY CARD: SISA KAS / NET PROFIT
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sisa Kas / Keuntungan Bersih",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (netCash >= 0) "Surplus Finansial" else "Defisit Finansial",
                            color = if (netCash >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = formatRupiah(netCash),
                        color = if (netCash >= 0) tenantAccent else Color(0xFFEF4444),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // DIALOG 0: SET MODAL AWAL (Otomatis masuk ke data pengeluaran "Belanja stok" / Keterangan "Modal Awal")
    if (showAddModalAwalDialog) {
        AlertDialog(
            onDismissRequest = { showAddModalAwalDialog = false },
            title = {
                Text(
                    text = "Tentukan Modal Awal",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Nominal modal awal ini akan otomatis dicatat sebagai data pengeluaran dengan keterangan 'Modal Awal':",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = newModalAwalText,
                        onValueChange = { newModalAwalText = it },
                        label = { Text("Nominal Modal Awal (Rp)", color = Color.Gray) },
                        placeholder = { Text("Contoh: 1000000", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = tenantAccent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = newModalAwalText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            // Update Modal Awal state
                            modalAwal = amount
                            sharedPrefs.edit().putFloat("modal_awal", amount.toFloat()).apply()

                            // Otomatis buat data pengeluaran dengan keterangan 'Modal Awal'
                            val modalExpense = ExpenseEntry(
                                category = "Belanja stok",
                                amount = amount,
                                description = "Modal Awal",
                                timestamp = getCurrentTimestamp()
                            )
                            val updatedExpenses = expenseList + modalExpense
                            expenseList = updatedExpenses
                            saveExpenses(sharedPrefs, updatedExpenses)
                        }
                        showAddModalAwalDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tenantAccent)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddModalAwalDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // DIALOG 1: TAMBAH PEMASUKAN (FORM: Nominal, Keterangan)
    if (showAddIncomeDialog) {
        AlertDialog(
            onDismissRequest = { showAddIncomeDialog = false },
            title = {
                Text(
                    text = "Tambah Data Pemasukkan",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Isi nominal dan keterangan pemasukkan lain-lain:",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = newIncomeAmountText,
                        onValueChange = { newIncomeAmountText = it },
                        label = { Text("Nominal (Rp)", color = Color.Gray) },
                        placeholder = { Text("Contoh: 100000", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = tenantAccent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newIncomeDescText,
                        onValueChange = { newIncomeDescText = it },
                        label = { Text("Keterangan", color = Color.Gray) },
                        placeholder = { Text("Contoh: Hasil penjualan kardus bekas", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = tenantAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = newIncomeAmountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            val newItem = IncomeEntry(
                                amount = amount,
                                description = if (newIncomeDescText.isBlank()) "Pemasukkan Lain-Lain" else newIncomeDescText,
                                timestamp = getCurrentTimestamp()
                            )
                            val updatedList = incomeList + newItem
                            incomeList = updatedList
                            saveIncomes(sharedPrefs, updatedList)
                        }
                        showAddIncomeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tenantAccent)
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddIncomeDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // DIALOG 2: TAMBAH PENGELUARAN (FORM: Nominal, Dropdown [Belanja stok, Gaji karyawan, Operasional dan lainnya, BON], Keterangan)
    if (showAddExpenseDialog) {
        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = {
                Text(
                    text = "Tambah Data Pengeluaran",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Isi nominal, kategori, dan keterangan pengeluaran:",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = newExpenseAmountText,
                        onValueChange = { newExpenseAmountText = it },
                        label = { Text("Nominal (Rp)", color = Color.Gray) },
                        placeholder = { Text("Contoh: 50000", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFF87171)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dropdown Kategori
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expenseCategoryDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kategori: $newExpenseCategory",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = expenseCategoryDropdownExpanded,
                            onDismissRequest = { expenseCategoryDropdownExpanded = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            expenseCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category, color = Color.White) },
                                    onClick = {
                                        newExpenseCategory = category
                                        expenseCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newExpenseDescText,
                        onValueChange = { newExpenseDescText = it },
                        label = { Text("Keterangan", color = Color.Gray) },
                        placeholder = { Text("Contoh: Beli plastik & tisu", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFF87171)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = newExpenseAmountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            val newItem = ExpenseEntry(
                                category = newExpenseCategory,
                                amount = amount,
                                description = if (newExpenseDescText.isBlank()) "Pengeluaran $newExpenseCategory" else newExpenseDescText,
                                timestamp = getCurrentTimestamp()
                            )
                            val updatedList = expenseList + newItem
                            expenseList = updatedList
                            saveExpenses(sharedPrefs, updatedList)
                        }
                        showAddExpenseDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Simpan", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // DIALOG 3: LIST PEMASUKAN LAIN2
    if (showIncomeListDialog) {
        AlertDialog(
            onDismissRequest = { showIncomeListDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Data Pemasukkan Lain2",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = formatRupiah(otherIncome),
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            text = {
                if (filteredIncomeList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada catatan pemasukkan lain-lain.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredIncomeList, key = { it.id }) { item ->
                            var isExpanded by remember { mutableStateOf(false) }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = !isExpanded },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatRupiah(item.amount),
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    if (isExpanded) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Divider(color = Color(0x1AFFFFFF))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Keterangan: ${item.description}",
                                            color = Color.LightGray,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Waktu: ${item.timestamp}",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val updated = incomeList.filter { it.id != item.id }
                                                    incomeList = updated
                                                    saveIncomes(sharedPrefs, updated)
                                                },
                                                modifier = Modifier.size(24.dp)
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
            },
            confirmButton = {
                Button(
                    onClick = { showIncomeListDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = tenantAccent)
                ) {
                    Text("Tutup", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // DIALOG 4: LIST PENGELUARAN BERDASARKAN KATEGORI
    if (showExpenseCategoryDialog) {
        val filteredCategoryExpenses = remember(filteredExpenseList, selectedExpenseCategory) {
            filteredExpenseList.filter { it.category == selectedExpenseCategory }
        }
        val categoryTotal = filteredCategoryExpenses.sumOf { it.amount }

        AlertDialog(
            onDismissRequest = { showExpenseCategoryDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detail: $selectedExpenseCategory",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = formatRupiah(categoryTotal),
                        color = Color(0xFFF87171),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            text = {
                if (filteredCategoryExpenses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada data pengeluaran untuk $selectedExpenseCategory.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCategoryExpenses, key = { it.id }) { item ->
                            var isExpanded by remember { mutableStateOf(false) }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = !isExpanded },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatRupiah(item.amount),
                                            color = Color(0xFFF87171),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    if (isExpanded) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Divider(color = Color(0x1AFFFFFF))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Keterangan: ${item.description}",
                                            color = Color.LightGray,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Waktu: ${item.timestamp}",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val updated = expenseList.filter { it.id != item.id }
                                                    expenseList = updated
                                                    saveExpenses(sharedPrefs, updated)
                                                },
                                                modifier = Modifier.size(24.dp)
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
            },
            confirmButton = {
                Button(
                    onClick = { showExpenseCategoryDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Tutup", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// -----------------------------------------------------------------
// 1. ROOM ENTITIES (Isolated by tenantDbName)
// -----------------------------------------------------------------

@Entity(tableName = "local_products", primaryKeys = ["tenantDbName", "id"])
data class LocalProductEntity(
    val tenantDbName: String,
    val id: String,
    val name: String,
    val sku: String,
    val category: String,
    val price: Double,
    val stock: Int,
    val minStockAlert: Int = 5
)

@Entity(tableName = "local_transactions", primaryKeys = ["tenantDbName", "id"])
data class LocalTransactionEntity(
    val tenantDbName: String,
    val id: String,
    val productName: String,
    val sku: String,
    val quantity: Int,
    val totalPrice: Double,
    val timestamp: String,
    val operator: String
)

@Entity(tableName = "local_incomes", primaryKeys = ["tenantDbName", "id"])
data class LocalIncomeEntity(
    val tenantDbName: String,
    val id: String,
    val amount: Double,
    val description: String,
    val timestamp: String
)

@Entity(tableName = "local_expenses", primaryKeys = ["tenantDbName", "id"])
data class LocalExpenseEntity(
    val tenantDbName: String,
    val id: String,
    val category: String,
    val amount: Double,
    val description: String,
    val timestamp: String
)

@Entity(tableName = "local_admins", primaryKeys = ["tenantDbName", "id"])
data class LocalAdminEntity(
    val tenantDbName: String,
    val id: String,
    val name: String,
    val username: String,
    val role: String,
    val area: String
)

@Entity(tableName = "local_areas", primaryKeys = ["tenantDbName", "id"])
data class LocalAreaEntity(
    val tenantDbName: String,
    val id: String,
    val name: String
)

@Entity(tableName = "local_categories", primaryKeys = ["tenantDbName", "categoryName"])
data class LocalCategoryEntity(
    val tenantDbName: String,
    val categoryName: String
)

// -----------------------------------------------------------------
// 2. ROOM DAO (Data Access Object)
// -----------------------------------------------------------------

@Dao
interface TenantDataDao {
    // Products
    @Query("SELECT * FROM local_products WHERE tenantDbName = :tenantDbName ORDER BY name ASC")
    fun getProductsByTenant(tenantDbName: String): Flow<List<LocalProductEntity>>

    @Query("SELECT * FROM local_products WHERE tenantDbName = :tenantDbName ORDER BY name ASC")
    suspend fun getProductsListByTenant(tenantDbName: String): List<LocalProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<LocalProductEntity>)

    @Query("DELETE FROM local_products WHERE tenantDbName = :tenantDbName AND id = :id")
    suspend fun deleteProduct(tenantDbName: String, id: String)

    @Query("DELETE FROM local_products WHERE tenantDbName = :tenantDbName")
    suspend fun clearProductsByTenant(tenantDbName: String)

    // Transactions
    @Query("SELECT * FROM local_transactions WHERE tenantDbName = :tenantDbName ORDER BY timestamp DESC")
    fun getTransactionsByTenant(tenantDbName: String): Flow<List<LocalTransactionEntity>>

    @Query("SELECT * FROM local_transactions WHERE tenantDbName = :tenantDbName ORDER BY timestamp DESC")
    suspend fun getTransactionsListByTenant(tenantDbName: String): List<LocalTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<LocalTransactionEntity>)

    @Query("DELETE FROM local_transactions WHERE tenantDbName = :tenantDbName")
    suspend fun clearTransactionsByTenant(tenantDbName: String)

    // Incomes
    @Query("SELECT * FROM local_incomes WHERE tenantDbName = :tenantDbName ORDER BY timestamp DESC")
    suspend fun getIncomesByTenant(tenantDbName: String): List<LocalIncomeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: LocalIncomeEntity)

    @Query("DELETE FROM local_incomes WHERE tenantDbName = :tenantDbName AND id = :id")
    suspend fun deleteIncome(tenantDbName: String, id: String)

    // Expenses
    @Query("SELECT * FROM local_expenses WHERE tenantDbName = :tenantDbName ORDER BY timestamp DESC")
    suspend fun getExpensesByTenant(tenantDbName: String): List<LocalExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: LocalExpenseEntity)

    @Query("DELETE FROM local_expenses WHERE tenantDbName = :tenantDbName AND id = :id")
    suspend fun deleteExpense(tenantDbName: String, id: String)

    // Admins
    @Query("SELECT * FROM local_admins WHERE tenantDbName = :tenantDbName")
    suspend fun getAdminsByTenant(tenantDbName: String): List<LocalAdminEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmins(admins: List<LocalAdminEntity>)

    @Query("DELETE FROM local_admins WHERE tenantDbName = :tenantDbName AND id = :id")
    suspend fun deleteAdmin(tenantDbName: String, id: String)

    // Areas
    @Query("SELECT * FROM local_areas WHERE tenantDbName = :tenantDbName")
    suspend fun getAreasByTenant(tenantDbName: String): List<LocalAreaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAreas(areas: List<LocalAreaEntity>)

    @Query("DELETE FROM local_areas WHERE tenantDbName = :tenantDbName AND id = :id")
    suspend fun deleteArea(tenantDbName: String, id: String)

    // Categories
    @Query("SELECT categoryName FROM local_categories WHERE tenantDbName = :tenantDbName")
    suspend fun getCategoriesByTenant(tenantDbName: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<LocalCategoryEntity>)
}

// -----------------------------------------------------------------
// 3. ROOM DATABASE CLASS
// -----------------------------------------------------------------

@Database(
    entities = [
        LocalProductEntity::class,
        LocalTransactionEntity::class,
        LocalIncomeEntity::class,
        LocalExpenseEntity::class,
        LocalAdminEntity::class,
        LocalAreaEntity::class,
        LocalCategoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppRoomDatabase : RoomDatabase() {
    abstract fun tenantDataDao(): TenantDataDao

    companion object {
        @Volatile
        private var INSTANCE: AppRoomDatabase? = null

        fun getDatabase(context: Context): AppRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppRoomDatabase::class.java,
                    "astock_local_tenant.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

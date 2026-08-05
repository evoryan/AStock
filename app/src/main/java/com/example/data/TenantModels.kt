package com.example.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class TenantConfig(
    val id: String,
    val name: String,
    val ownerName: String,
    val dbName: String,
    val accentColor: String, // Hex color for the tenant's visual branding
    val businessType: String,
    val initialProducts: List<Product>
)

data class Product(
    val id: String,
    val name: String,
    val sku: String,
    val category: String,
    val price: Double,
    val stock: Int,
    val minStockAlert: Int = 5
)

data class SalesTransaction(
    val id: String,
    val productName: String,
    val sku: String,
    val quantity: Int,
    val totalPrice: Double,
    val timestamp: String,
    val operator: String
)



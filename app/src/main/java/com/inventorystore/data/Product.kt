package com.inventorystore.data

data class Product(
    val id: Int = 0,
    val name: String,
    val description: String,
    val price: Double,
    val stock: Int,
    val category: String,
    val imageUrl: String? = null,
    val lowStockThreshold: Int = 5
) {
    fun isLowStock(): Boolean = stock <= lowStockThreshold
}
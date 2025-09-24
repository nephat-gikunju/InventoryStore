package com.inventorystore.data

import java.util.Date

data class Sale(
    val id: Int = 0,
    val customerName: String,
    val items: List<SaleItem>,
    val total: Double,
    val date: Date = Date()
)

data class SaleItem(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val price: Double
) {
    fun getSubtotal(): Double = quantity * price
}
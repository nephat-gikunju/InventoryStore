package com.inventorystore.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "inventory_store.db"
        private const val DATABASE_VERSION = 1

        // Products table
        private const val TABLE_PRODUCTS = "products"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_PRICE = "price"
        private const val COLUMN_STOCK = "stock"
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_IMAGE_URL = "image_url"
        private const val COLUMN_LOW_STOCK_THRESHOLD = "low_stock_threshold"

        // Sales table
        private const val TABLE_SALES = "sales"
        private const val COLUMN_CUSTOMER_NAME = "customer_name"
        private const val COLUMN_TOTAL = "total"
        private const val COLUMN_DATE = "date"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createProductsTable = """
            CREATE TABLE $TABLE_PRODUCTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_PRICE REAL NOT NULL,
                $COLUMN_STOCK INTEGER NOT NULL,
                $COLUMN_CATEGORY TEXT NOT NULL,
                $COLUMN_IMAGE_URL TEXT,
                $COLUMN_LOW_STOCK_THRESHOLD INTEGER DEFAULT 5
            )
        """.trimIndent()

        val createSalesTable = """
            CREATE TABLE $TABLE_SALES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CUSTOMER_NAME TEXT NOT NULL,
                $COLUMN_TOTAL REAL NOT NULL,
                $COLUMN_DATE TEXT NOT NULL
            )
        """.trimIndent()

        db.execSQL(createProductsTable)
        db.execSQL(createSalesTable)

        // Insert sample data
        insertSampleData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SALES")
        onCreate(db)
    }

    private fun insertSampleData(db: SQLiteDatabase) {
        val products = listOf(
            Product(name = "Ear Buds", description = "Enjoy your music with uninterrupted and good quality music", price = 100.0, stock = 6, category = "Electronics"),
            Product(name = "Head Phones", description = "A good sound quality head phones", price = 40.0, stock = 0, category = "Electronics"),
            Product(name = "Humidifier", description = "Keep your air fresh and clean", price = 20.0, stock = 15, category = "Accessory"),
            Product(name = "Mouse", description = "Gaming mouse with RGB lighting", price = 45.0, stock = 8, category = "Electronics"),
            Product(name = "Power Bank", description = "A power bank to keep you online even in trips your number one choice", price = 100.0, stock = 12, category = "Accessory")
        )

        products.forEach { product ->
            val values = ContentValues().apply {
                put(COLUMN_NAME, product.name)
                put(COLUMN_DESCRIPTION, product.description)
                put(COLUMN_PRICE, product.price)
                put(COLUMN_STOCK, product.stock)
                put(COLUMN_CATEGORY, product.category)
                put(COLUMN_LOW_STOCK_THRESHOLD, product.lowStockThreshold)
            }
            db.insert(TABLE_PRODUCTS, null, values)
        }
    }

    fun getAllProducts(): List<Product> {
        val products = mutableListOf<Product>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PRODUCTS", null)

        cursor.use {
            while (it.moveToNext()) {
                products.add(
                    Product(
                        id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                        description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                        price = it.getDouble(it.getColumnIndexOrThrow(COLUMN_PRICE)),
                        stock = it.getInt(it.getColumnIndexOrThrow(COLUMN_STOCK)),
                        category = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                        imageUrl = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE_URL)),
                        lowStockThreshold = it.getInt(it.getColumnIndexOrThrow(COLUMN_LOW_STOCK_THRESHOLD))
                    )
                )
            }
        }
        return products
    }

    fun updateProductStock(productId: Int, newStock: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_STOCK, newStock)
        }
        val result = db.update(TABLE_PRODUCTS, values, "$COLUMN_ID = ?", arrayOf(productId.toString()))
        return result > 0
    }

    fun addSale(sale: Sale): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CUSTOMER_NAME, sale.customerName)
            put(COLUMN_TOTAL, sale.total)
            put(COLUMN_DATE, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(sale.date))
        }
        return db.insert(TABLE_SALES, null, values)
    }

    fun getTotalRevenue(): Double {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT SUM($COLUMN_TOTAL) FROM $TABLE_SALES", null)
        var total = 0.0
        cursor.use {
            if (it.moveToFirst()) {
                total = it.getDouble(0)
            }
        }
        return total
    }

    fun getLowStockCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_PRODUCTS WHERE $COLUMN_STOCK <= $COLUMN_LOW_STOCK_THRESHOLD", null)
        var count = 0
        cursor.use {
            if (it.moveToFirst()) {
                count = it.getInt(0)
            }
        }
        return count
    }
}
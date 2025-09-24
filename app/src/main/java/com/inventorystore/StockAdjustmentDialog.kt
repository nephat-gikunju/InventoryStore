package com.inventorystore.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.inventorystore.R
import com.inventorystore.data.DatabaseHelper
import com.inventorystore.data.Product
import com.inventorystore.utils.CurrencyFormatter

class StockAdjustmentDialog(
    context: Context,
    private val product: Product,
    private val onStockUpdated: (Product) -> Unit
) : Dialog(context) {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etCurrentStock: TextInputEditText
    private lateinit var etAdjustmentAmount: TextInputEditText
    private lateinit var btnAdd: MaterialButton
    private lateinit var btnSubtract: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_stock_adjustment)

        dbHelper = DatabaseHelper(context)

        initViews()
        setupData()
        setupClickListeners()

        // Make dialog non-cancelable by touching outside
        setCanceledOnTouchOutside(false)
    }

    private fun initViews() {
        etCurrentStock = findViewById(R.id.et_current_stock)
        etAdjustmentAmount = findViewById(R.id.et_adjustment_amount)
        btnAdd = findViewById(R.id.btn_add)
        btnSubtract = findViewById(R.id.btn_subtract)
        btnSave = findViewById(R.id.btn_save)
        btnCancel = findViewById(R.id.btn_cancel)
    }

    private fun setupData() {
        etCurrentStock.setText(product.stock.toString())
        etCurrentStock.isEnabled = false // Read-only

        // Set dialog title
        findViewById<android.widget.TextView>(R.id.tv_dialog_title).text = "Adjust Stock - ${product.name}"
        findViewById<android.widget.TextView>(R.id.tv_product_price).text = CurrencyFormatter.format(product.price)
    }

    private fun setupClickListeners() {
        btnAdd.setOnClickListener {
            adjustStock(positive = true)
        }

        btnSubtract.setOnClickListener {
            adjustStock(positive = false)
        }

        btnSave.setOnClickListener {
            saveStockAdjustment()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun adjustStock(positive: Boolean) {
        val adjustmentText = etAdjustmentAmount.text.toString()
        if (adjustmentText.isEmpty()) {
            etAdjustmentAmount.error = "Enter adjustment amount"
            return
        }

        val adjustment = adjustmentText.toIntOrNull()
        if (adjustment == null || adjustment <= 0) {
            etAdjustmentAmount.error = "Enter valid positive number"
            return
        }

        val currentStock = product.stock
        val newStock = if (positive) {
            currentStock + adjustment
        } else {
            maxOf(0, currentStock - adjustment) // Don't allow negative stock
        }

        etCurrentStock.setText(newStock.toString())
        etAdjustmentAmount.setText("")
        etAdjustmentAmount.error = null
    }

    private fun saveStockAdjustment() {
        val newStockText = etCurrentStock.text.toString()
        val newStock = newStockText.toIntOrNull()

        if (newStock == null || newStock < 0) {
            Toast.makeText(context, "Invalid stock quantity", Toast.LENGTH_SHORT).show()
            return
        }

        if (dbHelper.updateProductStock(product.id, newStock)) {
            val updatedProduct = product.copy(stock = newStock)
            onStockUpdated(updatedProduct)
            Toast.makeText(context, "Stock updated successfully", Toast.LENGTH_SHORT).show()
            dismiss()
        } else {
            Toast.makeText(context, "Failed to update stock", Toast.LENGTH_SHORT).show()
        }
    }
}
package com.inventorystore

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.inventorystore.data.DatabaseHelper
import com.inventorystore.data.Product

class AddEditProductActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRODUCT_ID = "product_id"
        const val EXTRA_IS_EDIT_MODE = "is_edit_mode"
    }

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etProductName: TextInputEditText
    private lateinit var etProductDescription: TextInputEditText
    private lateinit var etProductPrice: TextInputEditText
    private lateinit var etProductStock: TextInputEditText
    private lateinit var etProductCategory: TextInputEditText
    private lateinit var etLowStockThreshold: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnDelete: MaterialButton

    private var isEditMode = false
    private var productId = -1
    private var currentProduct: Product? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_product)

        dbHelper = DatabaseHelper(this)

        // Get intent data
        isEditMode = intent.getBooleanExtra(EXTRA_IS_EDIT_MODE, false)
        productId = intent.getIntExtra(EXTRA_PRODUCT_ID, -1)

        initViews()
        setupToolbar()
        setupClickListeners()

        if (isEditMode && productId != -1) {
            loadProductData()
        }
    }

    private fun initViews() {
        etProductName = findViewById(R.id.et_product_name)
        etProductDescription = findViewById(R.id.et_product_description)
        etProductPrice = findViewById(R.id.et_product_price)
        etProductStock = findViewById(R.id.et_product_stock)
        etProductCategory = findViewById(R.id.et_product_category)
        etLowStockThreshold = findViewById(R.id.et_low_stock_threshold)
        btnSave = findViewById(R.id.btn_save)
        btnCancel = findViewById(R.id.btn_cancel)
        btnDelete = findViewById(R.id.btn_delete)
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = if (isEditMode) "Edit Product" else "Add Product"
        }

        // Show delete button only in edit mode
        btnDelete.visibility = if (isEditMode) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun setupClickListeners() {
        btnSave.setOnClickListener { saveProduct() }
        btnCancel.setOnClickListener { finish() }
        btnDelete.setOnClickListener { deleteProduct() }
    }

    private fun loadProductData() {
        currentProduct = dbHelper.getProductById(productId)
        currentProduct?.let { product ->
            etProductName.setText(product.name)
            etProductDescription.setText(product.description)
            etProductPrice.setText(product.price.toString())
            etProductStock.setText(product.stock.toString())
            etProductCategory.setText(product.category)
            etLowStockThreshold.setText(product.lowStockThreshold.toString())
        }
    }

    private fun saveProduct() {
        if (!validateInput()) return

        val name = etProductName.text.toString().trim()
        val description = etProductDescription.text.toString().trim()
        val price = etProductPrice.text.toString().toDoubleOrNull() ?: 0.0
        val stock = etProductStock.text.toString().toIntOrNull() ?: 0
        val category = etProductCategory.text.toString().trim()
        val threshold = etLowStockThreshold.text.toString().toIntOrNull() ?: 5

        val product = if (isEditMode) {
            Product(
                id = productId,
                name = name,
                description = description,
                price = price,
                stock = stock,
                category = category,
                lowStockThreshold = threshold
            )
        } else {
            Product(
                name = name,
                description = description,
                price = price,
                stock = stock,
                category = category,
                lowStockThreshold = threshold
            )
        }

        val success = if (isEditMode) {
            dbHelper.updateProduct(product)
        } else {
            dbHelper.addProduct(product) > 0
        }

        if (success) {
            Toast.makeText(this, if (isEditMode) "Product updated successfully" else "Product added successfully", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } else {
            Toast.makeText(this, "Failed to save product", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateInput(): Boolean {
        var isValid = true

        if (etProductName.text.toString().trim().isEmpty()) {
            etProductName.error = "Product name is required"
            isValid = false
        }

        val priceText = etProductPrice.text.toString().trim()
        if (priceText.isEmpty() || priceText.toDoubleOrNull() == null || priceText.toDouble() < 0) {
            etProductPrice.error = "Valid price is required"
            isValid = false
        }

        val stockText = etProductStock.text.toString().trim()
        if (stockText.isEmpty() || stockText.toIntOrNull() == null || stockText.toInt() < 0) {
            etProductStock.error = "Valid stock quantity is required"
            isValid = false
        }

        if (etProductCategory.text.toString().trim().isEmpty()) {
            etProductCategory.error = "Category is required"
            isValid = false
        }

        return isValid
    }

    private fun deleteProduct() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete this product? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                if (dbHelper.deleteProduct(productId)) {
                    Toast.makeText(this, "Product deleted successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to delete product", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
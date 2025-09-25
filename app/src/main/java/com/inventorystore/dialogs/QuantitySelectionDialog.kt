
package com.inventorystore.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.inventorystore.R
import com.inventorystore.data.Product
import com.inventorystore.utils.CurrencyFormatter

class QuantitySelectionDialog(
    context: Context,
    private val product: Product,
    private val maxQuantity: Int,
    private val currentCartQuantity: Int = 0,
    private val isEditing: Boolean = false,
    private val currentQuantity: Int = 1,
    private val onQuantitySelected: (Int) -> Unit
) : Dialog(context) {

    private lateinit var tvProductName: TextView
    private lateinit var tvProductPrice: TextView
    private lateinit var tvAvailableStock: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var numberPicker: NumberPicker
    private lateinit var etCustomQuantity: TextInputEditText
    private lateinit var btnQuick1: MaterialButton
    private lateinit var btnQuick5: MaterialButton
    private lateinit var btnQuick10: MaterialButton
    private lateinit var btnQuickAll: MaterialButton
    private lateinit var btnAdd: MaterialButton
    private lateinit var btnCancel: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure window for mobile optimization
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_quantity_selection)

        // Set dialog window properties for mobile
        window?.let { window ->
            window.setLayout(
                (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }

        initViews()
        setupData()
        setupClickListeners()
        updateSubtotal()

        setCanceledOnTouchOutside(true)
    }

    private fun initViews() {
        tvProductName = findViewById(R.id.tv_product_name)
        tvProductPrice = findViewById(R.id.tv_product_price)
        tvAvailableStock = findViewById(R.id.tv_available_stock)
        tvSubtotal = findViewById(R.id.tv_subtotal)
        numberPicker = findViewById(R.id.number_picker)
        etCustomQuantity = findViewById(R.id.et_custom_quantity)
        btnQuick1 = findViewById(R.id.btn_quick_1)
        btnQuick5 = findViewById(R.id.btn_quick_5)
        btnQuick10 = findViewById(R.id.btn_quick_10)
        btnQuickAll = findViewById(R.id.btn_quick_all)
        btnAdd = findViewById(R.id.btn_add)
        btnCancel = findViewById(R.id.btn_cancel)
    }

    private fun setupData() {
        tvProductName.text = product.name
        tvProductPrice.text = CurrencyFormatter.format(product.price)

        val availableForCart = maxQuantity - currentCartQuantity
        tvAvailableStock.text = if (isEditing) {
            "Available: ${maxQuantity}"
        } else {
            "Available: $availableForCart${if (currentCartQuantity > 0) " (${currentCartQuantity} in cart)" else ""}"
        }

        val maxAllowed = if (isEditing) maxQuantity else availableForCart

        // Setup NumberPicker with mobile-friendly configuration
        numberPicker.minValue = if (isEditing) 0 else 1
        numberPicker.maxValue = kotlin.math.max(1, maxAllowed)
        numberPicker.value = if (isEditing) currentQuantity else 1
        numberPicker.wrapSelectorWheel = false
        numberPicker.setOnValueChangedListener { _, _, newVal ->
            updateSubtotal()
            etCustomQuantity.setText(newVal.toString())
        }

        // Setup custom quantity input
        etCustomQuantity.setText(numberPicker.value.toString())

        // Setup quick buttons with smart availability
        btnQuick1.isEnabled = maxAllowed >= 1
        btnQuick5.isEnabled = maxAllowed >= 5
        btnQuick10.isEnabled = maxAllowed >= 10

        // Smart "All" button text
        btnQuickAll.text = if (maxAllowed <= 99) "All ($maxAllowed)" else "All"
        btnQuickAll.isEnabled = maxAllowed > 1

        // Update button text for editing mode
        btnAdd.text = if (isEditing) "Update" else "Add to Cart"
    }

    private fun setupClickListeners() {
        btnQuick1.setOnClickListener { setQuantity(1) }
        btnQuick5.setOnClickListener { setQuantity(5) }
        btnQuick10.setOnClickListener { setQuantity(10) }
        btnQuickAll.setOnClickListener {
            val maxAllowed = if (isEditing) maxQuantity else (maxQuantity - currentCartQuantity)
            setQuantity(maxAllowed)
        }

        btnAdd.setOnClickListener { confirmQuantity() }
        btnCancel.setOnClickListener { dismiss() }

        // Handle custom quantity input
        etCustomQuantity.setOnEditorActionListener { _, _, _ ->
            val customQuantity = etCustomQuantity.text.toString().toIntOrNull()
            if (customQuantity != null) {
                setQuantity(customQuantity)
            }
            true
        }
    }

    private fun setQuantity(quantity: Int) {
        val maxAllowed = if (isEditing) maxQuantity else (maxQuantity - currentCartQuantity)
        val validQuantity = kotlin.math.min(kotlin.math.max(if (isEditing) 0 else 1, quantity), maxAllowed)

        if (validQuantity != quantity && quantity > maxAllowed) {
            Toast.makeText(context, "Maximum available: $maxAllowed", Toast.LENGTH_SHORT).show()
        }

        numberPicker.value = validQuantity
        etCustomQuantity.setText(validQuantity.toString())
        updateSubtotal()
    }

    private fun updateSubtotal() {
        val quantity = numberPicker.value
        val subtotal = product.price * quantity
        tvSubtotal.text = "Subtotal: ${CurrencyFormatter.format(subtotal)}"
    }

    private fun confirmQuantity() {
        val quantity = numberPicker.value
        val maxAllowed = if (isEditing) maxQuantity else (maxQuantity - currentCartQuantity)

        if (quantity < (if (isEditing) 0 else 1) || quantity > maxAllowed) {
            Toast.makeText(context, "Please select a valid quantity (1-$maxAllowed)", Toast.LENGTH_SHORT).show()
            return
        }

        onQuantitySelected(quantity)
        dismiss()
    }
}
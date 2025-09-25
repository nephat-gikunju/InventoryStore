package com.inventorystore.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.inventorystore.R
import com.inventorystore.adapters.ProductAdapter
import com.inventorystore.data.DatabaseHelper
import com.inventorystore.data.Product
import com.inventorystore.data.Sale
import com.inventorystore.data.SaleItem
import com.inventorystore.dialogs.QuantitySelectionDialog
import com.inventorystore.utils.CurrencyFormatter

class POSFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var rvAvailableProducts: RecyclerView
    private lateinit var rvCartItems: RecyclerView
    private lateinit var tvTotal: TextView
    private lateinit var tvCartItemCount: TextView
    private lateinit var layoutEmptyCart: View
    private lateinit var etCustomerName: TextInputEditText
    private lateinit var btnCheckout: Button
    private lateinit var btnClearCart: Button

    private lateinit var availableProductsAdapter: ProductAdapter
    private lateinit var cartAdapter: ProductAdapter
    private val cartItems = mutableListOf<CartItem>()

    data class CartItem(val product: Product, var quantity: Int) {
        fun getSubtotal(): Double = product.price * quantity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_pos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())

        initViews(view)
        setupRecyclerViews()
        setupClickListeners()
        loadAvailableProducts()
        updateCartDisplay()
    }

    private fun initViews(view: View) {
        rvAvailableProducts = view.findViewById(R.id.rv_available_products)
        rvCartItems = view.findViewById(R.id.rv_cart_items)
        tvTotal = view.findViewById(R.id.tv_total)
        tvCartItemCount = view.findViewById(R.id.tv_cart_item_count)
        layoutEmptyCart = view.findViewById(R.id.layout_empty_cart)
        etCustomerName = view.findViewById(R.id.et_customer_name)
        btnCheckout = view.findViewById(R.id.btn_checkout)
        btnClearCart = view.findViewById(R.id.btn_clear_cart)
    }

    private fun setupRecyclerViews() {
        // Available Products Adapter - Use Grid for better product visibility
        availableProductsAdapter = ProductAdapter(
            products = emptyList(),
            onProductClick = { product ->
                showQuantitySelectionDialog(product)
            }
        )

        // Cart Items Adapter - for managing cart
        cartAdapter = ProductAdapter(
            products = emptyList(),
            onProductClick = { product ->
                showCartItemOptions(product)
            }
        )

        rvAvailableProducts.apply {
            layoutManager = GridLayoutManager(requireContext(), 2) // Grid layout for better visibility
            adapter = availableProductsAdapter
        }

        rvCartItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }
    }

    private fun setupClickListeners() {
        btnCheckout.setOnClickListener { processSale() }
        btnClearCart.setOnClickListener { showClearCartConfirmation() }
    }

    private fun loadAvailableProducts() {
        // Load ALL products with stock > 0
        val products = dbHelper.getAllProducts().filter { it.stock > 0 }
        availableProductsAdapter.updateProducts(products)

        if (products.isEmpty()) {
            Toast.makeText(requireContext(), "No products available for sale", Toast.LENGTH_LONG).show()
        }
    }

    private fun showQuantitySelectionDialog(product: Product) {
        val dialog = QuantitySelectionDialog(
            context = requireContext(),
            product = product,
            maxQuantity = product.stock,
            currentCartQuantity = getCurrentCartQuantity(product)
        ) { quantity ->
            addToCartWithQuantity(product, quantity)
        }
        dialog.show()
    }

    private fun getCurrentCartQuantity(product: Product): Int {
        return cartItems.find { it.product.id == product.id }?.quantity ?: 0
    }

    private fun addToCartWithQuantity(product: Product, quantity: Int) {
        if (quantity <= 0) return

        val availableStock = product.stock - getCurrentCartQuantity(product)
        if (quantity > availableStock) {
            Toast.makeText(requireContext(), "Not enough stock available", Toast.LENGTH_SHORT).show()
            return
        }

        val existingItem = cartItems.find { it.product.id == product.id }
        if (existingItem != null) {
            existingItem.quantity += quantity
        } else {
            cartItems.add(CartItem(product, quantity))
        }

        Toast.makeText(
            requireContext(),
            "Added $quantity x ${product.name} to cart",
            Toast.LENGTH_SHORT
        ).show()
        updateCartDisplay()
    }

    private fun showCartItemOptions(product: Product) {
        val cartItem = cartItems.find { it.product.id == product.id } ?: return

        val options = arrayOf(
            "Edit quantity",
            "Add more",
            "Remove 1 item",
            "Remove all (${cartItem.quantity} items)"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Manage ${product.name}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editCartItemQuantity(cartItem)
                    1 -> showQuantitySelectionDialog(product)
                    2 -> removeOneFromCart(cartItem)
                    3 -> removeAllFromCart(cartItem)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editCartItemQuantity(cartItem: CartItem) {
        val dialog = QuantitySelectionDialog(
            context = requireContext(),
            product = cartItem.product,
            maxQuantity = cartItem.product.stock,
            currentCartQuantity = 0, // Don't limit by current cart
            isEditing = true,
            currentQuantity = cartItem.quantity
        ) { newQuantity ->
            if (newQuantity <= 0) {
                removeAllFromCart(cartItem)
            } else {
                cartItem.quantity = newQuantity
                Toast.makeText(
                    requireContext(),
                    "Updated ${cartItem.product.name} quantity to $newQuantity",
                    Toast.LENGTH_SHORT
                ).show()
                updateCartDisplay()
            }
        }
        dialog.show()
    }

    private fun removeOneFromCart(cartItem: CartItem) {
        if (cartItem.quantity > 1) {
            cartItem.quantity--
            Toast.makeText(requireContext(), "Removed 1 ${cartItem.product.name}", Toast.LENGTH_SHORT).show()
        } else {
            cartItems.remove(cartItem)
            Toast.makeText(requireContext(), "Removed ${cartItem.product.name} from cart", Toast.LENGTH_SHORT).show()
        }
        updateCartDisplay()
    }

    private fun removeAllFromCart(cartItem: CartItem) {
        cartItems.remove(cartItem)
        Toast.makeText(requireContext(), "Removed all ${cartItem.product.name} from cart", Toast.LENGTH_SHORT).show()
        updateCartDisplay()
    }

    private fun updateCartDisplay() {
        // Update cart items list with quantity and subtotal info
        val cartProducts = cartItems.map { cartItem ->
            cartItem.product.copy(
                name = "${cartItem.product.name} × ${cartItem.quantity}",
                description = "Unit: ${CurrencyFormatter.format(cartItem.product.price)} | Subtotal: ${CurrencyFormatter.format(cartItem.getSubtotal())}",
                stock = cartItem.quantity
            )
        }
        cartAdapter.updateProducts(cartProducts)

        // Update cart summary
        val totalItems = cartItems.sumOf { it.quantity }
        val uniqueProducts = cartItems.size
        tvCartItemCount.text = "$totalItems item${if (totalItems != 1) "s" else ""} ($uniqueProducts product${if (uniqueProducts != 1) "s" else ""})"

        // Show/hide empty cart state
        if (cartItems.isEmpty()) {
            rvCartItems.visibility = View.GONE
            layoutEmptyCart.visibility = View.VISIBLE
        } else {
            rvCartItems.visibility = View.VISIBLE
            layoutEmptyCart.visibility = View.GONE
        }

        updateTotal()
    }

    private fun updateTotal() {
        val total = cartItems.sumOf { it.getSubtotal() }
        tvTotal.text = CurrencyFormatter.format(total)

        // Enable/disable checkout button
        btnCheckout.isEnabled = cartItems.isNotEmpty()
        btnCheckout.alpha = if (cartItems.isNotEmpty()) 1.0f else 0.5f
    }

    private fun showClearCartConfirmation() {
        if (cartItems.isEmpty()) {
            Toast.makeText(requireContext(), "Cart is already empty", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear Cart")
            .setMessage("Remove all ${cartItems.sumOf { it.quantity }} items from cart?")
            .setPositiveButton("Clear All") { _, _ ->
                clearCart()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processSale() {
        if (cartItems.isEmpty()) {
            Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val customerName = etCustomerName.text.toString().ifEmpty { "Guest Customer" }
        val total = cartItems.sumOf { it.getSubtotal() }
        val totalItems = cartItems.sumOf { it.quantity }

        // Build detailed sale summary
        val itemsSummary = cartItems.joinToString("\n") {
            "• ${it.quantity}x ${it.product.name} - ${CurrencyFormatter.format(it.getSubtotal())}"
        }

        val message = """
            Customer: $customerName
            Items: $totalItems total
            
            $itemsSummary
            
            Total: ${CurrencyFormatter.format(total)}
            
            Process this sale?
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Sale")
            .setMessage(message)
            .setPositiveButton("Process Sale") { _, _ ->
                completeSale(customerName, total)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun completeSale(customerName: String, total: Double) {
        val saleItems = cartItems.map {
            SaleItem(it.product.id, it.product.name, it.quantity, it.product.price)
        }

        val sale = Sale(customerName = customerName, items = saleItems, total = total)
        val saleId = dbHelper.addSale(sale)

        if (saleId > 0) {
            // Update stock for each item
            var allStockUpdated = true
            val stockUpdates = mutableListOf<String>()

            cartItems.forEach { cartItem ->
                val newStock = cartItem.product.stock - cartItem.quantity
                if (dbHelper.updateProductStock(cartItem.product.id, newStock)) {
                    stockUpdates.add("${cartItem.product.name}: ${cartItem.product.stock} → $newStock")
                } else {
                    allStockUpdated = false
                }
            }

            if (allStockUpdated) {
                showSaleSuccessDialog(customerName, total, saleItems.size, stockUpdates)
                clearCart()
                loadAvailableProducts() // Refresh to show updated stock
            } else {
                Toast.makeText(requireContext(), "Sale saved but some stock updates failed", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "Failed to process sale", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSaleSuccessDialog(customerName: String, total: Double, itemCount: Int, stockUpdates: List<String>) {
        val updatesText = stockUpdates.joinToString("\n")

        val message = """
            ✅ Sale Complete!
            
            Customer: $customerName
            Items: $itemCount
            Total: ${CurrencyFormatter.format(total)}
            
            Stock Updates:
            $updatesText
            
            Thank you for your business! 🎉
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sale Successful")
            .setMessage(message)
            .setPositiveButton("New Sale") { _, _ ->
                // Ready for next sale
            }
            .setCancelable(false)
            .show()
    }

    private fun clearCart() {
        cartItems.clear()
        etCustomerName.text?.clear()
        updateCartDisplay()
    }

    override fun onResume() {
        super.onResume()
        loadAvailableProducts() // Refresh when fragment becomes visible
    }
}
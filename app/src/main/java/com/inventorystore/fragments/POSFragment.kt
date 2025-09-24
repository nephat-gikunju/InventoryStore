package com.inventorystore.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
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

    data class CartItem(val product: Product, var quantity: Int)

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
        // Available Products Adapter - for adding to cart
        availableProductsAdapter = ProductAdapter(
            products = emptyList(),
            onProductClick = { product ->
                addToCart(product)
            }
        )

        // Cart Items Adapter - for removing from cart
        cartAdapter = ProductAdapter(
            products = emptyList(),
            onProductClick = { product ->
                showRemoveFromCartOptions(product)
            }
        )

        rvAvailableProducts.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = availableProductsAdapter
        }

        rvCartItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }
    }

    private fun setupClickListeners() {
        btnCheckout.setOnClickListener { processSale() }
        btnClearCart.setOnClickListener { clearCart() }
    }

    private fun loadAvailableProducts() {
        val products = dbHelper.getAllProducts().filter { it.stock > 0 }
        availableProductsAdapter.updateProducts(products)
    }

    private fun addToCart(product: Product) {
        val existingItem = cartItems.find { it.product.id == product.id }
        if (existingItem != null) {
            if (existingItem.quantity < product.stock) {
                existingItem.quantity++
                Toast.makeText(requireContext(), "Added another ${product.name} to cart", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Not enough stock available", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            cartItems.add(CartItem(product, 1))
            Toast.makeText(requireContext(), "Added ${product.name} to cart", Toast.LENGTH_SHORT).show()
        }
        updateCartDisplay()
    }

    private fun showRemoveFromCartOptions(product: Product) {
        val cartItem = cartItems.find { it.product.id == product.id } ?: return

        val options = if (cartItem.quantity > 1) {
            arrayOf("Remove 1 item", "Remove all (${cartItem.quantity} items)")
        } else {
            arrayOf("Remove from cart")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove ${product.name}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (cartItem.quantity > 1) {
                            cartItem.quantity--
                            Toast.makeText(requireContext(), "Removed 1 ${product.name} from cart", Toast.LENGTH_SHORT).show()
                        } else {
                            cartItems.remove(cartItem)
                            Toast.makeText(requireContext(), "Removed ${product.name} from cart", Toast.LENGTH_SHORT).show()
                        }
                        updateCartDisplay()
                    }
                    1 -> {
                        cartItems.remove(cartItem)
                        Toast.makeText(requireContext(), "Removed all ${product.name} from cart", Toast.LENGTH_SHORT).show()
                        updateCartDisplay()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateCartDisplay() {
        // Update cart items list
        val cartProducts = cartItems.map { cartItem ->
            // Create a modified product that shows quantity in the name
            cartItem.product.copy(
                name = "${cartItem.product.name} (${cartItem.quantity}x)",
                stock = cartItem.quantity // Show quantity as stock for display
            )
        }
        cartAdapter.updateProducts(cartProducts)

        // Update cart item count
        val totalItems = cartItems.sumOf { it.quantity }
        tvCartItemCount.text = "$totalItems item${if (totalItems != 1) "s" else ""}"

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
        val total = cartItems.sumOf { it.product.price * it.quantity }
        tvTotal.text = CurrencyFormatter.format(total)

        // Enable/disable checkout button
        btnCheckout.isEnabled = cartItems.isNotEmpty()
        btnCheckout.alpha = if (cartItems.isNotEmpty()) 1.0f else 0.5f
    }

    private fun processSale() {
        if (cartItems.isEmpty()) {
            Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val customerName = etCustomerName.text.toString().ifEmpty { "Guest Customer" }
        val total = cartItems.sumOf { it.product.price * it.quantity }
        val saleItems = cartItems.map {
            SaleItem(it.product.id, it.product.name, it.quantity, it.product.price)
        }

        // Show confirmation dialog
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Sale")
            .setMessage("Process sale for ${CurrencyFormatter.format(total)}?\n\nCustomer: $customerName\nItems: ${cartItems.size} products")
            .setPositiveButton("Confirm Sale") { _, _ ->
                completeSale(customerName, saleItems, total)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun completeSale(customerName: String, saleItems: List<SaleItem>, total: Double) {
        val sale = Sale(customerName = customerName, items = saleItems, total = total)
        val saleId = dbHelper.addSale(sale)

        if (saleId > 0) {
            // Update stock for each item
            var allStockUpdated = true
            cartItems.forEach { cartItem ->
                val newStock = cartItem.product.stock - cartItem.quantity
                if (!dbHelper.updateProductStock(cartItem.product.id, newStock)) {
                    allStockUpdated = false
                }
            }

            if (allStockUpdated) {
                Toast.makeText(requireContext(), "Sale processed successfully!", Toast.LENGTH_LONG).show()
                clearCart()
                loadAvailableProducts() // Refresh products to show updated stock

                // Show sale summary
                showSaleSummary(customerName, total, saleItems.size)
            } else {
                Toast.makeText(requireContext(), "Sale saved but some stock updates failed", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "Failed to process sale", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSaleSummary(customerName: String, total: Double, itemCount: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sale Complete! 🎉")
            .setMessage("Customer: $customerName\nItems: $itemCount\nTotal: ${CurrencyFormatter.format(total)}\n\nThank you for your business!")
            .setPositiveButton("New Sale") { _, _ ->
                // Already cleared, ready for new sale
            }
            .setCancelable(false)
            .show()
    }

    private fun clearCart() {
        cartItems.clear()
        etCustomerName.text?.clear()
        updateCartDisplay()
        Toast.makeText(requireContext(), "Cart cleared", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadAvailableProducts() // Refresh products when fragment becomes visible
    }
}
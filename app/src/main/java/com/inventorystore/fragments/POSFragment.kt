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
    private lateinit var etCustomerName: EditText
    private lateinit var btnCheckout: Button

    private lateinit var productAdapter: ProductAdapter
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
        loadProducts()
        updateTotal()
    }

    private fun initViews(view: View) {
        rvAvailableProducts = view.findViewById(R.id.rv_available_products)
        rvCartItems = view.findViewById(R.id.rv_cart_items)
        tvTotal = view.findViewById(R.id.tv_total)
        etCustomerName = view.findViewById(R.id.et_customer_name)
        btnCheckout = view.findViewById(R.id.btn_checkout)

        btnCheckout.setOnClickListener { processSale() }
    }

    private fun setupRecyclerViews() {
        productAdapter = ProductAdapter(emptyList()) { product ->
            addToCart(product)
        }

        cartAdapter = ProductAdapter(emptyList()) { product ->
            removeFromCart(product)
        }

        rvAvailableProducts.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = productAdapter
        }

        rvCartItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }
    }

    private fun loadProducts() {
        val products = dbHelper.getAllProducts().filter { it.stock > 0 }
        productAdapter.updateProducts(products)
    }

    private fun addToCart(product: Product) {
        val existingItem = cartItems.find { it.product.id == product.id }
        if (existingItem != null) {
            if (existingItem.quantity < product.stock) {
                existingItem.quantity++
            } else {
                Toast.makeText(requireContext(), "Not enough stock available", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            cartItems.add(CartItem(product, 1))
        }
        updateCartDisplay()
        updateTotal()
    }

    private fun removeFromCart(product: Product) {
        cartItems.removeAll { it.product.id == product.id }
        updateCartDisplay()
        updateTotal()
    }

    private fun updateCartDisplay() {
        val cartProducts = cartItems.map { it.product }
        cartAdapter.updateProducts(cartProducts)
    }

    private fun updateTotal() {
        val total = cartItems.sumOf { it.product.price * it.quantity }
        tvTotal.text = "Total: ${CurrencyFormatter.format(total)}"
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

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Sale")
            .setMessage("Process sale for ${CurrencyFormatter.format(total)}?")
            .setPositiveButton("Confirm") { _, _ ->
                val sale = Sale(customerName = customerName, items = saleItems, total = total)
                val saleId = dbHelper.addSale(sale)

                if (saleId > 0) {
                    // Update stock for each item
                    cartItems.forEach { cartItem ->
                        val newStock = cartItem.product.stock - cartItem.quantity
                        dbHelper.updateProductStock(cartItem.product.id, newStock)
                    }

                    Toast.makeText(requireContext(), "Sale processed successfully!", Toast.LENGTH_SHORT).show()
                    clearCart()
                    loadProducts() // Refresh products to show updated stock
                } else {
                    Toast.makeText(requireContext(), "Failed to process sale", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearCart() {
        cartItems.clear()
        etCustomerName.text.clear()
        updateCartDisplay()
        updateTotal()
    }
}
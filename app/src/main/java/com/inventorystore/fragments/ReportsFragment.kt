package com.inventorystore.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.inventorystore.AddEditProductActivity
import com.inventorystore.R
import com.inventorystore.adapters.ProductAdapter
import com.inventorystore.data.DatabaseHelper
import com.inventorystore.data.Product
import com.inventorystore.utils.CurrencyFormatter

class ReportsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvTotalRevenue: TextView
    private lateinit var tvLowStockItems: TextView
    private lateinit var tvTotalSales: TextView
    private lateinit var rvLowStockProducts: RecyclerView
    private lateinit var layoutNoLowStock: View
    private lateinit var btnRefreshReports: MaterialButton
    private lateinit var tvViewAllLowStock: TextView
    private lateinit var lowStockAdapter: ProductAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())

        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        loadReports()
    }

    private fun initViews(view: View) {
        tvTotalRevenue = view.findViewById(R.id.tv_total_revenue)
        tvLowStockItems = view.findViewById(R.id.tv_low_stock_items)
        tvTotalSales = view.findViewById(R.id.tv_total_sales)
        rvLowStockProducts = view.findViewById(R.id.rv_low_stock_products)
        layoutNoLowStock = view.findViewById(R.id.layout_no_low_stock)
        btnRefreshReports = view.findViewById(R.id.btn_refresh_reports)
        tvViewAllLowStock = view.findViewById(R.id.tv_view_all_low_stock)
    }

    private fun setupRecyclerView() {
        lowStockAdapter = ProductAdapter(
            products = emptyList(),
            onProductClick = { product ->
                // Handle low stock product click - open edit to restock
                openProductForRestock(product)
            }
        )
        rvLowStockProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = lowStockAdapter
        }
    }

    private fun setupClickListeners() {
        btnRefreshReports.setOnClickListener {
            loadReports()
        }

        tvViewAllLowStock.setOnClickListener {
            // Could navigate to inventory with low stock filter
            // For now, just refresh the reports
            loadReports()
        }
    }

    private fun loadReports() {
        // Load revenue data
        val totalRevenue = dbHelper.getTotalRevenue()
        tvTotalRevenue.text = CurrencyFormatter.format(totalRevenue)

        // Load sales count (you might need to add this method to DatabaseHelper)
        val salesCount = getSalesCount()
        tvTotalSales.text = salesCount.toString()

        // Load low stock products
        val allProducts = dbHelper.getAllProducts()
        val lowStockProducts = allProducts.filter { it.isLowStock() }

        tvLowStockItems.text = lowStockProducts.size.toString()

        if (lowStockProducts.isEmpty()) {
            rvLowStockProducts.visibility = View.GONE
            layoutNoLowStock.visibility = View.VISIBLE
        } else {
            rvLowStockProducts.visibility = View.VISIBLE
            layoutNoLowStock.visibility = View.GONE
            lowStockAdapter.updateProducts(lowStockProducts)
        }
    }

    private fun getSalesCount(): Int {
        // This is a simple implementation - you might want to add this to DatabaseHelper
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM sales", null)
        var count = 0
        cursor.use {
            if (it.moveToFirst()) {
                count = it.getInt(0)
            }
        }
        return count
    }

    private fun openProductForRestock(product: Product) {
        // Open edit product screen to allow restocking
        val intent = Intent(requireContext(), AddEditProductActivity::class.java).apply {
            putExtra(AddEditProductActivity.EXTRA_IS_EDIT_MODE, true)
            putExtra(AddEditProductActivity.EXTRA_PRODUCT_ID, product.id)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadReports() // Refresh reports when fragment becomes visible
    }

    // Additional helper methods for enhanced reporting

    private fun getTopSellingProducts(): List<Product> {
        // This would require a more complex query to get actual sales data
        // For now, return empty list - you can enhance this later
        return emptyList()
    }

    private fun getRevenueByCategory(): Map<String, Double> {
        // This would require joining sales with products
        // For now, return empty map - you can enhance this later
        return emptyMap()
    }

    private fun getTodaysRevenue(): Double {
        // Get today's sales revenue
        val db = dbHelper.readableDatabase
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val cursor = db.rawQuery(
            "SELECT SUM(total) FROM sales WHERE date LIKE ?",
            arrayOf("$today%")
        )
        var revenue = 0.0
        cursor.use {
            if (it.moveToFirst()) {
                revenue = it.getDouble(0)
            }
        }
        return revenue
    }

    private fun getWeeklyRevenue(): Double {
        // Get this week's revenue - simplified implementation
        val db = dbHelper.readableDatabase
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
        val weekAgo = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calendar.time)

        val cursor = db.rawQuery(
            "SELECT SUM(total) FROM sales WHERE date >= ?",
            arrayOf(weekAgo)
        )
        var revenue = 0.0
        cursor.use {
            if (it.moveToFirst()) {
                revenue = it.getDouble(0)
            }
        }
        return revenue
    }
}
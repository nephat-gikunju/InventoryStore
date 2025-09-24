package com.inventorystore.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.inventorystore.R
import com.inventorystore.adapters.SalesAdapter
import com.inventorystore.data.DatabaseHelper
import com.inventorystore.data.Sale
import com.inventorystore.data.SaleItem
import com.inventorystore.utils.CurrencyFormatter
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvTotalProducts: TextView
    private lateinit var tvRevenue: TextView
    private lateinit var tvLowStock: TextView
    private lateinit var rvRecentSales: RecyclerView
    private lateinit var salesAdapter: SalesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())

        initViews(view)
        setupRecyclerView()
        loadDashboardData()
    }

    private fun initViews(view: View) {
        tvTotalProducts = view.findViewById(R.id.tv_total_products)
        tvRevenue = view.findViewById(R.id.tv_revenue)
        tvLowStock = view.findViewById(R.id.tv_low_stock)
        rvRecentSales = view.findViewById(R.id.rv_recent_sales)
    }

    private fun setupRecyclerView() {
        salesAdapter = SalesAdapter(emptyList())
        rvRecentSales.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = salesAdapter
        }
    }

    private fun loadDashboardData() {
        val products = dbHelper.getAllProducts()
        val revenue = dbHelper.getTotalRevenue()
        val lowStockCount = dbHelper.getLowStockCount()

        tvTotalProducts.text = products.size.toString()
        tvRevenue.text = CurrencyFormatter.format(revenue)
        tvLowStock.text = lowStockCount.toString()

        // Sample recent sales data
        val recentSales = listOf(
            Sale(1, "Guest Customer", listOf(SaleItem(1, "Ear Buds", 1, 100.0)), 100.0),
            Sale(2, "John Doe", listOf(SaleItem(2, "Mouse", 1, 45.0)), 45.0),
            Sale(3, "Jane Smith", listOf(SaleItem(3, "Power Bank", 2, 100.0)), 200.0)
        )
        salesAdapter.updateSales(recentSales)
    }
}
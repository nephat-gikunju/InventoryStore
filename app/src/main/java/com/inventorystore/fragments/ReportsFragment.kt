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
import com.inventorystore.adapters.ProductAdapter
import com.inventorystore.data.DatabaseHelper
import com.inventorystore.utils.CurrencyFormatter

class ReportsFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvTotalRevenue: TextView
    private lateinit var tvLowStockItems: TextView
    private lateinit var rvLowStockProducts: RecyclerView
    private lateinit var lowStockAdapter: ProductAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())

        initViews(view)
        setupRecyclerView()
        loadReports()
    }

    private fun initViews(view: View) {
        tvTotalRevenue = view.findViewById(R.id.tv_total_revenue)
        tvLowStockItems = view.findViewById(R.id.tv_low_stock_items)
        rvLowStockProducts = view.findViewById(R.id.rv_low_stock_products)
    }

    private fun setupRecyclerView() {
        lowStockAdapter = ProductAdapter(emptyList()) { }
        rvLowStockProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = lowStockAdapter
        }
    }

    private fun loadReports() {
        val totalRevenue = dbHelper.getTotalRevenue()
        val allProducts = dbHelper.getAllProducts()
        val lowStockProducts = allProducts.filter { it.isLowStock() }

        tvTotalRevenue.text = CurrencyFormatter.format(totalRevenue)
        tvLowStockItems.text = lowStockProducts.size.toString()
        lowStockAdapter.updateProducts(lowStockProducts)
    }
}
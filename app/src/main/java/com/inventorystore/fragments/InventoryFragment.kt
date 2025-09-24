package com.inventorystore.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.inventorystore.R
import com.inventorystore.adapters.ProductAdapter
import com.inventorystore.data.DatabaseHelper
import com.inventorystore.data.Product

class InventoryFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var etSearch: TextInputEditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var rvProducts: RecyclerView
    private lateinit var layoutEmptyProducts: View
    private lateinit var productAdapter: ProductAdapter
    private var allProducts = listOf<Product>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_inventory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())

        initViews(view)
        setupRecyclerView()
        setupSearchAndFilter()
        loadProducts()
    }

    private fun initViews(view: View) {
        etSearch = view.findViewById(R.id.et_search)
        chipGroup = view.findViewById(R.id.chip_group)
        rvProducts = view.findViewById(R.id.rv_products)
        layoutEmptyProducts = view.findViewById(R.id.layout_empty_products)
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(emptyList()) { product ->
            // Handle product click (e.g., edit product)
            // You can implement product detail/edit functionality here
        }
        rvProducts.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productAdapter
        }
    }

    private fun setupSearchAndFilter() {
        // Setup search functionality with TextWatcher
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProducts(s.toString(), getSelectedCategory())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Setup chip group selection listener
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedCategory = if (checkedIds.isNotEmpty()) {
                val chip = chipGroup.findViewById<Chip>(checkedIds[0])
                chip?.text?.toString()
            } else {
                null
            }
            filterProducts(etSearch.text.toString(), selectedCategory)
        }
    }

    private fun loadProducts() {
        allProducts = dbHelper.getAllProducts()
        productAdapter.updateProducts(allProducts)
        setupCategoryChips()
        updateEmptyState()
    }

    private fun setupCategoryChips() {
        val categories = allProducts.map { it.category }.distinct()
        chipGroup.removeAllViews()

        categories.forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category
                isCheckable = true
                setChipBackgroundColorResource(R.color.surface_variant)
                setTextColor(resources.getColorStateList(R.color.on_surface, null))
            }
            chipGroup.addView(chip)
        }
    }

    private fun filterProducts(query: String, category: String?) {
        var filteredProducts = allProducts

        if (query.isNotEmpty()) {
            filteredProducts = filteredProducts.filter { product ->
                product.name.contains(query, ignoreCase = true) ||
                        product.description.contains(query, ignoreCase = true)
            }
        }

        if (!category.isNullOrEmpty()) {
            filteredProducts = filteredProducts.filter { it.category == category }
        }

        productAdapter.updateProducts(filteredProducts)
        updateEmptyState(filteredProducts)
    }

    private fun getSelectedCategory(): String? {
        val checkedChipId = chipGroup.checkedChipId
        return if (checkedChipId != View.NO_ID) {
            val chip = chipGroup.findViewById<Chip>(checkedChipId)
            chip?.text?.toString()
        } else {
            null
        }
    }

    private fun updateEmptyState(products: List<Product> = allProducts) {
        if (products.isEmpty()) {
            rvProducts.visibility = View.GONE
            layoutEmptyProducts.visibility = View.VISIBLE
        } else {
            rvProducts.visibility = View.VISIBLE
            layoutEmptyProducts.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh products when fragment becomes visible
        loadProducts()
    }
}
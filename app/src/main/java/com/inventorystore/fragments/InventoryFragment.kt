package com.inventorystore.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.inventorystore.AddEditProductActivity
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
    private lateinit var btnAddProduct: MaterialButton
    private lateinit var productAdapter: ProductAdapter
    private var allProducts = listOf<Product>()

    private val addEditProductLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadProducts() // Refresh the product list
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_inventory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())

        initViews(view)
        setupRecyclerView()
        setupSearchAndFilter()
        setupClickListeners()
        loadProducts()
    }

    private fun initViews(view: View) {
        etSearch = view.findViewById(R.id.et_search)
        chipGroup = view.findViewById(R.id.chip_group)
        rvProducts = view.findViewById(R.id.rv_products)
        layoutEmptyProducts = view.findViewById(R.id.layout_empty_products)
        btnAddProduct = view.findViewById(R.id.btn_add_product)
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            products = emptyList(),
            onProductClick = { product ->
                // Handle product click - open edit dialog
                openEditProduct(product)
            },
            onProductUpdated = { updatedProduct ->
                // Handle product update from stock adjustment dialog
                handleProductUpdate(updatedProduct)
            }
        )
        rvProducts.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productAdapter
        }
    }

    private fun setupClickListeners() {
        btnAddProduct.setOnClickListener {
            openAddProduct()
        }
    }

    private fun setupSearchAndFilter() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProducts(s.toString(), getSelectedCategory())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

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
        val categories = dbHelper.getCategories()
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

    private fun openAddProduct() {
        val intent = Intent(requireContext(), AddEditProductActivity::class.java).apply {
            putExtra(AddEditProductActivity.EXTRA_IS_EDIT_MODE, false)
        }
        addEditProductLauncher.launch(intent)
    }

    private fun openEditProduct(product: Product) {
        val intent = Intent(requireContext(), AddEditProductActivity::class.java).apply {
            putExtra(AddEditProductActivity.EXTRA_IS_EDIT_MODE, true)
            putExtra(AddEditProductActivity.EXTRA_PRODUCT_ID, product.id)
        }
        addEditProductLauncher.launch(intent)
    }

    private fun handleProductUpdate(updatedProduct: Product) {
        // Update the product in the adapter
        productAdapter.updateProduct(updatedProduct)

        // Update the local list
        val index = allProducts.indexOfFirst { it.id == updatedProduct.id }
        if (index != -1) {
            val mutableProducts = allProducts.toMutableList()
            mutableProducts[index] = updatedProduct
            allProducts = mutableProducts
        }
    }

    override fun onResume() {
        super.onResume()
        loadProducts()
    }
}
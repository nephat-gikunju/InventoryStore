package com.inventorystore.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.inventorystore.R
import com.inventorystore.data.Product
import com.inventorystore.utils.CurrencyFormatter

class ProductAdapter(
    private var products: List<Product>,
    private val onProductClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProduct: ImageView = view.findViewById(R.id.iv_product)
        val tvName: TextView = view.findViewById(R.id.tv_product_name)
        val tvDescription: TextView = view.findViewById(R.id.tv_product_description)
        val tvPrice: TextView = view.findViewById(R.id.tv_product_price)
        val tvStock: TextView = view.findViewById(R.id.tv_product_stock)
        val tvCategory: TextView = view.findViewById(R.id.tv_product_category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        holder.tvName.text = product.name
        holder.tvDescription.text = product.description
        holder.tvPrice.text = CurrencyFormatter.format(product.price)
        holder.tvStock.text = "Stock: ${product.stock}"
        holder.tvCategory.text = product.category

        // Set stock color based on availability
        if (product.isLowStock()) {
            holder.tvStock.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.error))
        } else {
            holder.tvStock.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.success))
        }

        // Set placeholder image based on category
        when (product.category.lowercase()) {
            "electronics" -> holder.ivProduct.setImageResource(R.drawable.ic_electronics)
            "accessory" -> holder.ivProduct.setImageResource(R.drawable.ic_accessory)
            else -> holder.ivProduct.setImageResource(R.drawable.ic_product)
        }

        holder.itemView.setOnClickListener { onProductClick(product) }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}
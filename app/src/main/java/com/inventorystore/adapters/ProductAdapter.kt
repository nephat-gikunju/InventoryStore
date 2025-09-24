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
import com.inventorystore.dialogs.StockAdjustmentDialog
import com.inventorystore.utils.CurrencyFormatter

class ProductAdapter(
    private var products: List<Product>,
    private val onProductClick: (Product) -> Unit,
    private val onProductUpdated: ((Product) -> Unit)? = null
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProduct: ImageView = view.findViewById(R.id.iv_product)
        val tvName: TextView = view.findViewById(R.id.tv_product_name)
        val tvDescription: TextView = view.findViewById(R.id.tv_product_description)
        val tvPrice: TextView = view.findViewById(R.id.tv_product_price)
        val tvStock: TextView = view.findViewById(R.id.tv_product_stock)
        val tvCategory: TextView = view.findViewById(R.id.tv_product_category)
        val ivStockIndicator: ImageView = view.findViewById(R.id.iv_stock_indicator)
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

        // Set stock color and indicator based on availability
        if (product.isLowStock()) {
            holder.tvStock.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.error))
            holder.ivStockIndicator.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.error))
        } else if (product.stock == 0) {
            holder.tvStock.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.error))
            holder.ivStockIndicator.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.error))
            holder.tvStock.text = "Out of Stock"
        } else {
            holder.tvStock.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.success))
            holder.ivStockIndicator.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.success))
        }

        // Set placeholder image based on category
        when (product.category.lowercase()) {
            "electronics" -> holder.ivProduct.setImageResource(R.drawable.ic_electronics)
            "accessory" -> holder.ivProduct.setImageResource(R.drawable.ic_accessory)
            else -> holder.ivProduct.setImageResource(R.drawable.ic_product)
        }

        // Click listeners
        holder.itemView.setOnClickListener { onProductClick(product) }

        // Long click for quick stock adjustment
        holder.itemView.setOnLongClickListener {
            if (onProductUpdated != null) {
                val dialog = StockAdjustmentDialog(holder.itemView.context, product) { updatedProduct ->
                    onProductUpdated.invoke(updatedProduct)
                }
                dialog.show()
            }
            true
        }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    fun updateProduct(updatedProduct: Product) {
        val index = products.indexOfFirst { it.id == updatedProduct.id }
        if (index != -1) {
            val mutableProducts = products.toMutableList()
            mutableProducts[index] = updatedProduct
            products = mutableProducts
            notifyItemChanged(index)
        }
    }
}
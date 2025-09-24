package com.inventorystore.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.inventorystore.R
import com.inventorystore.data.Sale
import com.inventorystore.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

class SalesAdapter(
    private var sales: List<Sale>,
    private val onSaleClick: ((Sale) -> Unit)? = null
) : RecyclerView.Adapter<SalesAdapter.SaleViewHolder>() {

    class SaleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCustomerName: TextView = view.findViewById(R.id.tv_customer_name)
        val tvSaleTotal: TextView = view.findViewById(R.id.tv_sale_total)
        val tvSaleDate: TextView = view.findViewById(R.id.tv_sale_date)
        val tvItemCount: TextView = view.findViewById(R.id.tv_item_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sale, parent, false)
        return SaleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaleViewHolder, position: Int) {
        val sale = sales[position]

        // Set customer name
        holder.tvCustomerName.text = sale.customerName

        // Format and set sale total
        holder.tvSaleTotal.text = CurrencyFormatter.format(sale.total)

        // Format and set item count with proper pluralization
        val itemCount = sale.items.size
        holder.tvItemCount.text = "$itemCount item${if (itemCount != 1) "s" else ""}"

        // Format and set sale date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        holder.tvSaleDate.text = dateFormat.format(sale.date)

        // Set click listener if provided
        onSaleClick?.let { clickListener ->
            holder.itemView.setOnClickListener {
                clickListener(sale)
            }
            // Add visual feedback for clickable items
            holder.itemView.isClickable = true
            holder.itemView.isFocusable = true
        }
    }

    override fun getItemCount(): Int = sales.size

    /**
     * Update the entire sales list
     */
    fun updateSales(newSales: List<Sale>) {
        sales = newSales
        notifyDataSetChanged()
    }

    /**
     * Add a new sale to the beginning of the list
     */
    fun addSale(sale: Sale) {
        val mutableSales = sales.toMutableList()
        mutableSales.add(0, sale)
        sales = mutableSales
        notifyItemInserted(0)
    }

    /**
     * Remove a sale at the specified position
     */
    fun removeSale(position: Int) {
        if (position in 0 until sales.size) {
            val mutableSales = sales.toMutableList()
            mutableSales.removeAt(position)
            sales = mutableSales
            notifyItemRemoved(position)
        }
    }

    /**
     * Get the current list of sales
     */
    fun getSales(): List<Sale> = sales

    /**
     * Clear all sales from the list
     */
    fun clearSales() {
        val previousSize = sales.size
        sales = emptyList()
        notifyItemRangeRemoved(0, previousSize)
    }

    /**
     * Get a specific sale by position
     */
    fun getSaleAt(position: Int): Sale? {
        return if (position in 0 until sales.size) {
            sales[position]
        } else {
            null
        }
    }

    /**
     * Check if the adapter has any sales
     */
    fun isEmpty(): Boolean = sales.isEmpty()

    /**
     * Get the total count of sales
     */
    fun getCount(): Int = sales.size
}
package com.bexmarket.ng.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.button.MaterialButton

class MyPostsAdapter(
    private var products: List<MarketItem>,
    private val onEditClick: (MarketItem) -> Unit,
    private val onDeleteClick: (MarketItem) -> Unit
) : RecyclerView.Adapter<MyPostsAdapter.MyPostViewHolder>() {

    class MyPostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProductImage: ImageView = view.findViewById(R.id.ivProductImage)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
        val tvProductPrice: TextView = view.findViewById(R.id.tvProductPrice)
        val btnEdit: MaterialButton = view.findViewById(R.id.btnEdit)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyPostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_post, parent, false)
        return MyPostViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyPostViewHolder, position: Int) {
        val product = products[position]
        holder.tvProductName.text = product.name
        holder.tvProductPrice.text = "₦${product.price}"
        
        holder.ivProductImage.load(product.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_market)
            error(R.drawable.ic_market)
        }

        holder.btnEdit.setOnClickListener { onEditClick(product) }
        holder.btnDelete.setOnClickListener { onDeleteClick(product) }
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<MarketItem>) {
        this.products = newProducts
        notifyDataSetChanged()
    }
}

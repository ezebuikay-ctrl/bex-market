package com.bexmarket.ng.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private var categories: List<String>,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedCategory: String = "All"

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryName: TextView = view.findViewById(R.id.categoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.categoryName.text = category
        
        // Highlight logic
        if (category == selectedCategory) {
            holder.categoryName.setBackgroundResource(R.drawable.bg_category_tag_selected)
            holder.categoryName.setTextColor(Color.WHITE)
        } else {
            holder.categoryName.setBackgroundResource(R.drawable.bg_category_tag)
            holder.categoryName.setTextColor(Color.parseColor("#0b6b2e"))
        }

        holder.itemView.setOnClickListener {
            val oldSelected = selectedCategory
            selectedCategory = category
            
            // Notify changes to update UI
            val oldIndex = categories.indexOf(oldSelected)
            val newIndex = categories.indexOf(category)
            if (oldIndex != -1) notifyItemChanged(oldIndex)
            if (newIndex != -1) notifyItemChanged(newIndex)
            
            onCategoryClick(category)
        }
    }

    override fun getItemCount(): Int = categories.size

    fun updateCategories(newCategories: List<String>, current: String) {
        this.categories = newCategories
        this.selectedCategory = current
        notifyDataSetChanged()
    }
}

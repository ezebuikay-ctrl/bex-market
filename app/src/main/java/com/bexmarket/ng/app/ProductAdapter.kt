package com.bexmarket.ng.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

import android.widget.Button
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

class ProductAdapter(
    private var items: List<Any>,
    private val onFavoriteClick: (MarketItem) -> Unit = {},
    private val onProductClick: (MarketItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_PRODUCT = 0
        private const val TYPE_AD = 1
    }

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImage: ImageView = view.findViewById(R.id.productImage)
        val productName: TextView = view.findViewById(R.id.productName)
        val productPrice: TextView = view.findViewById(R.id.productPrice)
        val productCategory: TextView = view.findViewById(R.id.productCategory)
        val btnFavorite: ImageView = view.findViewById(R.id.btnFavorite)
    }

    class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val adView: NativeAdView = view as NativeAdView
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is NativeAd) TYPE_AD else TYPE_PRODUCT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_AD) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.ad_unified_grid, parent, false)
            AdViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_product, parent, false)
            ProductViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_AD) {
            populateNativeAd(items[position] as NativeAd, (holder as AdViewHolder).adView)
        } else {
            val product = items[position] as MarketItem
            val productHolder = holder as ProductViewHolder
            productHolder.productName.text = product.name
            productHolder.productPrice.text = "₦${product.price}"
            productHolder.productCategory.text = product.category ?: "General"
            
            productHolder.productImage.load(product.imageUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.stat_notify_error)
                diskCachePolicy(coil.request.CachePolicy.ENABLED)
                memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            }

            productHolder.btnFavorite.setImageResource(
                if (product.isFavorite) android.R.drawable.btn_star_big_on 
                else android.R.drawable.btn_star_big_off
            )

            productHolder.btnFavorite.setOnClickListener { onFavoriteClick(product) }
            productHolder.itemView.setOnClickListener { onProductClick(product) }
        }
    }

    private fun populateNativeAd(nativeAd: NativeAd, adView: NativeAdView) {
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.mediaView = adView.findViewById(R.id.ad_media)

        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.mediaContent?.let { adView.mediaView?.setMediaContent(it) }

        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as Button).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(nativeAd.icon?.drawable)
            adView.iconView?.visibility = View.VISIBLE
        }

        adView.setNativeAd(nativeAd)
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<Any>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}

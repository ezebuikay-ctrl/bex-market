package com.bexmarket.ng.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd

class SearchFragment : Fragment() {
    private val viewModel: MarketViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var productAdapter: ProductAdapter
    private var nativeAd: NativeAd? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearch(view)
        setupRecyclerView(view)
        loadNativeAd()
        observeViewModel()
    }

    private fun loadNativeAd() {
        val adUnitId = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-9972987433387087/4007265985"
        val adLoader = AdLoader.Builder(requireContext(), adUnitId)
            .forNativeAd { ad ->
                nativeAd = ad
                // If we already have products, refresh the list to include the ad
                val state = viewModel.uiState.value
                if (state is MarketUiState.Success) {
                    updateListWithAds(state.items)
                }
            }
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun setupSearch(view: View) {
        val searchEditText: EditText = view.findViewById(R.id.searchEditText)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onSearchQueryChange(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView: RecyclerView = view.findViewById(R.id.searchResultsRecyclerView)
        productAdapter = ProductAdapter(
            items = emptyList(),
            onFavoriteClick = { product ->
                authViewModel.currentUser.value?.let { user ->
                    viewModel.toggleFavorite(user.uid, product.id)
                }
            },
            onProductClick = { product ->
                val intent = Intent(requireContext(), ProductDetailActivity::class.java)
                intent.putExtra("PRODUCT_JSON", Gson().toJson(product))
                startActivity(intent)
            }
        )
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (productAdapter.getItemViewType(position) == 1) 2 else 1
                }
            }
        }
        recyclerView.adapter = productAdapter
    }

    private fun updateListWithAds(products: List<MarketItem>) {
        val itemsWithAds = mutableListOf<Any>()
        products.forEachIndexed { index, product ->
            itemsWithAds.add(product)
            // Inject ad every 7 items
            nativeAd?.let { ad ->
                if ((index + 1) % 7 == 0) {
                    itemsWithAds.add(ad)
                }
            }
        }
        productAdapter.updateItems(itemsWithAds)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state is MarketUiState.Success) {
                        updateListWithAds(state.items)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        nativeAd?.destroy()
        super.onDestroyView()
    }
}

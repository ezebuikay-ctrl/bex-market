package com.bexmarket.ng.app

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class MarketUiState {
    object Loading : MarketUiState()
    data class Success(val items: List<MarketItem>) : MarketUiState()
    data class Error(val message: String) : MarketUiState()
}

class MarketViewModel : ViewModel() {
    private val _uiState = mutableStateOf<MarketUiState>(MarketUiState.Loading)

    private var allItems: List<MarketItem> = emptyList()

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    val filteredUiState: State<MarketUiState> = derivedStateOf {
        val currentState = _uiState.value
        if (currentState is MarketUiState.Success) {
            val query = _searchQuery.value
            if (query.isEmpty()) {
                currentState
            } else {
                val filtered = allItems.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
                }
                MarketUiState.Success(filtered)
            }
        } else {
            currentState
        }
    }

    init {
        fetchProducts()
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun fetchProducts() {
        viewModelScope.launch {
            _uiState.value = MarketUiState.Loading
            try {
                val products = RetrofitClient.apiService.getProducts()
                allItems = products
                _uiState.value = MarketUiState.Success(products)
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Server error, loading sample data: ${e.message}")
                
                // Fallback to sample data so the app works even if the server fails
                val sampleData = listOf(
                    MarketItem(1, "BEX Premium Rice", "₦45,000", "High quality long grain rice, stone-free.", "https://bexmarket-ng.vercel.app/images/rice.jpg"),
                    MarketItem(2, "Vegetable Oil", "₦12,500", "Pure refined vegetable oil, 5L.", "https://bexmarket-ng.vercel.app/images/oil.jpg"),
                    MarketItem(3, "Fresh Tomatoes", "₦15,000", "A basket of fresh farm tomatoes.", "https://bexmarket-ng.vercel.app/images/tomatoes.jpg"),
                    MarketItem(4, "White Onions", "₦8,000", "Bag of fresh white onions.", "https://bexmarket-ng.vercel.app/images/onions.jpg")
                )
                allItems = sampleData
                _uiState.value = MarketUiState.Success(sampleData)
            }
        }
    }
}

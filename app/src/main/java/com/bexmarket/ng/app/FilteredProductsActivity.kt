package com.bexmarket.ng.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.gson.Gson
import kotlinx.coroutines.launch

class FilteredProductsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val maxPrice = intent.getDoubleExtra("MAX_PRICE", 10000.0)
        val title = intent.getStringExtra("TITLE") ?: "Products under ₦$maxPrice"

        setContent {
            AppTheme {
                FilteredProductsScreen(
                    title = title,
                    maxPrice = maxPrice,
                    onBack = { finish() },
                    onProductClick = { product ->
                        val intent = Intent(this, ProductDetailActivity::class.java)
                        intent.putExtra("PRODUCT_JSON", Gson().toJson(product))
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilteredProductsScreen(
    title: String,
    maxPrice: Double,
    onBack: () -> Unit,
    onProductClick: (MarketItem) -> Unit
) {
    val repository = AppwriteProvider.repository
    val authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val user by authViewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    
    var products by remember { mutableStateOf<List<MarketItem>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(user) {
        isLoading = true
        products = repository.getProductsUnderPrice(maxPrice, user?.uid)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (products.isNullOrEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No affordable products available right now",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(products ?: emptyList()) { product ->
                        ProductGridItem(
                            product = product,
                            modifier = Modifier,
                            onClick = onProductClick,
                            onFavoriteToggle = {
                                user?.let { u ->
                                    scope.launch {
                                        if (product.isFavorite) {
                                            repository.removeFromFavorites(u.uid, product.id)
                                        } else {
                                            repository.addToFavorites(u.uid, product.id)
                                        }
                                        // Refresh list after toggle
                                        products = repository.getProductsUnderPrice(maxPrice, u.uid)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

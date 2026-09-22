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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.gson.Gson
import kotlinx.coroutines.launch

class SellerCatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val userId = intent.getStringExtra("USER_ID") ?: ""
        if (userId.isEmpty()) {
            finish()
            return
        }

        setContent {
            AppTheme {
                SellerCatalogScreen(
                    userId = userId,
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
fun SellerCatalogScreen(
    userId: String,
    onBack: () -> Unit,
    onProductClick: (MarketItem) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = AppwriteProvider.repository
    val authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val viewModel: MarketViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val user by authViewModel.currentUser.collectAsState()
    
    var sellerProfile by remember { mutableStateOf<UserProfile?>(null) }
    var sellerProducts by remember { mutableStateOf<List<MarketItem>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showFullScreenProfilePic by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        isLoading = true
        sellerProfile = repository.getSellerProfile(userId)
        sellerProducts = repository.getProductsByUser(userId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sellerProfile?.businessName?.ifEmpty { sellerProfile?.name } ?: "Seller Catalog") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Seller Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val profilePicUrl = sellerProfile?.profilePic
                    android.util.Log.d("SellerDebug", "Profile Image URL from Users collection: $profilePicUrl")

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (!profilePicUrl.isNullOrEmpty()) {
                                    showFullScreenProfilePic = true
                                }
                            }
                    ) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(if (profilePicUrl.isNullOrEmpty()) R.drawable.ic_person else profilePicUrl)
                                .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                .build(),
                            contentDescription = "Seller Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            placeholder = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person),
                            error = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person),
                            fallback = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person)
                        )
                    }

                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = sellerProfile?.businessName?.ifEmpty { sellerProfile?.name } ?: "Bex Seller",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${sellerProducts?.size ?: 0} Products",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))

                // Seller Products Grid
                if (sellerProducts.isNullOrEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No products found for this seller.", color = Color.Gray)
                    }
                } else {
                    var productToDelete by remember { mutableStateOf<MarketItem?>(null) }
                    
                    if (productToDelete != null) {
                        AlertDialog(
                            onDismissRequest = { productToDelete = null },
                            title = { Text("Delete Product?") },
                            text = { Text("Are you sure you want to delete '${productToDelete?.name}'? This will permanently remove the product.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        productToDelete?.let { viewModel.deleteProduct(it.id) }
                                        productToDelete = null
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                ) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { productToDelete = null }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sellerProducts ?: emptyList()) { product ->
                            val isOwner = user?.uid == product.userId
                            
                            ProductGridItem(
                                product = product,
                                modifier = Modifier,
                                onClick = onProductClick,
                                onFavoriteToggle = {
                                    user?.let { u ->
                                        viewModel.toggleFavorite(u.uid, product.id)
                                        // Refresh local list state
                                        sellerProducts = sellerProducts?.map { 
                                            if (it.id == product.id) it.copy(isFavorite = !it.isFavorite) else it
                                        }
                                    }
                                },
                                isOwner = isOwner,
                                onEdit = {
                                    val intent = Intent(context, EditProductActivity::class.java).apply {
                                        putExtra("PRODUCT_ID", product.id)
                                    }
                                    context.startActivity(intent)
                                },
                                onDelete = { productToDelete = product }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFullScreenProfilePic) {
        Dialog(
            onDismissRequest = { showFullScreenProfilePic = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(sellerProfile?.profilePic ?: R.drawable.ic_person)
                        .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                        .build(),
                    contentDescription = "Full Screen Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    error = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person)
                )
                IconButton(
                    onClick = { showFullScreenProfilePic = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

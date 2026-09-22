package com.bexmarket.ng.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil.compose.AsyncImage
import com.bexmarket.ng.app.BuildConfig

class ProfileFragment : Fragment() {
    private val authViewModel: AuthViewModel by activityViewModels()
    private val marketViewModel: MarketViewModel by activityViewModels()
    private val settingsViewModel: SettingsViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    ProfileScreen(
                        authViewModel = authViewModel,
                        marketViewModel = marketViewModel,
                        settingsViewModel = settingsViewModel,
                        onLogout = {
                            authViewModel.logout()
                            val intent = Intent(requireContext(), LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            requireActivity().finish()
                        },
                        onPostAd = {
                            startActivity(Intent(requireContext(), PostAdActivity::class.java))
                        },
                        onSettings = {
                            startActivity(Intent(requireContext(), SettingsActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    marketViewModel: MarketViewModel,
    settingsViewModel: SettingsViewModel,
    onLogout: () -> Unit,
    onPostAd: () -> Unit,
    onSettings: () -> Unit
) {
    val user by authViewModel.currentUser.collectAsState()
    val isDarkMode by settingsViewModel.isDarkMode.collectAsState(initial = false)
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    val pickMedia = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            authViewModel.updateProfilePicture(context, uri)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Settings Icon and Theme Toggle at Top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Theme Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { settingsViewModel.toggleTheme(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryColor,
                            uncheckedThumbColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color.LightGray else Color.White,
                            uncheckedTrackColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF444444) else Color(0xFFE0E0E0),
                            checkedBorderColor = Color.Transparent,
                            uncheckedBorderColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color.Gray else Color.DarkGray
                        )
                    )
                }

                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = primaryColor
                    )
                }
            }

            // User Info Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(user?.profilePic)
                            .build(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0)),
                        contentScale = ContentScale.Crop,
                        placeholder = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person),
                        error = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person),
                        fallback = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person)
                    )
                    
                    // Edit Button
                    IconButton(
                        onClick = { 
                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .background(primaryColor, CircleShape)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Column(modifier = Modifier.padding(start = 16.dp)) {
                    val displayName = user?.businessName?.ifEmpty { user?.name } ?: "Guest User"
                    val displayContact = user?.phone?.ifEmpty { user?.email } ?: "Please log in"
                    
                    Text(
                        text = displayName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor
                    )
                    Text(
                        text = displayContact,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    
                    if (user?.profilePic?.isNotEmpty() == true) {
                        TextButton(
                            onClick = { authViewModel.updateProfilePicture(context, null) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Red)
                            Spacer(Modifier.width(4.dp))
                            Text("Remove Photo", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Manage Store Section
            if (user != null) {
                Text(
                    text = "Manage Store",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = onSurfaceColor
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Temporarily unblocked for testing purposes
                            onPostAd()
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = primaryColor, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("+ Post New Product", color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Test Entry Point for SubscriptionActivity
                Button(
                    onClick = {
                        context.startActivity(Intent(context, SubscriptionActivity::class.java))
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                ) {
                    Icon(Icons.Default.CardMembership, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Subscription & Boost (Coming Soon)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                // Placeholder/Action for Guest Users
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLogout() }, // Logic to take to login screen
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF1E3A1E) else Color(0xFFE8F5E9)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Become a Seller",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Register now to start posting products",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onLogout, // Navigates to Login/Signup
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text("Login / Register")
                        }
                    }
                }
            }

            // My Products Section
            if (user != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Products",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = onSurfaceColor
                    )
                    TextButton(onClick = {
                        val intent = Intent(context, SellerCatalogActivity::class.java).apply {
                            putExtra("USER_ID", user?.uid)
                        }
                        context.startActivity(intent)
                    }) {
                        Text("ALL", color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }

                val myProductsState by marketViewModel.uiState.collectAsState()
                
                if (myProductsState is MarketUiState.Error) {
                    Text(
                        text = (myProductsState as MarketUiState.Error).message,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (myProductsState is MarketUiState.Success) {
                    val myProducts = (myProductsState as MarketUiState.Success).items.filter { it.userId == user?.uid }
                    Log.d("AppwriteDebug", "My Products found: ${myProducts.size}")
                    
                    if (myProducts.isEmpty()) {
                        Text("You haven't posted any products yet.", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        var productToDelete by remember { mutableStateOf<MarketItem?>(null) }
                        
                        if (productToDelete != null) {
                            AlertDialog(
                                onDismissRequest = { productToDelete = null },
                                title = { Text("Delete Product?") },
                                text = { Text("Are you sure you want to delete '${productToDelete?.name}'? This will permanently remove the product and all its images from the marketplace.") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            productToDelete?.let { marketViewModel.deleteProduct(it.id) }
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

                        Column {
                            myProducts.chunked(2).forEach { rowItems ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    rowItems.forEach { product ->
                                        ProductGridItem(
                                            product = product,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                val intent = Intent(context, ProductDetailActivity::class.java)
                                                intent.putExtra("PRODUCT_JSON", com.google.gson.Gson().toJson(product))
                                                context.startActivity(intent)
                                            },
                                            onFavoriteToggle = {}, // Not needed here as it's owner view
                                            isOwner = true,
                                            onEdit = {
                                                val intent = Intent(context, EditProductActivity::class.java).apply {
                                                    putExtra("PRODUCT_ID", product.id)
                                                }
                                                context.startActivity(intent)
                                            },
                                            onDelete = { productToDelete = product }
                                        )
                                    }
                                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
            
            if (BuildConfig.DEBUG && user?.email == "bex3@bexmarket.ng") {
                Button(
                    onClick = { marketViewModel.clearAllProducts() },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("ADMIN: WIPE ALL PRODUCTS", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("https://t.me/BMngsupport")
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.SupportAgent, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Report Issue / Support", fontWeight = FontWeight.Medium)
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("https://t.me/bexmarketngfeedback")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback: If Telegram is not installed, open in browser
                        val webIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/bexmarketngfeedback"))
                        context.startActivity(webIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Feedback, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Community Feedback", fontWeight = FontWeight.Medium)
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("market://details?id=${context.packageName}")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback to browser if Play Store is not installed
                        val webIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                        }
                        context.startActivity(webIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700))
                Spacer(Modifier.width(8.dp))
                Text("Rate BEX MARKET", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MyProductItem(product: MarketItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, maxLines = 1, color = onSurfaceColor)
                Text("₦${product.price}", color = primaryColor, fontSize = 14.sp)
            }
            
            Row {
                IconButton(onClick = {
                    Log.d("EditProduct", "Edit button clicked for product: ${product.id}")
                    val intent = Intent(context, EditProductActivity::class.java).apply {
                        putExtra("PRODUCT_ID", product.id)
                    }
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }
}

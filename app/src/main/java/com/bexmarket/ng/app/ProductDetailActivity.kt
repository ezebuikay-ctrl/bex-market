package com.bexmarket.ng.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.net.URLEncoder

class ProductDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val productJson = intent.getStringExtra("PRODUCT_JSON")
        if (productJson == null) {
            Toast.makeText(this, "Product data missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val product = try {
            Gson().fromJson(productJson, MarketItem::class.java)
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading product", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            AppTheme {
                ProductDetailScreen(
                    product = product,
                    onBack = { finish() },
                    onCall = { 
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${product.sellerPhone}")
                        }
                        startActivity(intent)
                    },
                    onWhatsapp = {
                        openWhatsApp(this, product.sellerWhatsapp ?: "", "Hello, I am interested in your product: ${product.name}")
                    }
                )
            }
        }
    }
}

private fun openWhatsApp(context: android.content.Context, phoneNumber: String, message: String) {
    // 1. Clean Phone Number (Remove non-digits)
    var cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
    
    // 2. Add Nigeria Country Code if it's a local number
    if (cleanNumber.startsWith("0")) {
        cleanNumber = "234" + cleanNumber.substring(1)
    } else if (cleanNumber.length == 10) { // e.g. 803...
        cleanNumber = "234" + cleanNumber
    } else if (cleanNumber.startsWith("+")) {
        cleanNumber = cleanNumber.substring(1)
    }
    
    // 3. Use the robust wa.me format
    val url = "https://wa.me/$cleanNumber?text=${URLEncoder.encode(message, "UTF-8")}"
    
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ProductDetailTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF006400),
            background = Color.White
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductDetailScreen(
    product: MarketItem,
    onBack: () -> Unit,
    onCall: () -> Unit,
    onWhatsapp: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showFullScreen by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    
    val viewModel: MarketViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val user by authViewModel.currentUser.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Fetch seller profile from Users collection
    var sellerProfile by remember { mutableStateOf<UserProfile?>(null) }
    val similarProducts by viewModel.similarProducts.collectAsState()

    LaunchedEffect(product.userId, product.category) {
        if (product.userId.isNotEmpty()) {
            sellerProfile = AppwriteProvider.repository.getSellerProfile(product.userId)
        }
        product.category?.let {
            viewModel.fetchSimilarProducts(it, product.id)
        }
    }

    // Find the latest state of this product to get correct isFavorite status
    val currentProduct = (uiState as? MarketUiState.Success)?.items?.find { it.id == product.id } ?: product

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val allImages = remember(product) {
        val list = mutableListOf<String>()
        // Combine primary image and additional images
        product.imageUrl?.let { if (it.isNotEmpty()) list.add(it) }
        product.imageUrls.forEach { id ->
            val url = AppwriteProvider.repository.getFullImageUrl(id)
            if (!list.contains(url)) list.add(url)
        }
        list
    }
    
    val pagerState = rememberPagerState(pageCount = { allImages.size })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val reportMessage = "I would like to report listing ID: ${product.id}\nProduct: ${product.name}"
                        val telegramUrl = "https://t.me/BMngsupport?text=${Uri.encode(reportMessage)}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl))
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Report Listing",
                            tint = Color.Red
                        )
                    }

                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            val deepLink = "bexmarketnative://product?id=${product.id}"
                            val shareMessage = "Check out this product on BEXMARKETPLACE-NG: ${product.name}\nPrice: ₦${product.price}\n\nView details: $deepLink"
                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share product via"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = primaryColor)
                    }

                    IconButton(onClick = { 
                        user?.let { viewModel.toggleFavorite(it.uid, product.id) }
                    }) {
                        Icon(
                            imageVector = if (currentProduct.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (currentProduct.isFavorite) Color.Red else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = onSurfaceColor
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp, shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onCall,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Call Seller")
                    }
                    Button(
                        onClick = onWhatsapp,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("WhatsApp")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Main Image Pager (Carousel)
            if (allImages.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f) // Square aspect ratio as requested
                        .background(backgroundColor)
                        .clickable {
                            selectedImageUrl = allImages[pagerState.currentPage]
                            showFullScreen = true
                        }
                ) { page ->
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(allImages[page])
                            .crossfade(true)
                            .build(),
                        contentDescription = "${product.name} Image ${page + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person),
                        fallback = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person)
                    )
                }
            }

            // Thumbnail Navigation
            if (allImages.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp, start = 20.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(allImages) { index, imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Thumbnail ${index + 1}",
                            modifier = Modifier
                                .size(60.dp) // 60.dp by 60.dp as requested
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (pagerState.currentPage == index) 3.dp else 1.dp,
                                    color = if (pagerState.currentPage == index) primaryColor else Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedImageUrl = imageUrl
                                    showFullScreen = true
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Content Layout: Title, Price, Description, Seller Info
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = product.name,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = onSurfaceColor
                )
                
                Text(
                    text = "₦${product.price}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.2f))

                Text(
                    text = "Description",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceColor
                )
                
                Text(
                    text = product.description ?: "No description available.",
                    fontSize = 16.sp,
                    color = onSurfaceColor.copy(alpha = 0.7f),
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )

                // Sub-category Specific Details
                if (!product.subCategory.isNullOrEmpty() && product.subCategory != "Others") {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Specifications",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpecRow("Sub-category", product.subCategory)
                        
                        if (!product.brand.isNullOrEmpty()) SpecRow("Brand", product.brand)
                        if (!product.model.isNullOrEmpty()) SpecRow("Model", product.model)
                        
                        if (product.subCategory == "Mobile Phones") {
                            if (!product.storage.isNullOrEmpty()) SpecRow("Storage", product.storage)
                            if (!product.batteryHealth.isNullOrEmpty()) SpecRow("Battery Health", product.batteryHealth)
                        }
                        
                        if (product.subCategory == "Laptops") {
                            if (!product.processor.isNullOrEmpty()) SpecRow("Processor", product.processor)
                            if (!product.ram.isNullOrEmpty()) SpecRow("RAM", product.ram)
                            if (!product.ssd.isNullOrEmpty()) SpecRow("SSD", product.ssd)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = Color.LightGray.copy(alpha = 0.2f))

                // Chat Input Section
                Text(
                    text = "Contact Seller",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceColor
                )
                
                Spacer(Modifier.height(12.dp))

                // Quick Replies Chips
                val quickReplies = listOf(
                    "Make an offer" to "Hello, I would like to make an offer for this product.",
                    "Is this available?" to "Hello, is this available?",
                    "Last price" to "Hello, what is the last price for this?"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickReplies) { (label, fullText) ->
                        SuggestionChip(
                            onClick = { messageText = fullText },
                            label = { Text(label) },
                            shape = RoundedCornerShape(20.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = primaryColor
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Send a message...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    openWhatsApp(context, product.sellerWhatsapp ?: "", messageText)
                                    messageText = ""
                                }
                            },
                            enabled = messageText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (messageText.isNotBlank()) primaryColor else Color.Gray
                            )
                        }
                    },
                    maxLines = 3
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = Color.LightGray.copy(alpha = 0.2f))

                val isOwner = user?.uid == product.userId
                
                if (isOwner) {
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Product?") },
                            text = { Text("Are you sure you want to permanently delete this product and all its images?") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.deleteProduct(product.id) {
                                            showDeleteDialog = false
                                            onBack()
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                ) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(context, EditProductActivity::class.java).apply {
                                    putExtra("PRODUCT_ID", product.id)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Edit")
                        }
                        
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Delete")
                        }
                    }

                    if (!product.isPromoted) {
                        Button(
                            onClick = {
                                if (RewardedAdManager.isAdReady()) {
                                    RewardedAdManager.showAdSequence(
                                        activity = context as Activity,
                                        totalAds = 4,
                                        onComplete = {
                                            viewModel.promoteProduct(product.id) { success ->
                                                if (success) {
                                                    Toast.makeText(context, "Product Boosted to Top!", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        onCancel = { adsShown ->
                                            if (adsShown < 4 && !RewardedAdManager.isAdReady()) {
                                                Toast.makeText(context, "No ad available right now. Please try again later.", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Boost canceled. You watched $adsShown of 4 ads.", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        onProgress = { currentAd ->
                                            Toast.makeText(context, "Playing ad $currentAd of 4...", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    if (RewardedAdManager.isAdLoading) {
                                        Toast.makeText(context, "Ads are loading, please try again in a few seconds...", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "No ad available right now. Please try again later.", Toast.LENGTH_LONG).show()
                                        RewardedAdManager.loadAd(context as Activity)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9300))
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Boost to Top (Watch 4 Ads)", fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = Color.LightGray.copy(alpha = 0.2f))
                }

                // Seller Information Section
                Text(
                    text = "Seller Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceColor
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val intent = Intent(context, SellerCatalogActivity::class.java).apply {
                                putExtra("USER_ID", product.userId)
                            }
                            context.startActivity(intent)
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val profilePicUrl = sellerProfile?.profilePic
                    if (!profilePicUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(profilePicUrl)
                                .build(),
                            contentDescription = "Seller Profile Picture",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray),
                            contentScale = ContentScale.Crop,
                            error = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person),
                            fallback = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = primaryColor.copy(alpha = 0.1f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val initial = when {
                                    !sellerProfile?.username.isNullOrEmpty() -> sellerProfile?.username?.take(1)
                                    !sellerProfile?.businessName.isNullOrEmpty() -> sellerProfile?.businessName?.take(1)
                                    !sellerProfile?.name.isNullOrEmpty() -> sellerProfile?.name?.take(1)
                                    !product.sellerName.isNullOrEmpty() -> product.sellerName?.take(1)
                                    else -> "B"
                                }?.uppercase() ?: "B"

                                Text(
                                    text = initial,
                                    color = primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }
                    
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        val sellerName = when {
                            !sellerProfile?.username.isNullOrEmpty() -> sellerProfile?.username
                            !sellerProfile?.businessName.isNullOrEmpty() -> sellerProfile?.businessName
                            !sellerProfile?.name.isNullOrEmpty() -> sellerProfile?.name
                            !product.sellerName.isNullOrEmpty() && product.sellerName != "Bex Seller" -> product.sellerName
                            else -> "Bex Seller"
                        } ?: "Bex Seller"
                            
                        Text(
                            text = sellerName,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor
                        )
                        Text(
                            text = "${product.locationState}, ${product.locationAxis}",
                            color = onSurfaceColor.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://t.me/BMngsupport")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Report Issue with Listing", fontWeight = FontWeight.Medium)
                }

                if (similarProducts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Similar Products",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor
                    )
                    
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(similarProducts) { similarProduct ->
                            ProductGridItem(
                                product = similarProduct,
                                modifier = Modifier.width(160.dp),
                                onClick = { clickedProduct ->
                                    val intent = Intent(context, ProductDetailActivity::class.java).apply {
                                        putExtra("PRODUCT_JSON", com.google.gson.Gson().toJson(clickedProduct))
                                    }
                                    context.startActivity(intent)
                                },
                                onFavoriteToggle = {
                                    user?.let { viewModel.toggleFavorite(it.uid, similarProduct.id) }
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showFullScreen) {
        Dialog(
            onDismissRequest = { showFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = selectedImageUrl,
                    contentDescription = "Full Screen Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { showFullScreen = false },
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

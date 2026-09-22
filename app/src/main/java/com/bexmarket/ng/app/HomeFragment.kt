package com.bexmarket.ng.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.ExperimentalFoundationApi
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil.compose.AsyncImage
import com.google.android.gms.ads.nativead.NativeAd
import com.google.gson.Gson

class HomeFragment : Fragment() {
    private val viewModel: MarketViewModel by activityViewModels()

    override fun onResume() {
        super.onResume()
        viewModel.refreshProducts()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    HomeScreen(viewModel) { product ->
                        val intent = Intent(requireContext(), ProductDetailActivity::class.java)
                        intent.putExtra("PRODUCT_JSON", Gson().toJson(product))
                        startActivity(intent)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(viewModel: MarketViewModel, onProductClick: (MarketItem) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val promotedItems by viewModel.promotedItems.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val priceRange by viewModel.priceRange.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedSubCategory by viewModel.selectedSubCategory.collectAsState()
    
    var adRefreshTrigger by remember { mutableStateOf(0) }
    
    val categoryHierarchy = mapOf(
        "Electronics" to listOf("Mobile Phones", "Laptops", "Tablets", "Accessories"),
        "Fashion" to listOf("Men's Wear", "Women's Wear", "Shoes", "Jewelry"),
        "Real Estate" to listOf("Rent", "Sale", "Short Let"),
        "Vehicles" to listOf("Cars", "Motorcycles", "Trucks"),
        "Home" to listOf("Furniture", "Kitchen Appliances", "Decor"),
        "Beauty" to listOf("Skincare", "Makeup", "Fragrance"),
        "Services" to listOf("Repair", "Delivery", "Consulting")
    )
    
    val pullToRefreshState = rememberPullToRefreshState()
    
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            adRefreshTrigger++
            viewModel.refreshProducts()
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }
    
    var showMoreCategories by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var isPriceFilterVisible by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    
    val authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val authStatus by authViewModel.authStatus.collectAsState()
    val user by authViewModel.currentUser.collectAsState()

    LaunchedEffect(user) {
        viewModel.fetchProducts(user?.uid)
    }
    
    val context = LocalContext.current

    // AD CONFIGURATION
    val adUnitId = if (BuildConfig.DEBUG) "ca-app-pub-3940256099942544/2247696110" else "ca-app-pub-9972987433387087/4007265985"
    val (nativeAd, adError) = rememberNativeAd(adUnitId = adUnitId, refreshTrigger = adRefreshTrigger)
    
    val darkGreen = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (!isSearchVisible) {
                        Text("BEXMARKETPLACE-NG", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 16.sp)
                    } else {
                        TextField(
                            value = searchQuery,
                            onValueChange = { 
                                viewModel.onSearchQueryChange(it) 
                            },
                            placeholder = { Text("Search products...", color = Color.Gray, fontSize = 14.sp) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black, fontSize = 16.sp),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                cursorColor = Color.Black
                            ),
                            shape = RoundedCornerShape(28.dp),
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                            trailingIcon = {
                                IconButton(onClick = { 
                                    isSearchVisible = false
                                    viewModel.onSearchQueryChange("")
                                }) {
                                    Icon(Icons.Default.Close, "Close Search", tint = Color.Gray)
                                }
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Search
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSearch = {
                                    viewModel.saveSearchQuery(searchQuery)
                                }
                            ),
                            singleLine = true
                        )
                    }
                },
                actions = {
                    if (!isSearchVisible) {
                        IconButton(onClick = { isSearchVisible = true }) {
                            Icon(Icons.Default.Search, "Search", tint = Color.White)
                        }
                        
                        val settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                        val notificationsEnabled by settingsViewModel.isNotificationsEnabled.collectAsState(initial = false)
                        
                        IconButton(onClick = { settingsViewModel.toggleNotifications(!notificationsEnabled) }) {
                            Icon(
                                imageVector = if (notificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = "Notifications",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkGreen,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (authStatus is AuthStatus.Authenticated) {
                        context.startActivity(Intent(context, PostAdActivity::class.java))
                    } else {
                        Toast.makeText(context, "Please sign up or log in to post your products.", Toast.LENGTH_LONG).show()
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    }
                },
                containerColor = Color(0xFFFF9300),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(darkGreen)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        val states = listOf("All Locations") + NigeriaData.statesAndAreas.keys.sorted()
                        
                        Box {
                            Row(
                                modifier = Modifier.clickable { expanded = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(selectedLocation, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                states.forEach { state ->
                                    DropdownMenuItem(
                                        text = { Text(state) },
                                        onClick = {
                                            viewModel.onLocationSelected(state)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    if (promotedItems.isNotEmpty()) {
                        val bannerPagerState = rememberPagerState(pageCount = { promotedItems.size })
                        
                        LaunchedEffect(Unit) {
                            while(true) {
                                kotlinx.coroutines.delay(5000)
                                if (bannerPagerState.pageCount > 0) {
                                    val nextPage = (bannerPagerState.currentPage + 1) % bannerPagerState.pageCount
                                    bannerPagerState.animateScrollToPage(nextPage)
                                }
                            }
                        }

                        Box(modifier = Modifier.padding(16.dp)) {
                            HorizontalPager(
                                state = bannerPagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) { page ->
                                val item = promotedItems[page]
                                Card(
                                    modifier = Modifier.fillMaxSize().clickable { onProductClick(item) },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9300))
                                ) {
                                    Box {
                                        AsyncImage(
                                            model = item.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            alpha = 0.6f
                                        )
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.Bottom
                                        ) {
                                            Text(
                                                "PROMOTED",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp)
                                            )
                                            Text(
                                                item.name,
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1
                                            )
                                            Text(
                                                "₦${item.price}",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(promotedItems.size) { iteration ->
                                    val color = if (bannerPagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(6.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.padding(16.dp).fillMaxWidth().height(90.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9300))
                        ) {
                            Row(modifier = Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Buy affordable products", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(Modifier.height(4.dp))
                                    Button(
                                        onClick = { 
                                            val intent = Intent(context, FilteredProductsActivity::class.java).apply {
                                                putExtra("MAX_PRICE", 10000.0)
                                                putExtra("TITLE", "Products under ₦10,000")
                                            }
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Shop Now", fontSize = 10.sp, color = Color(0xFFFF9300), fontWeight = FontWeight.Bold)
                                    }
                                }
                                Icon(Icons.Default.ShoppingCart, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(40.dp))
                            }
                        }
                    }
                }

                item {
                    Text("Categories", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 16.dp), color = onSurfaceColor)
                    Spacer(Modifier.height(6.dp))
                    
                    val mainCategories = listOf(
                        "All" to Icons.Default.GridView,
                        "Fashion" to Icons.Default.Checkroom,
                        "Electronics" to Icons.Default.Tv,
                        "Phones" to Icons.Default.Smartphone,
                        "Computing" to Icons.Default.Computer,
                        "Beauty" to Icons.Default.Face
                    )
                    
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(mainCategories) { (name, icon) ->
                            CategoryItem(name, icon) { viewModel.onCategorySelected(name) }
                        }
                        item {
                            CategoryItem("More", Icons.Default.MenuOpen) { showMoreCategories = true }
                        }
                    }
                }

                item {
                    val subs = categoryHierarchy[selectedCategory]
                    if (!subs.isNullOrEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedSubCategory == "All",
                                    onClick = { viewModel.onSubCategorySelected("All") },
                                    label = { Text("All $selectedCategory") }
                                )
                            }
                            items(subs) { sub ->
                                FilterChip(
                                    selected = selectedSubCategory == sub,
                                    onClick = { viewModel.onSubCategorySelected(sub) },
                                    label = { Text(sub) }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { isPriceFilterVisible = !isPriceFilterVisible },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Filter by Price", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = onSurfaceColor)
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isPriceFilterVisible) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = darkGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (priceRange != null) {
                            TextButton(onClick = { viewModel.clearPriceFilter() }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(30.dp)) {
                                Text("Reset", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                    
                    if (isPriceFilterVisible) {
                        var sliderRange by remember(priceRange) {
                            mutableStateOf((priceRange?.first?.toFloat() ?: 0f)..(priceRange?.second?.toFloat() ?: 100000f))
                        }
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            ThreeDPriceSlider(
                                value = sliderRange,
                                onValueChange = { sliderRange = it },
                                onValueChangeFinished = {
                                    viewModel.onPriceRangeSelected(sliderRange.start.toDouble(), sliderRange.endInclusive.toDouble())
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = if (selectedCategory == "Favorites") "Your Favorites" else "Popular Products", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = onSurfaceColor)
                        TextButton(onClick = { 
                            viewModel.onCategorySelected("All")
                            viewModel.onLocationSelected("All Locations")
                            viewModel.onSearchQueryChange("")
                        }) { Text("View All", color = darkGreen) }
                    }
                }

                when (val state = uiState) {
                    is MarketUiState.Loading -> {
                        items(3) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                                Box(modifier = Modifier.weight(1f)) { ShimmerProductItem() }
                                Box(modifier = Modifier.weight(1f)) { ShimmerProductItem() }
                            }
                        }
                    }
                    is MarketUiState.Error -> {
                        item {
                            ErrorStateHandler(message = state.message, onRetry = { viewModel.refreshProducts() })
                        }
                    }
                    is MarketUiState.Success -> {
                        val products = state.items
                        
                        val itemsWithAds = mutableListOf<Any>()
                        products.forEachIndexed { index, item ->
                            itemsWithAds.add(item)
                            // Smoothly insert an ad item after every 6 products
                            if ((index + 1) % 6 == 0) {
                                if (nativeAd != null || (nativeAd == null && adError == null)) {
                                    itemsWithAds.add(nativeAd ?: "AD_SLOT")
                                }
                            }
                        }

                        if (itemsWithAds.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        if (priceRange != null) "No products match this budget—try adjusting your price range!" 
                                        else if (selectedCategory == "Favorites") "You haven't favorited any products yet."
                                        else "No products available.", 
                                        color = Color.Gray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            val chunkedItems = itemsWithAds.chunked(2)
                            items(chunkedItems.size) { index ->
                                val rowItems = chunkedItems[index]
                                Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                                    rowItems.forEach { item ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            when (item) {
                                                is MarketItem -> {
                                                    ProductGridItem(
                                                        product = item, 
                                                        modifier = Modifier, 
                                                        onClick = onProductClick, 
                                                        onFavoriteToggle = { user?.let { viewModel.toggleFavorite(it.uid, item.id) } }
                                                    )
                                                }
                                                is NativeAd -> {
                                                    NativeAdGridItem(nativeAd = item)
                                                }
                                                else -> {
                                                    Box(modifier = Modifier.padding(8.dp)) { ShimmerProductItem() }
                                                }
                                            }
                                        }
                                    }
                                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
            
            PullToRefreshContainer(
                state = pullToRefreshState, 
                modifier = Modifier.align(Alignment.TopCenter), 
                containerColor = surfaceColor, 
                contentColor = darkGreen
            )

            if (showMoreCategories) {
                ModalBottomSheet(
                    onDismissRequest = { showMoreCategories = false },
                    sheetState = sheetState,
                    containerColor = surfaceColor
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
                        Text("More Categories", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp), color = onSurfaceColor)
                        val moreCategories = listOf(
                            "Real Estate" to Icons.Default.Home,
                            "Vehicles" to Icons.Default.DirectionsCar,
                            "Home" to Icons.Default.House,
                            "Services" to Icons.Default.Build,
                            "Others" to Icons.Default.MoreHoriz
                        )
                        moreCategories.chunked(3).forEach { rowItems ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                rowItems.forEach { (name, icon) ->
                                    CategoryItem(name, icon) {
                                        viewModel.onCategorySelected(name)
                                        showMoreCategories = false
                                    }
                                }
                                repeat(3 - rowItems.size) { Spacer(modifier = Modifier.size(60.dp)) }
                            }
                        }
                    }
                }
            }

            if (isSearchVisible && searchHistory.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { isSearchVisible = false }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recent Searches", fontWeight = FontWeight.Bold, color = onSurfaceColor)
                                TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                    Text("Clear All", color = Color.Red, fontSize = 12.sp)
                                }
                            }
                            
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                searchHistory.forEach { term ->
                                    SuggestionChip(
                                        onClick = { 
                                            viewModel.onSearchQueryChange(term)
                                            viewModel.saveSearchQuery(term)
                                        },
                                        label = { Text(term) },
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(name: String, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(name, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ProductGridItem(
    product: MarketItem, 
    modifier: Modifier, 
    onClick: (MarketItem) -> Unit,
    onFavoriteToggle: () -> Unit,
    isOwner: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = modifier.padding(8.dp).clickable { onClick(product) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    contentScale = ContentScale.Crop,
                    placeholder = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person),
                    error = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person)
                )
                
                if (!isOwner) {
                    IconButton(onClick = onFavoriteToggle, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                        Icon(imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (product.isFavorite) Color.Red else Color.Gray)
                    }
                } else {
                    Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "Edit", tint = Color.White, modifier = Modifier.size(16.dp)) }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(16.dp)) }
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(product.name, maxLines = 1, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = onSurfaceColor)
                Spacer(Modifier.height(4.dp))
                Text("₦${product.price}", color = primaryColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

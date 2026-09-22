package com.bexmarket.ng.app

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

class PostAdActivity : ComponentActivity() {
    private val viewModel: MarketViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                PostAdScreen(viewModel, authViewModel) { finish() }
            }
        }
    }
}

@Composable
fun PostAdTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF006400), // Dark Green
            background = Color(0xFFF9F9F9) // Off-white
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostAdScreen(
    viewModel: MarketViewModel,
    authViewModel: AuthViewModel,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val user by authViewModel.currentUser.collectAsState()
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    // Hierarchical Category Structure
    val categoryHierarchy = mapOf(
        "Electronics" to listOf("Mobile Phones", "Laptops", "Tablets", "Accessories"),
        "Fashion" to listOf("Men's Wear", "Women's Wear", "Shoes", "Jewelry"),
        "Real Estate" to listOf("Rent", "Sale", "Short Let"),
        "Vehicles" to listOf("Cars", "Motorcycles", "Trucks"),
        "Home" to listOf("Furniture", "Kitchen Appliances", "Decor"),
        "Beauty" to listOf("Skincare", "Makeup", "Fragrance"),
        "Services" to listOf("Repair", "Delivery", "Consulting"),
        "Others" to emptyList<String>()
    )

    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var sellerPhone by remember { mutableStateOf("") }
    var sellerWhatsapp by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }

    // Dynamic Fields State
    var selectedCategory by remember { mutableStateOf(categoryHierarchy.keys.first()) }
    var selectedSubCategory by remember { mutableStateOf("") }
    
    // Sub-category Specific States
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var storage by remember { mutableStateOf("") }
    var batteryHealth by remember { mutableStateOf("") }
    var processor by remember { mutableStateOf("") }
    var ram by remember { mutableStateOf("") }
    var ssd by remember { mutableStateOf("") }

    // Dropdowns Visibility
    var categoryExpanded by remember { mutableStateOf(false) }
    var subCategoryExpanded by remember { mutableStateOf(false) }

    // Reset sub-category when main category changes
    LaunchedEffect(selectedCategory) {
        selectedSubCategory = categoryHierarchy[selectedCategory]?.firstOrNull() ?: ""
    }

    // Dropdowns State
    var stateExpanded by remember { mutableStateOf(false) }
    val nigeriaStates = NigeriaData.statesAndAreas.keys.toList().sorted()
    var selectedState by remember { mutableStateOf(nigeriaStates.firstOrNull() ?: "Lagos") }

    var areaExpanded by remember { mutableStateOf(false) }
    val areas = NigeriaData.statesAndAreas[selectedState] ?: listOf("General")
    var selectedArea by remember { mutableStateOf(areas.firstOrNull() ?: "General") }

    // Pre-fill contact and location details from user profile
    LaunchedEffect(user) {
        user?.let {
            android.util.Log.d("PostAdActivity", "Pre-filling profile data - Phone: ${it.phone}, WhatsApp: ${it.whatsapp}, Location: ${it.locationState}, Axis: ${it.locationAxis}")
            
            // Pre-fill contact details if they are currently empty
            if (sellerPhone.isEmpty() && it.phone.isNotEmpty()) {
                sellerPhone = it.phone
            }
            if (sellerWhatsapp.isEmpty() && it.whatsapp.isNotEmpty()) {
                sellerWhatsapp = it.whatsapp
            }

            // Pre-fill location details
            if (it.locationState.isNotEmpty()) {
                selectedState = it.locationState
            }
            if (it.locationAxis.isNotEmpty()) {
                selectedArea = it.locationAxis
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(2)
    ) { uris ->
        selectedImageUris = uris
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publish Your Product", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor,
                    titleContentColor = onSurfaceColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceColor)
                    .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                    .clickable { 
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUris.isNotEmpty()) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(selectedImageUris.first())
                            .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                            .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person)
                    )
                    
                    if (selectedImageUris.size > 1) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "+${selectedImageUris.size - 1}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Text("Add up to 2 Photos", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            }
            
            if (selectedImageUris.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImageUris) { uri ->
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(uri)
                                .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                            error = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person)
                        )
                    }
                }
            }

            SectionTitle("Product Details")

            HighVisibilityTextField(
                value = title,
                onValueChange = { if (it.length <= 40) title = it },
                label = "Product Title",
                placeholder = "e.g. iPhone 15 Pro Max"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HighVisibilityTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = "Price (₦)",
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    HighVisibilityTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        label = "Category",
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categoryHierarchy.keys.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Sub-category Dropdown
            AnimatedVisibility(
                visible = categoryHierarchy[selectedCategory]?.isNotEmpty() == true,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ExposedDropdownMenuBox(
                    expanded = subCategoryExpanded,
                    onExpandedChange = { subCategoryExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HighVisibilityTextField(
                        value = selectedSubCategory,
                        onValueChange = {},
                        label = "Sub-category",
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = subCategoryExpanded,
                        onDismissRequest = { subCategoryExpanded = false }
                    ) {
                        categoryHierarchy[selectedCategory]?.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub) },
                                onClick = {
                                    selectedSubCategory = sub
                                    subCategoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Dynamic Fields based on Sub-category
            AnimatedVisibility(
                visible = selectedSubCategory == "Mobile Phones",
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HighVisibilityTextField(value = brand, onValueChange = { brand = it }, label = "Brand", modifier = Modifier.weight(1f), placeholder = "e.g. Apple")
                        HighVisibilityTextField(value = model, onValueChange = { model = it }, label = "Model", modifier = Modifier.weight(1f), placeholder = "e.g. iPhone 15")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HighVisibilityTextField(value = storage, onValueChange = { storage = it }, label = "Storage", modifier = Modifier.weight(1f), placeholder = "e.g. 128GB")
                        HighVisibilityTextField(value = batteryHealth, onValueChange = { batteryHealth = it }, label = "Battery Health", modifier = Modifier.weight(1f), placeholder = "e.g. 95%")
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedSubCategory == "Laptops",
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HighVisibilityTextField(value = brand, onValueChange = { brand = it }, label = "Brand", modifier = Modifier.weight(1f), placeholder = "e.g. Dell")
                        HighVisibilityTextField(value = model, onValueChange = { model = it }, label = "Model", modifier = Modifier.weight(1f), placeholder = "e.g. XPS 13")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HighVisibilityTextField(value = processor, onValueChange = { processor = it }, label = "Processor", modifier = Modifier.weight(1f), placeholder = "e.g. Core i7")
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HighVisibilityTextField(value = ram, onValueChange = { ram = it }, label = "RAM", modifier = Modifier.weight(1f), placeholder = "e.g. 16GB")
                            HighVisibilityTextField(value = ssd, onValueChange = { ssd = it }, label = "SSD", modifier = Modifier.weight(1f), placeholder = "e.g. 512GB")
                        }
                    }
                }
            }

            HighVisibilityTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                modifier = Modifier.height(120.dp),
                singleLine = false
            )

            SectionTitle("Location & Contact")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // State Dropdown
                ExposedDropdownMenuBox(
                    expanded = stateExpanded,
                    onExpandedChange = { stateExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    HighVisibilityTextField(
                        value = selectedState,
                        onValueChange = {},
                        label = "State",
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = stateExpanded,
                        onDismissRequest = { stateExpanded = false }
                    ) {
                        nigeriaStates.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = {
                                    selectedState = st
                                    selectedArea = NigeriaData.statesAndAreas[st]?.firstOrNull() ?: "General"
                                    stateExpanded = false
                                }
                            )
                        }
                    }
                }

                // Area Dropdown
                ExposedDropdownMenuBox(
                    expanded = areaExpanded,
                    onExpandedChange = { areaExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    HighVisibilityTextField(
                        value = selectedArea,
                        onValueChange = {},
                        label = "Area / Axis",
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = areaExpanded,
                        onDismissRequest = { areaExpanded = false }
                    ) {
                        areas.forEach { area ->
                            DropdownMenuItem(
                                text = { Text(area) },
                                onClick = {
                                    selectedArea = area
                                    areaExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            HighVisibilityTextField(
                value = sellerPhone,
                onValueChange = { sellerPhone = it },
                label = "Phone Number",
                keyboardType = KeyboardType.Phone
            )

            HighVisibilityTextField(
                value = sellerWhatsapp,
                onValueChange = { sellerWhatsapp = it },
                label = "WhatsApp Number",
                keyboardType = KeyboardType.Phone,
                placeholder = "e.g. +234..."
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (title.isBlank() || price.isBlank() || selectedImageUris.isEmpty()) {
                        Toast.makeText(context, "Please fill required fields and add at least one photo", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    if (selectedImageUris.size > 2) {
                        Toast.makeText(context, "You can only upload a maximum of 2 photos", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    val currentUid = user?.uid
                    if (currentUid.isNullOrEmpty()) {
                        Toast.makeText(context, "You must be logged in to post", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    uploadProductListing(context, user, title, price, description, selectedCategory, selectedSubCategory, brand, model, storage, batteryHealth, processor, ram, ssd, selectedImageUris, selectedState, selectedArea, currentUid, viewModel, onFinish = {
                        isUploading = false
                        onFinish()
                    }, onStart = {
                        isUploading = true
                    })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("POST LISTING", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

private fun uploadProductListing(
    context: android.content.Context,
    user: UserProfile?,
    title: String,
    price: String,
    description: String,
    selectedCategory: String,
    selectedSubCategory: String,
    brand: String,
    model: String,
    storage: String,
    batteryHealth: String,
    processor: String,
    ram: String,
    ssd: String,
    selectedImageUris: List<Uri>,
    selectedState: String,
    selectedArea: String,
    currentUid: String,
    viewModel: MarketViewModel,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    onStart()
    // Clean price input
    val cleanPrice = price.replace("₦", "").replace(",", "").replace(" ", "")
    val parsedPrice = cleanPrice.toDoubleOrNull() ?: 0.0
    
    val product = MarketItem(
        id = "",
        name = title,
        price = parsedPrice,
        description = description,
        category = selectedCategory,
        subCategory = selectedSubCategory,
        brand = if (selectedSubCategory == "Mobile Phones" || selectedSubCategory == "Laptops") brand else "",
        model = if (selectedSubCategory == "Mobile Phones" || selectedSubCategory == "Laptops") model else "",
        storage = if (selectedSubCategory == "Mobile Phones") storage else "",
        batteryHealth = if (selectedSubCategory == "Mobile Phones") batteryHealth else "",
        processor = if (selectedSubCategory == "Laptops") processor else "",
        ram = if (selectedSubCategory == "Laptops") ram else "",
        ssd = if (selectedSubCategory == "Laptops") ssd else "",
        imageUrl = "", 
        sellerName = user?.name ?: "Bex Seller",
        sellerPhone = user?.phone ?: "",
        sellerWhatsapp = user?.whatsapp ?: "",
        sellerProfilePic = user?.profilePic,
        locationState = selectedState,
        locationAxis = selectedArea,
        userId = currentUid
    )
    
    viewModel.addOrUpdateProduct(context, product, selectedImageUris) { success, msg ->
        if (success) {
            Toast.makeText(context, "Product posted!", Toast.LENGTH_SHORT).show()
            onFinish()
        } else {
            Toast.makeText(context, "Error: $msg", Toast.LENGTH_LONG).show()
            onFinish()
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighVisibilityTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color(0xFF1A1A1A) // Deep black/gray
    val labelColor = if (isDark) Color.LightGray else Color(0xFF1A1A1A) // High contrast for light mode
    val placeholderColor = if (isDark) Color.LightGray else Color(0xFF444444) // Clear distinction
    val containerColor = if (isDark) Color(0xFF2D2D2D) else Color(0xFFF0F0F0) // Slight gray for depth
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontWeight = FontWeight.Bold) },
        placeholder = placeholder?.let { { Text(it, color = placeholderColor) } },
        modifier = modifier.fillMaxWidth(),
        readOnly = readOnly,
        singleLine = singleLine,
        trailingIcon = trailingIcon,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Gray,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedPlaceholderColor = placeholderColor,
            unfocusedPlaceholderColor = placeholderColor,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = labelColor,
            disabledLabelColor = labelColor.copy(alpha = 0.5f)
        )
    )
}

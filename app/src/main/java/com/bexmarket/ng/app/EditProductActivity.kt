package com.bexmarket.ng.app

import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import io.appwrite.ID
import io.appwrite.models.InputFile
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class EditProductActivity : ComponentActivity() {
    private val viewModel: MarketViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val productId = intent.getStringExtra("PRODUCT_ID")
        if (productId == null) {
            finish()
            return
        }

        setContent {
            AppTheme {
                EditProductScreen(productId, viewModel, authViewModel) { finish() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    productId: String,
    viewModel: MarketViewModel,
    authViewModel: AuthViewModel,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val databases = AppwriteProvider.databases
    val repository = AppwriteProvider.repository

    val DATABASE_ID = "6a4d8f75003a9cd90f61"
    val PRODUCTS_COLLECTION_ID = "products"

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface

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

    // Form State
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
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

    // Location & Contact
    val nigeriaStates = NigeriaData.statesAndAreas.keys.toList().sorted()
    var selectedState by remember { mutableStateOf(nigeriaStates.firstOrNull() ?: "Lagos") }
    var selectedArea by remember { mutableStateOf("General") }
    var sellerPhone by remember { mutableStateOf("") }
    var sellerWhatsapp by remember { mutableStateOf("") }

    var imageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var localNewImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    
    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }

    // Dropdowns Visibility
    var categoryExpanded by remember { mutableStateOf(false) }
    var subCategoryExpanded by remember { mutableStateOf(false) }
    var stateExpanded by remember { mutableStateOf(false) }
    var areaExpanded by remember { mutableStateOf(false) }

    val areas = NigeriaData.statesAndAreas[selectedState] ?: listOf("General")

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(2)
    ) { uris ->
        localNewImageUris = localNewImageUris + uris
    }

    // Fetch Existing Data
    LaunchedEffect(productId) {
        try {
            Log.d("EditProduct", "Fetching document for ID: $productId")
            val response = databases.getDocument(
                databaseId = DATABASE_ID,
                collectionId = PRODUCTS_COLLECTION_ID,
                documentId = productId
            )
            Log.d("EditProduct", "Retrieved data: ${response.data}")

            title = response.data["productName"] as? String ?: ""
            price = (response.data["price"]?.toString()) ?: ""
            description = response.data["description"] as? String ?: ""
            selectedCategory = response.data["category"] as? String ?: "Others"
            selectedSubCategory = response.data["subCategory"] as? String ?: ""
            
            brand = response.data["brand"] as? String ?: ""
            model = response.data["model"] as? String ?: ""
            storage = response.data["storage"] as? String ?: ""
            batteryHealth = response.data["batteryHealth"] as? String ?: ""
            processor = response.data["processor"] as? String ?: ""
            ram = response.data["ram"] as? String ?: ""
            ssd = response.data["ssd"] as? String ?: ""

            selectedState = response.data["locationState"] as? String ?: (nigeriaStates.firstOrNull() ?: "Lagos")
            selectedArea = response.data["locationAxis"] as? String ?: "General"
            sellerPhone = response.data["sellerPhone"] as? String ?: ""
            sellerWhatsapp = response.data["sellerWhatsapp"] as? String ?: ""
            
            @Suppress("UNCHECKED_CAST")
            imageUrls = response.data["imageUrls"] as? List<String> ?: emptyList()
            
            isLoading = false
        } catch (e: Exception) {
            Log.e("EditProduct", "Error fetching product: ${e.message}")
            isLoading = false
            Toast.makeText(context, "Failed to load product details", Toast.LENGTH_SHORT).show()
            onFinish()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Product", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor,
                    titleContentColor = onSurfaceColor,
                    navigationIconContentColor = onSurfaceColor
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(backgroundColor)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionTitle("Product Images")

                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(imageUrls) { index, id ->
                        Box {
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(context)
                                    .data(repository.getFullImageUrl(id))
                                    .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                    .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                                error = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person)
                            )
                            IconButton(
                                onClick = { imageUrls = imageUrls.filterIndexed { i, _ -> i != index } },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Red, CircleShape).padding(4.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    itemsIndexed(localNewImageUris) { index, uri ->
                        Box {
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(context)
                                    .data(uri)
                                    .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                    .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, primaryColor, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                                error = androidx.compose.ui.res.painterResource(id = R.drawable.ic_person)
                            )
                            IconButton(
                                onClick = { localNewImageUris = localNewImageUris.filterIndexed { i, _ -> i != index } },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Red, CircleShape).padding(4.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(surfaceColor).border(1.dp, Color.Gray, RoundedCornerShape(8.dp)).clickable {
                                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Image", tint = Color.Gray)
                        }
                    }
                }

                SectionTitle("Product Details")

                HighVisibilityTextField(
                    value = title,
                    onValueChange = { if (it.length <= 40) title = it },
                    label = "Product Title"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HighVisibilityTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = "Price (₦)",
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number
                    )

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
                                        selectedSubCategory = categoryHierarchy[cat]?.firstOrNull() ?: ""
                                    }
                                )
                            }
                        }
                    }
                }

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
                                        stateExpanded = false
                                    }
                                )
                            }
                        }
                    }

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
                    keyboardType = KeyboardType.Phone
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (title.isBlank() || price.isBlank()) {
                            Toast.makeText(context, "Title and Price are required", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        val totalImages = imageUrls.size + localNewImageUris.size
                        if (totalImages > 2) {
                            Toast.makeText(context, "Maximum of 2 photos allowed total", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isUpdating = true
                        
                        val cleanPrice = price.replace("₦", "").replace(",", "").replace(" ", "")
                        val parsedPrice = cleanPrice.toDoubleOrNull() ?: 0.0

                        viewModel.updateProduct(
                            context = context,
                            productId = productId,
                            title = title,
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
                            locationState = selectedState,
                            locationAxis = selectedArea,
                            sellerPhone = sellerPhone,
                            sellerWhatsapp = sellerWhatsapp,
                            existingImageIds = imageUrls,
                            newImageUris = localNewImageUris
                        ) { success, msg ->
                            isUpdating = false
                            if (success) {
                                Toast.makeText(context, "Product updated successfully!", Toast.LENGTH_SHORT).show()
                                onFinish()
                            } else {
                                Toast.makeText(context, "Update failed: $msg", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    enabled = !isUpdating
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("UPDATE PRODUCT", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

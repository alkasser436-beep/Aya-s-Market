package com.example.ui.customer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditLocationAlt
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppCurrency
import com.example.data.model.AppLanguage
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.PromoBanner
import com.example.data.model.StoreConfig
import com.example.data.model.UiState
import com.example.data.model.UserAddress
import com.example.localization.AppStrings
import com.example.localization.PriceFormatter
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ProductVisualArtwork

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    language: AppLanguage,
    currency: AppCurrency,
    storeConfig: StoreConfig,
    productsState: UiState<List<Product>>,
    promosState: UiState<List<PromoBanner>>,
    pinnedLocation: UserAddress,
    searchQuery: String,
    selectedCategory: ProductCategory?,
    cart: Map<String, CartItem>,
    isAdminAuthorized: Boolean,
    onSearchChange: (String) -> Unit,
    onCategorySelected: (ProductCategory?) -> Unit,
    onOpenLocationPicker: () -> Unit,
    onOpenBarcodeScanner: () -> Unit,
    onAddToCart: (Product, Int) -> Unit,
    onDecrementCart: (String) -> Unit,
    onSeedCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    val allProducts = (productsState as? UiState.Success)?.data ?: emptyList()
    val filteredProducts = remember(allProducts, searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase()
        allProducts.filter { product ->
            product.active &&
                (selectedCategory == null || product.parsedCategory == selectedCategory) &&
                (q.isBlank() ||
                    product.nameEn.lowercase().contains(q) ||
                    product.nameAr.lowercase().contains(q) ||
                    product.barcode.lowercase().contains(q) ||
                    product.descriptionEn.lowercase().contains(q))
        }
    }

    val promos = (promosState as? UiState.Success)?.data?.filter { it.active } ?: emptyList()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 900.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Delivery Location Banner
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenLocationPicker)
                        .testTag("delivery_location_bar")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = AppStrings.deliverTo(language),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = AppStrings.deliverTo(language),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${pinnedLocation.label} • ${pinnedLocation.addressLine}",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.EditLocationAlt,
                            contentDescription = "Change Location",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Smart Search Bar + Barcode Scanner Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = {
                            Text(
                                text = AppStrings.searchPlaceholder(language),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_products_input")
                    )

                    FilledIconButton(
                        onClick = onOpenBarcodeScanner,
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("barcode_scan_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = AppStrings.scanBarcode(language),
                            tint = Color.White
                        )
                    }
                }
            }

            // Promo Hero Carousel
            item {
                val displayPromos = if (promos.isNotEmpty()) promos else listOf(
                    PromoBanner(
                        id = "default_1",
                        titleEn = "Fresh Produce Daily",
                        titleAr = "خضار وفواكه طازجة يومياً",
                        subtitleEn = "Farm-to-door organic harvest",
                        subtitleAr = "من المزرعة إلى باب بيتك",
                        badgeText = "FRESH",
                        category = ProductCategory.FRUITS_VEG.name,
                        bannerKey = "fresh_produce"
                    ),
                    PromoBanner(
                        id = "default_2",
                        titleEn = "Morning Dairy & Bakery",
                        titleAr = "ألبان ومخبوزات الصباح",
                        subtitleEn = "Warm artisan bread & baladi labneh",
                        subtitleAr = "خبز ساخن ولبنة بلدية طازجة",
                        badgeText = "HOT",
                        category = ProductCategory.DAIRY.name,
                        bannerKey = "dairy_bakery"
                    )
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayPromos, key = { it.id }) { promo ->
                        val drawableRes = if (promo.bannerKey == "dairy_bakery") {
                            R.drawable.img_banner_dairy_bakery
                        } else {
                            R.drawable.img_hero_fresh_produce
                        }
                        Card(
                            modifier = Modifier
                                .width(296.dp)
                                .height(148.dp)
                                .clickable {
                                    val cat = if (promo.category == "ALL") null else ProductCategory.fromRaw(promo.category)
                                    onCategorySelected(cat)
                                },
                            shape = RoundedCornerShape(22.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = painterResource(id = drawableRes),
                                    contentDescription = promo.localizedTitle(language),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(Color(0xD9064E30), Color(0x33064E30))
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp)
                                ) {
                                    if (promo.badgeText.isNotBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondary,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = promo.badgeText,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                    }
                                    Text(
                                        text = promo.localizedTitle(language),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = promo.localizedSubtitle(language),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { onCategorySelected(null) },
                            label = { Text(AppStrings.allCategories(language)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("category_chip_all")
                        )
                    }
                    items(ProductCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onCategorySelected(cat) },
                            label = { Text(AppStrings.categoryLabel(cat, language)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("category_chip_${cat.name.lowercase()}")
                        )
                    }
                }
            }

            // Products Content
            when (productsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        EmptyStateCard(
                            title = if (language == AppLanguage.AR) "تعذر تحميل المنتجات" else "Unable to load catalog",
                            subtitle = productsState.message
                        )
                    }
                }
                is UiState.Success -> {
                    if (filteredProducts.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = if (language == AppLanguage.AR) "لا توجد منتجات مطابقة" else "No matching products",
                                subtitle = if (allProducts.isEmpty() && isAdminAuthorized)
                                    if (language == AppLanguage.AR) "اضغط لتحميل الكتالوج التمهيدي فوراً" else "Tap below to populate the starter supermarket catalog"
                                else
                                    if (language == AppLanguage.AR) "جرّب البحث باسم آخر أو مسح باركود" else "Try another keyword or clear category filters",
                                actionLabel = if (allProducts.isEmpty() && isAdminAuthorized) AppStrings.seedCatalog(language) else null,
                                onAction = if (allProducts.isEmpty() && isAdminAuthorized) onSeedCatalog else null
                            )
                        }
                    } else {
                        val rows = filteredProducts.chunked(columns)
                        items(rows, key = { row -> row.joinToString("_") { it.id } }) { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { product ->
                                    val qtyInCart = cart[product.id]?.quantity ?: 0
                                    ProductGridCard(
                                        product = product,
                                        qtyInCart = qtyInCart,
                                        language = language,
                                        currency = currency,
                                        exchangeRateLbp = storeConfig.exchangeRateLbp,
                                        onCardClick = { selectedProduct = product },
                                        onAdd = { onAddToCart(product, 1) },
                                        onDecrement = { onDecrementCart(product.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(columns - rowItems.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Product Detail ModalBottomSheet
    selectedProduct?.let { product ->
        var modalQty by remember(product.id) { mutableIntStateOf(1) }
        ModalBottomSheet(onDismissRequest = { selectedProduct = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProductVisualArtwork(
                    imageKey = product.imageKey,
                    category = product.parsedCategory,
                    contentDescription = product.localizedName(language),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(20.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = AppStrings.categoryLabel(product.parsedCategory, language),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    if (product.discountPercent > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "-${product.discountPercent}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = product.localizedName(language),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${product.localizedUnit(language)} • ${
                        if (product.stock > 0) "${product.stock} ${if (language == AppLanguage.AR) "متوفر" else "in stock"}"
                        else AppStrings.outOfStock(language)
                    }",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (product.localizedDescription(language).isNotBlank()) {
                    Text(
                        text = product.localizedDescription(language),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (product.barcode.isNotBlank()) {
                    Text(
                        text = "Barcode: ${product.barcode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = PriceFormatter.format(
                                product.effectivePriceUsd,
                                currency,
                                storeConfig.exchangeRateLbp,
                                language
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (product.discountPercent > 0) {
                            Text(
                                text = PriceFormatter.format(
                                    product.priceUsd,
                                    currency,
                                    storeConfig.exchangeRateLbp,
                                    language
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledIconButton(
                            onClick = { if (modalQty > 1) modalQty-- },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            text = "$modalQty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilledIconButton(
                            onClick = { if (modalQty < product.stock) modalQty++ },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Increase")
                        }
                    }
                }

                Button(
                    onClick = {
                        onAddToCart(product, modalQty)
                        selectedProduct = null
                    },
                    enabled = product.stock > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.ShoppingBasket, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (product.stock > 0) AppStrings.addToCart(language) else AppStrings.outOfStock(language),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProductGridCard(
    product: Product,
    qtyInCart: Int,
    language: AppLanguage,
    currency: AppCurrency,
    exchangeRateLbp: Double,
    onCardClick: () -> Unit,
    onAdd: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onCardClick)
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(106.dp)
            ) {
                ProductVisualArtwork(
                    imageKey = product.imageKey,
                    category = product.parsedCategory,
                    contentDescription = product.localizedName(language),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                )
                if (product.discountPercent > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "-${product.discountPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = product.localizedName(language),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )

            Text(
                text = product.localizedUnit(language),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = PriceFormatter.format(
                            product.effectivePriceUsd,
                            currency,
                            exchangeRateLbp,
                            language
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    if (product.discountPercent > 0) {
                        Text(
                            text = PriceFormatter.format(
                                product.priceUsd,
                                currency,
                                exchangeRateLbp,
                                language
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                textDecoration = TextDecoration.LineThrough
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                if (qtyInCart <= 0) {
                    Surface(
                        color = if (product.stock > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(enabled = product.stock > 0, onClick = onAdd)
                            .testTag("add_cart_${product.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = AppStrings.addToCart(language),
                                tint = if (product.stock > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = onDecrement,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Remove,
                                contentDescription = "Decrease",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "$qtyInCart",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = onAdd,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Increase",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

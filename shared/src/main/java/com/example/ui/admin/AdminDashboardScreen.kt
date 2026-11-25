package com.example.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppLanguage
import com.example.data.model.DriverProfile
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.PromoBanner
import com.example.data.model.StoreConfig
import com.example.data.model.UiState
import com.example.localization.AppStrings
import com.example.localization.PriceFormatter
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExternalActions
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.ProductVisualArtwork

@Composable
fun AdminDashboardScreen(
    language: AppLanguage,
    isAdminAuthorized: Boolean,
    currentUserId: String,
    currentUserName: String,
    storeConfig: StoreConfig,
    productsState: UiState<List<Product>>,
    ordersState: UiState<List<Order>>,
    promosState: UiState<List<PromoBanner>>,
    driversState: UiState<List<DriverProfile>>,
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onUpdateRate: (Double, Double, Double) -> Unit,
    onSaveProduct: (Product, Boolean, () -> Unit) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onUpdateOrderStatus: (String, OrderStatus) -> Unit,
    onAssignDriver: (String, DriverProfile) -> Unit,
    onSavePromo: (PromoBanner, () -> Unit) -> Unit,
    onDeletePromo: (String) -> Unit,
    onSaveDriver: (DriverProfile, () -> Unit) -> Unit,
    onSeedCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (!isAdminAuthorized) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AdminPanelSettings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (language == AppLanguage.AR) "لوحة الإدارة للمشرفين فقط" else "Admin Access Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (language == AppLanguage.AR)
                    "يرجى تسجيل الدخول بحساب مالك المتجر المصرح له لإدارة الأسعار والمنتجات والطلبات."
                else
                    "Sign in with an authorized store owner Google account to manage exchange rates, inventory, and orders.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val orders = (ordersState as? UiState.Success)?.data ?: emptyList()
    val products = (productsState as? UiState.Success)?.data ?: emptyList()
    val promos = (promosState as? UiState.Success)?.data ?: emptyList()
    val drivers = (driversState as? UiState.Success)?.data ?: emptyList()

    // Sound notification when new PENDING order arrives
    val pendingCount = orders.count { it.parsedStatus == OrderStatus.PENDING }
    var lastPendingCount by remember { mutableIntStateOf(pendingCount) }
    LaunchedEffect(pendingCount) {
        if (pendingCount > lastPendingCount && soundEnabled) {
            ExternalActions.playNewOrderAlert()
        }
        lastPendingCount = pendingCount
    }

    var rateText by remember(storeConfig.exchangeRateLbp) { mutableStateOf(storeConfig.exchangeRateLbp.toLong().toString()) }
    var feeText by remember(storeConfig.deliveryFeeUsd) { mutableStateOf(storeConfig.deliveryFeeUsd.toString()) }
    var freeThreshText by remember(storeConfig.freeDeliveryThresholdUsd) { mutableStateOf(storeConfig.freeDeliveryThresholdUsd.toString()) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showNewProductDialog by remember { mutableStateOf(false) }
    var showNewPromoDialog by remember { mutableStateOf(false) }
    var showNewDriverDialog by remember { mutableStateOf(false) }

    // Compute Analytics
    val validOrders = orders.filter { it.parsedStatus != OrderStatus.CANCELLED }
    val salesUsd = validOrders.sumOf { it.totalUsd }
    val salesLbp = salesUsd * storeConfig.exchangeRateLbp
    val activeOrdersCount = orders.count {
        it.parsedStatus == OrderStatus.PENDING ||
            it.parsedStatus == OrderStatus.PREPARING ||
            it.parsedStatus == OrderStatus.OUT_FOR_DELIVERY
    }
    val topItems = remember(validOrders, language) {
        validOrders
            .flatMap { it.items }
            .groupBy { it.localizedName(language) }
            .mapValues { entry -> entry.value.sumOf { it.quantity } }
            .entries
            .sortedByDescending { it.value }
            .take(3)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Exchange Rate Quick Control Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CurrencyExchange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.AR) "التحكم الفوري بسعر الصرف" else "Live USD → LBP Exchange Rate",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = onToggleSound) {
                                Icon(
                                    imageVector = if (soundEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                                    contentDescription = "Sound Alerts",
                                    tint = if (soundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = rateText,
                                onValueChange = { rateText = it },
                                label = { Text(AppStrings.exchangeRate(language)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("admin_exchange_rate_input")
                            )
                            OutlinedTextField(
                                value = feeText,
                                onValueChange = { feeText = it },
                                label = { Text(if (language == AppLanguage.AR) "التوصيل $" else "Fee $") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(0.8f)
                            )
                            Button(
                                onClick = {
                                    val rate = rateText.toDoubleOrNull() ?: storeConfig.exchangeRateLbp
                                    val fee = feeText.toDoubleOrNull() ?: storeConfig.deliveryFeeUsd
                                    val thresh = freeThreshText.toDoubleOrNull() ?: storeConfig.freeDeliveryThresholdUsd
                                    onUpdateRate(rate, fee, thresh)
                                },
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("update_rate_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(AppStrings.updateRate(language))
                            }
                        }
                    }
                }
            }

            // Analytics Overview Cards
            item {
                BoxWithConstraints {
                    val isWide = maxWidth >= 600.dp
                    if (isWide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AnalyticsStatCard(
                                title = if (language == AppLanguage.AR) "إجمالي المبيعات" else "Daily Sales",
                                primaryValue = "$${"%.2f".format(salesUsd)}",
                                secondaryValue = PriceFormatter.formatLbpExact(salesLbp, language),
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsStatCard(
                                title = if (language == AppLanguage.AR) "الطلبات النشطة" else "Active Orders",
                                primaryValue = "$activeOrdersCount",
                                secondaryValue = "${pendingCount} ${if (language == AppLanguage.AR) "بانتظار القبول" else "pending"}",
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsStatCard(
                                title = if (language == AppLanguage.AR) "الأكثر مبيعاً" else "Top Selling",
                                primaryValue = topItems.firstOrNull()?.key ?: "—",
                                secondaryValue = topItems.joinToString(" • ") { "${it.key} (${it.value})" }.ifBlank { "No orders yet" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AnalyticsStatCard(
                                    title = if (language == AppLanguage.AR) "المبيعات" else "Sales",
                                    primaryValue = "$${"%.2f".format(salesUsd)}",
                                    secondaryValue = PriceFormatter.formatLbpExact(salesLbp, language),
                                    modifier = Modifier.weight(1f)
                                )
                                AnalyticsStatCard(
                                    title = if (language == AppLanguage.AR) "طلبات نشطة" else "Active Orders",
                                    primaryValue = "$activeOrdersCount",
                                    secondaryValue = "$pendingCount ${if (language == AppLanguage.AR) "جديد" else "pending"}",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (topItems.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp),
                                    tonalElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${if (language == AppLanguage.AR) "الأكثر مبيعاً: " else "Top Items: "}${topItems.joinToString(" • ") { "${it.key} (${it.value})" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Admin Sub-Tabs
            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("${if (language == AppLanguage.AR) "الطلبات" else "Orders"} (${orders.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("${if (language == AppLanguage.AR) "المخزون" else "Inventory"} (${products.size})") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(if (language == AppLanguage.AR) "العروض والسائقون" else "Promos & Drivers") }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    if (orders.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = if (language == AppLanguage.AR) "لا توجد طلبات واردة" else "No incoming orders yet",
                                subtitle = if (language == AppLanguage.AR) "ستظهر الطلبات الجديدة هنا مع تنبيه صوتي" else "Live customer orders will appear here with sound alerts"
                            )
                        }
                    } else {
                        items(orders, key = { it.id }) { order ->
                            AdminOrderCard(
                                order = order,
                                language = language,
                                drivers = drivers,
                                onAccept = { onUpdateOrderStatus(order.id, OrderStatus.PREPARING) },
                                onReject = { onUpdateOrderStatus(order.id, OrderStatus.CANCELLED) },
                                onAssignDriver = { drv -> onAssignDriver(order.id, drv) },
                                onPrintInvoice = { ExternalActions.printOrderInvoice(context, order, language) }
                            )
                        }
                    }
                }
                1 -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showNewProductDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_add_product_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (language == AppLanguage.AR) "إضافة منتج" else "Add Product")
                            }
                            OutlinedButton(
                                onClick = onSeedCatalog,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(AppStrings.seedCatalog(language))
                            }
                        }
                    }
                    items(products, key = { it.id }) { product ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProductVisualArtwork(
                                    imageKey = product.imageKey,
                                    category = product.parsedCategory,
                                    contentDescription = product.localizedName(language),
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.localizedName(language), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "$${"%.2f".format(product.priceUsd)} • ${PriceFormatter.formatLbpExact(product.priceUsd * storeConfig.exchangeRateLbp, language)} • Stock: ${product.stock}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { editingProduct = product }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { onDeleteProduct(product.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showNewPromoDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(if (language == AppLanguage.AR) "إضافة عرض" else "Add Promo")
                            }
                            OutlinedButton(
                                onClick = { showNewDriverDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.LocalShipping, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(if (language == AppLanguage.AR) "إضافة سائق" else "Add Driver")
                            }
                        }
                    }
                    items(promos, key = { it.id }) { promo ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(promo.localizedTitle(language), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(promo.localizedSubtitle(language), style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { onDeletePromo(promo.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete promo", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    items(drivers, key = { it.id }) { driver ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(driver.localizedName(language), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${driver.phone} • ${driver.vehicleLabel}", style = MaterialTheme.typography.bodySmall)
                                }
                                Surface(
                                    color = if (driver.available) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = if (driver.available) {
                                            if (language == AppLanguage.AR) "متاح" else "Available"
                                        } else {
                                            if (language == AppLanguage.AR) "مشغول" else "Offline"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(60.dp)) }
        }

        if (selectedTab == 1) {
            FloatingActionButton(
                onClick = { showNewProductDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Product", tint = Color.White)
            }
        }
    }

    if (showNewProductDialog || editingProduct != null) {
        ProductEditorDialog(
            language = language,
            initial = editingProduct,
            onDismiss = {
                showNewProductDialog = false
                editingProduct = null
            },
            onSave = { product, isNew ->
                onSaveProduct(product, isNew) {
                    showNewProductDialog = false
                    editingProduct = null
                }
            }
        )
    }

    if (showNewPromoDialog) {
        PromoEditorDialog(
            language = language,
            onDismiss = { showNewPromoDialog = false },
            onSave = { promo ->
                onSavePromo(promo) { showNewPromoDialog = false }
            }
        )
    }

    if (showNewDriverDialog) {
        DriverEditorDialog(
            language = language,
            defaultUid = currentUserId,
            defaultName = currentUserName,
            onDismiss = { showNewDriverDialog = false },
            onSave = { driver ->
                onSaveDriver(driver) { showNewDriverDialog = false }
            }
        )
    }
}

@Composable
private fun AnalyticsStatCard(
    title: String,
    primaryValue: String,
    secondaryValue: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(primaryValue, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(secondaryValue, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AdminOrderCard(
    order: Order,
    language: AppLanguage,
    drivers: List<DriverProfile>,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onAssignDriver: (DriverProfile) -> Unit,
    onPrintInvoice: () -> Unit
) {
    var showDriversMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("#${order.id.take(8).uppercase()} • ${order.customerName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${order.customerPhone} • ${order.deliveryAddress}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OrderStatusBadge(status = order.parsedStatus, language = language)
            }

            Text(
                text = order.items.joinToString(", ") { "${it.quantity}x ${it.localizedName(language)}" },
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${"%.2f".format(order.totalUsd)} (${PriceFormatter.formatLbpExact(order.totalLbp, language)})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedButton(onClick = onPrintInvoice, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(AppStrings.printInvoice(language))
                }
            }

            if (order.driverName.isNotBlank()) {
                Text(
                    text = "${if (language == AppLanguage.AR) "السائق المعين:" else "Assigned Driver:"} ${order.driverName}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (order.parsedStatus == OrderStatus.PENDING) {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (language == AppLanguage.AR) "قبول" else "Accept")
                    }
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Rounded.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (language == AppLanguage.AR) "رفض" else "Reject")
                    }
                }

                if (order.parsedStatus == OrderStatus.PENDING || order.parsedStatus == OrderStatus.PREPARING) {
                    Box(modifier = Modifier.weight(1.2f)) {
                        Button(
                            onClick = { showDriversMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Rounded.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (language == AppLanguage.AR) "تعيين سائق" else "Assign Driver")
                        }
                        DropdownMenu(
                            expanded = showDriversMenu,
                            onDismissRequest = { showDriversMenu = false }
                        ) {
                            if (drivers.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(if (language == AppLanguage.AR) "أضف سائقاً أولاً" else "Add a driver in Promos & Drivers tab") },
                                    onClick = { showDriversMenu = false }
                                )
                            } else {
                                drivers.forEach { drv ->
                                    DropdownMenuItem(
                                        text = { Text("${drv.localizedName(language)} (${drv.phone})") },
                                        onClick = {
                                            showDriversMenu = false
                                            onAssignDriver(drv)
                                        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductEditorDialog(
    language: AppLanguage,
    initial: Product?,
    onDismiss: () -> Unit,
    onSave: (Product, Boolean) -> Unit
) {
    val isNew = initial == null
    var nameEn by remember { mutableStateOf(initial?.nameEn ?: "") }
    var nameAr by remember { mutableStateOf(initial?.nameAr ?: "") }
    var descEn by remember { mutableStateOf(initial?.descriptionEn ?: "") }
    var descAr by remember { mutableStateOf(initial?.descriptionAr ?: "") }
    var priceUsd by remember { mutableStateOf(initial?.priceUsd?.toString() ?: "2.50") }
    var discount by remember { mutableStateOf(initial?.discountPercent?.toString() ?: "0") }
    var stock by remember { mutableStateOf(initial?.stock?.toString() ?: "25") }
    var unitEn by remember { mutableStateOf(initial?.unitEn ?: "1 kg") }
    var unitAr by remember { mutableStateOf(initial?.unitAr ?: "١ كغ") }
    var barcode by remember { mutableStateOf(initial?.barcode ?: "") }
    var imageKey by remember { mutableStateOf(initial?.imageKey ?: "") }
    var category by remember { mutableStateOf(initial?.parsedCategory ?: ProductCategory.FRUITS_VEG) }
    var active by remember { mutableStateOf(initial?.active ?: true) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageKey = uri.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isNew) {
                        if (language == AppLanguage.AR) "منتج جديد" else "New Product"
                    } else {
                        if (language == AppLanguage.AR) "تعديل المنتج" else "Edit Product"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("Name (English)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nameAr, onValueChange = { nameAr = it }, label = { Text("الاسم (عربي)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = priceUsd, onValueChange = { priceUsd = it }, label = { Text("USD Price") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("Disc %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(0.8f))
                    OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(0.8f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = unitEn, onValueChange = { unitEn = it }, label = { Text("Unit EN") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = unitAr, onValueChange = { unitAr = it }, label = { Text("الوحدة AR") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = descEn, onValueChange = { descEn = it }, label = { Text("Description EN") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = descAr, onValueChange = { descAr = it }, label = { Text("الوصف AR") }, modifier = Modifier.fillMaxWidth())

                OutlinedButton(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (language == AppLanguage.AR) "اختيار صورة من الجهاز" else "Pick Product Photo")
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProductCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(AppStrings.categoryLabel(cat, language)) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(if (language == AppLanguage.AR) "متاح في المتجر" else "Active in Store")
                    Switch(checked = active, onCheckedChange = { active = it })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(AppStrings.back(language))
                    }
                    Button(
                        onClick = {
                            onSave(
                                Product(
                                    id = initial?.id ?: "",
                                    nameEn = nameEn,
                                    nameAr = nameAr.ifBlank { nameEn },
                                    descriptionEn = descEn,
                                    descriptionAr = descAr,
                                    category = category.name,
                                    priceUsd = priceUsd.toDoubleOrNull() ?: 1.0,
                                    discountPercent = (discount.toLongOrNull() ?: 0L).coerceIn(0L, 90L),
                                    stock = (stock.toLongOrNull() ?: 0L).coerceAtLeast(0L),
                                    unitEn = unitEn,
                                    unitAr = unitAr,
                                    barcode = barcode,
                                    imageKey = imageKey,
                                    active = active,
                                    createdAt = initial?.createdAt
                                ),
                                isNew
                            )
                        },
                        enabled = nameEn.isNotBlank() && (priceUsd.toDoubleOrNull() ?: 0.0) > 0.0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (language == AppLanguage.AR) "حفظ" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun PromoEditorDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (PromoBanner) -> Unit
) {
    var titleEn by remember { mutableStateOf("") }
    var titleAr by remember { mutableStateOf("") }
    var subtitleEn by remember { mutableStateOf("") }
    var badge by remember { mutableStateOf("20% OFF") }
    var bannerKey by remember { mutableStateOf("fresh_produce") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (language == AppLanguage.AR) "بانر ترويجي جديد" else "New Promo Banner", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = titleEn, onValueChange = { titleEn = it }, label = { Text("Title EN") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = titleAr, onValueChange = { titleAr = it }, label = { Text("العنوان AR") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subtitleEn, onValueChange = { subtitleEn = it }, label = { Text("Subtitle EN") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = badge, onValueChange = { badge = it }, label = { Text("Badge (e.g. 20% OFF)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = bannerKey == "fresh_produce", onClick = { bannerKey = "fresh_produce" }, label = { Text("Fresh Produce") })
                    FilterChip(selected = bannerKey == "dairy_bakery", onClick = { bannerKey = "dairy_bakery" }, label = { Text("Dairy & Bakery") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(AppStrings.back(language)) }
                    Button(
                        onClick = {
                            onSave(
                                PromoBanner(
                                    titleEn = titleEn,
                                    titleAr = titleAr.ifBlank { titleEn },
                                    subtitleEn = subtitleEn,
                                    subtitleAr = subtitleEn,
                                    badgeText = badge,
                                    category = "ALL",
                                    bannerKey = bannerKey,
                                    active = true
                                )
                            )
                        },
                        enabled = titleEn.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text(if (language == AppLanguage.AR) "حفظ" else "Save") }
                }
            }
        }
    }
}

@Composable
private fun DriverEditorDialog(
    language: AppLanguage,
    defaultUid: String,
    defaultName: String,
    onDismiss: () -> Unit,
    onSave: (DriverProfile) -> Unit
) {
    var uid by remember { mutableStateOf(defaultUid) }
    var nameEn by remember { mutableStateOf(defaultName.ifBlank { "Aya Driver" }) }
    var nameAr by remember { mutableStateOf(defaultName.ifBlank { "سائق أيا" }) }
    var phone by remember { mutableStateOf("+961 70 888 436") }
    var vehicle by remember { mutableStateOf("Delivery Scooter #1") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (language == AppLanguage.AR) "تسجيل سائق توصيل" else "Register Delivery Driver", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = uid, onValueChange = { uid = it }, label = { Text("Driver Auth UID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("Name EN") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nameAr, onValueChange = { nameAr = it }, label = { Text("الاسم AR") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = vehicle, onValueChange = { vehicle = it }, label = { Text("Vehicle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(AppStrings.back(language)) }
                    Button(
                        onClick = {
                            onSave(
                                DriverProfile(
                                    id = uid.trim(),
                                    uid = uid.trim(),
                                    nameEn = nameEn,
                                    nameAr = nameAr.ifBlank { nameEn },
                                    phone = phone,
                                    vehicleLabel = vehicle,
                                    available = true
                                )
                            )
                        },
                        enabled = uid.isNotBlank() && nameEn.isNotBlank() && phone.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text(if (language == AppLanguage.AR) "حفظ" else "Save") }
                }
            }
        }
    }
}

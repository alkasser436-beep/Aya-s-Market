package com.example.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditLocationAlt
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppCurrency
import com.example.data.model.AppLanguage
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.StoreConfig
import com.example.data.model.UserAddress
import com.example.localization.AppStrings
import com.example.localization.PriceFormatter
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ProductVisualArtwork

@Composable
fun CustomerCartCheckoutScreen(
    language: AppLanguage,
    currency: AppCurrency,
    storeConfig: StoreConfig,
    cart: Map<String, CartItem>,
    pinnedLocation: UserAddress,
    savedAddresses: List<UserAddress>,
    defaultCustomerName: String,
    isBusy: Boolean,
    onAddToCart: (Product, Int) -> Unit,
    onDecrementCart: (String) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onOpenLocationPicker: () -> Unit,
    onSelectSavedAddress: (UserAddress) -> Unit,
    onBrowseMarket: () -> Unit,
    onPlaceOrder: (
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        deliveryNotes: String,
        latitude: Double,
        longitude: Double,
        paymentMethod: PaymentMethod
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemsList = cart.values.toList()
    var step by remember { mutableIntStateOf(1) }

    var customerName by remember(defaultCustomerName) { mutableStateOf(defaultCustomerName.ifBlank { "Aya Shopper" }) }
    var customerPhone by remember(pinnedLocation.phone) { mutableStateOf(pinnedLocation.phone.ifBlank { "+961 70 000 000" }) }
    var deliveryAddress by remember(pinnedLocation.addressLine, pinnedLocation.city) {
        mutableStateOf("${pinnedLocation.addressLine}, ${pinnedLocation.city}")
    }
    var deliveryNotes by remember(pinnedLocation.notes) { mutableStateOf(pinnedLocation.notes) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH_ON_DELIVERY) }

    if (itemsList.isEmpty()) {
        EmptyStateCard(
            title = AppStrings.emptyCartTitle(language),
            subtitle = AppStrings.emptyCartSubtitle(language),
            actionLabel = AppStrings.browseMarket(language),
            onAction = onBrowseMarket,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    val subtotalUsd = itemsList.sumOf { it.lineTotalUsd }
    val deliveryFeeUsd = if (subtotalUsd >= storeConfig.freeDeliveryThresholdUsd) 0.0 else storeConfig.deliveryFeeUsd
    val totalUsd = subtotalUsd + deliveryFeeUsd

    Column(modifier = modifier.fillMaxSize()) {
        // 3-Step Indicator Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                1 to AppStrings.checkoutStep1(language),
                2 to AppStrings.checkoutStep2(language),
                3 to AppStrings.checkoutStep3(language)
            ).forEach { (idx, label) ->
                val active = step >= idx
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { if (idx <= step) step = idx },
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (step) {
                1 -> {
                    items(itemsList, key = { it.product.id }) { item ->
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
                                    imageKey = item.product.imageKey,
                                    category = item.product.parsedCategory,
                                    contentDescription = item.product.localizedName(language),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.product.localizedName(language),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = PriceFormatter.format(
                                            item.lineTotalUsd,
                                            currency,
                                            storeConfig.exchangeRateLbp,
                                            language
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onDecrementCart(item.product.id) }) {
                                        Icon(Icons.Rounded.Remove, contentDescription = "Decrease")
                                    }
                                    Text("${item.quantity}", fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { onAddToCart(item.product, 1) }) {
                                        Icon(Icons.Rounded.Add, contentDescription = "Increase")
                                    }
                                    IconButton(onClick = { onRemoveFromCart(item.product.id) }) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                    Text(
                                        text = if (language == AppLanguage.AR) "موقع التوصيل على الخريطة" else "Pinned GPS Location",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    OutlinedButton(onClick = onOpenLocationPicker) {
                                        Icon(Icons.Rounded.EditLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (language == AppLanguage.AR) "تعديل الخريطة" else "Pin Map")
                                    }
                                }
                                Text(
                                    text = "${pinnedLocation.label} (${"%.4f".format(pinnedLocation.latitude)}, ${"%.4f".format(pinnedLocation.longitude)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (savedAddresses.isNotEmpty()) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(savedAddresses, key = { it.id }) { addr ->
                                            AssistChip(
                                                onClick = {
                                                    onSelectSavedAddress(addr)
                                                    deliveryAddress = "${addr.addressLine}, ${addr.city}"
                                                    customerPhone = addr.phone
                                                },
                                                label = { Text(addr.label) }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = customerName,
                                    onValueChange = { customerName = it },
                                    label = { Text(if (language == AppLanguage.AR) "الاسم الكامل" else "Full Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = customerPhone,
                                    onValueChange = { customerPhone = it },
                                    label = { Text(if (language == AppLanguage.AR) "رقم الهاتف" else "Phone Number") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = deliveryAddress,
                                    onValueChange = { deliveryAddress = it },
                                    label = { Text(if (language == AppLanguage.AR) "العنوان التفصيلي" else "Detailed Address") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = deliveryNotes,
                                    onValueChange = { deliveryNotes = it },
                                    label = { Text(if (language == AppLanguage.AR) "ملاحظات للسائق (اختياري)" else "Delivery Notes (Optional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                3 -> {
                    item {
                        Text(
                            text = if (language == AppLanguage.AR) "اختر طريقة الدفع" else "Select Payment Method",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(PaymentMethod.entries) { method ->
                        val icon = when (method) {
                            PaymentMethod.CASH_ON_DELIVERY -> Icons.Rounded.Payments
                            PaymentMethod.WHISH_MONEY -> Icons.Rounded.AccountBalanceWallet
                            PaymentMethod.CARD -> Icons.Rounded.CreditCard
                        }
                        PaymentMethodCard(
                            label = AppStrings.paymentLabel(method, language),
                            icon = icon,
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            modifier = Modifier.testTag("payment_${method.name.lowercase()}")
                        )
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.AR) "ملخص الطلب" else "Order Summary",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$customerName • $customerPhone",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = deliveryAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${itemsList.sumOf { it.quantity }} ${if (language == AppLanguage.AR) "منتجات" else "items"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Totals + Navigation Controls
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(AppStrings.subtotal(language), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        PriceFormatter.format(subtotalUsd, currency, storeConfig.exchangeRateLbp, language),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(AppStrings.deliveryFee(language), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (deliveryFeeUsd == 0.0) AppStrings.free(language)
                        else PriceFormatter.format(deliveryFeeUsd, currency, storeConfig.exchangeRateLbp, language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (deliveryFeeUsd == 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(AppStrings.total(language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        PriceFormatter.format(totalUsd, currency, storeConfig.exchangeRateLbp, language),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (step > 1) {
                        OutlinedButton(
                            onClick = { step-- },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(AppStrings.back(language))
                        }
                    }
                    Button(
                        onClick = {
                            if (step < 3) {
                                step++
                            } else {
                                onPlaceOrder(
                                    customerName,
                                    customerPhone,
                                    deliveryAddress,
                                    deliveryNotes,
                                    pinnedLocation.latitude,
                                    pinnedLocation.longitude,
                                    paymentMethod
                                )
                            }
                        },
                        enabled = !isBusy && (step != 2 || (customerName.isNotBlank() && customerPhone.isNotBlank() && deliveryAddress.isNotBlank())),
                        modifier = Modifier
                            .weight(2f)
                            .height(50.dp)
                            .testTag("checkout_primary_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (step < 3) AppStrings.next(language) else AppStrings.placeOrder(language),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            if (selected) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

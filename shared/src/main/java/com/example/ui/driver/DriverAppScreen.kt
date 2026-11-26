package com.example.ui.driver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AppCurrency
import com.example.data.model.AppLanguage
import com.example.data.model.DriverProfile
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.UiState
import com.example.localization.AppStrings
import com.example.localization.PriceFormatter
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ExternalActions
import com.example.ui.components.OrderStatusBadge

@Composable
fun DriverAppScreen(
    language: AppLanguage,
    currentUserId: String,
    currentUserEmail: String = "",
    driverOrdersState: UiState<List<Order>>,
    driversState: UiState<List<DriverProfile>>,
    onClaimDriverUid: (String) -> Unit = {},
    onToggleAvailability: (String, Boolean) -> Unit,
    onMarkPickedUp: (String) -> Unit,
    onConfirmDelivered: (String) -> Unit,
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val normalizedEmail = currentUserEmail.trim().lowercase()
    val myDriverProfile = (driversState as? UiState.Success)?.data
        ?.firstOrNull {
            it.uid == currentUserId ||
                it.id == currentUserId ||
                (normalizedEmail.isNotBlank() && it.email.equals(normalizedEmail, ignoreCase = true))
        }

    LaunchedEffect(myDriverProfile?.id, myDriverProfile?.uid) {
        if (myDriverProfile != null && myDriverProfile.uid.isBlank()) {
            onClaimDriverUid(myDriverProfile.id)
        }
    }

    if (driversState is UiState.Loading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (driversState is UiState.Error) {
        EmptyStateCard(
            title = if (language == AppLanguage.AR) "تعذر التحقق من حساب السائق" else "Unable to verify driver account",
            subtitle = driversState.message,
            actionLabel = AppStrings.signOut(language),
            onAction = onSignOut,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    if (driversState is UiState.Success && myDriverProfile == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Text(
                        text = if (language == AppLanguage.AR) "بانتظار إضافة الإدارة" else "Driver Access Restricted",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (language == AppLanguage.AR)
                            "تطبيق التوصيل مخصص فقط للسائقين الذين تضيفهم الإدارة من لوحة التحكم. اطلب من مالك المتجر إضافة بريدك الإلكتروني التالي:"
                        else
                            "Aya Driver is locked to drivers added by the Store Admin. Ask the store owner to add your Google email below:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    val identityToCopy = currentUserEmail.ifBlank { currentUserId }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.AR) "بريد السائق:" else "Driver Google Email:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = identityToCopy,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(onClick = { clipboard.setText(AnnotatedString(identityToCopy)) }) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy Driver Email"
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("driver_unauthorized_signout"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.AR) "تبديل الحساب / تسجيل الخروج" else "Switch Account / Sign Out"
                        )
                    }
                }
            }
        }
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        // High-contrast road header card
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (language == AppLanguage.AR) "تطبيق السائق" else "Driver Dispatch",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = myDriverProfile?.localizedName(language) ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (myDriverProfile != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (myDriverProfile.available) {
                                if (language == AppLanguage.AR) "متاح" else "Online"
                            } else {
                                if (language == AppLanguage.AR) "متوقف" else "Offline"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = myDriverProfile.available,
                            onCheckedChange = { onToggleAvailability(myDriverProfile.id, it) },
                            modifier = Modifier.testTag("driver_availability_switch")
                        )
                    }
                }
            }
        }

        when (driverOrdersState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                EmptyStateCard(
                    title = if (language == AppLanguage.AR) "تعذر تحميل مهام التوصيل" else "Unable to load driver tasks",
                    subtitle = driverOrdersState.message,
                    modifier = Modifier.fillMaxSize()
                )
            }
            is UiState.Success -> {
                val orders = driverOrdersState.data
                if (orders.isEmpty()) {
                    EmptyStateCard(
                        title = if (language == AppLanguage.AR) "لا توجد طلبات مسندة حالياً" else "No assigned deliveries",
                        subtitle = if (language == AppLanguage.AR)
                            "عند قيام الإدارة بتعيين طلب لك، سيظهر هنا فوراً بأزرار واضحة"
                        else
                            "Orders assigned to you from Admin Dashboard appear here immediately",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(orders, key = { it.id }) { order ->
                            DriverDeliveryTaskCard(
                                order = order,
                                language = language,
                                onCallCustomer = { ExternalActions.dialPhone(context, order.customerPhone) },
                                onWhatsAppCustomer = {
                                    ExternalActions.openWhatsApp(
                                        context,
                                        order.customerPhone,
                                        "Hello ${order.customerName}, I am on my way with your Aya Market order #${order.id.take(8)}."
                                    )
                                },
                                onOpenMaps = {
                                    ExternalActions.openInGoogleMaps(
                                        context,
                                        order.latitude,
                                        order.longitude,
                                        order.customerName
                                    )
                                },
                                onOpenWaze = {
                                    ExternalActions.openInWaze(context, order.latitude, order.longitude)
                                },
                                onMarkPickedUp = { onMarkPickedUp(order.id) },
                                onConfirmDelivered = { onConfirmDelivered(order.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverDeliveryTaskCard(
    order: Order,
    language: AppLanguage,
    onCallCustomer: () -> Unit,
    onWhatsAppCustomer: () -> Unit,
    onOpenMaps: () -> Unit,
    onOpenWaze: () -> Unit,
    onMarkPickedUp: () -> Unit,
    onConfirmDelivered: () -> Unit
) {
    val amountInChosenCurrency = when (order.parsedCurrency) {
        AppCurrency.USD -> PriceFormatter.format(order.totalUsd, AppCurrency.USD, order.exchangeRateAtOrder, language)
        AppCurrency.LBP -> PriceFormatter.formatLbpExact(order.totalLbp, language)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${order.id.take(8).uppercase()} • ${order.customerName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OrderStatusBadge(status = order.parsedStatus, language = language)
            }

            // Large High-Contrast Collection Amount Box
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.AR) "المبلغ المطلوب تحصيله (${order.currency})" else "Amount to Collect (${order.currency})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = AppStrings.paymentLabel(order.parsedPaymentMethod, language),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Text(
                        text = amountInChosenCurrency,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Text(
                text = order.deliveryAddress,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (order.deliveryNotes.isNotBlank()) {
                Text(
                    text = "${if (language == AppLanguage.AR) "ملاحظات:" else "Notes:"} ${order.deliveryNotes}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                order.items.forEach { item ->
                    Text(
                        text = "• ${item.quantity}x ${item.localizedName(language)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            HorizontalDivider()

            // One-Tap Call & WhatsApp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onCallCustomer,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(order.customerPhone, maxLines = 1)
                }
                FilledTonalButton(
                    onClick = onWhatsAppCustomer,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(AppStrings.whatsappDriver(language))
                }
            }

            // One-Tap Navigation (Google Maps & Waze)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenMaps,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("driver_maps_button_${order.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                ) {
                    Icon(Icons.Rounded.Map, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(AppStrings.openMaps(language), style = MaterialTheme.typography.titleSmall)
                }
                Button(
                    onClick = onOpenWaze,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("driver_waze_button_${order.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0099FF))
                ) {
                    Icon(Icons.Rounded.Directions, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(AppStrings.openWaze(language), style = MaterialTheme.typography.titleSmall)
                }
            }

            // Large On-The-Road Status Action Button
            when (order.parsedStatus) {
                OrderStatus.PREPARING -> {
                    Button(
                        onClick = onMarkPickedUp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("driver_pickup_button_${order.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Rounded.LocalShipping, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(AppStrings.markPickedUp(language), style = MaterialTheme.typography.titleMedium)
                    }
                }
                OrderStatus.OUT_FOR_DELIVERY -> {
                    Button(
                        onClick = onConfirmDelivered,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("driver_deliver_button_${order.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(AppStrings.confirmDelivered(language), style = MaterialTheme.typography.titleMedium)
                    }
                }
                else -> Unit
            }
        }
    }
}

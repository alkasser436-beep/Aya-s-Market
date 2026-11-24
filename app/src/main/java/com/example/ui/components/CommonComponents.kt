package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EggAlt
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AppCurrency
import com.example.data.model.AppLanguage
import com.example.data.model.AppRole
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.ProductCategory
import com.example.localization.AppStrings
import com.example.localization.PriceFormatter
import java.net.URLEncoder

@Composable
fun AyaHeaderBar(
    language: AppLanguage,
    currency: AppCurrency,
    appRole: AppRole,
    exchangeRateLbp: Double,
    onToggleLanguage: () -> Unit,
    onToggleCurrency: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = Color(0xFFFBFBFB),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.aya_market_icon),
                        contentDescription = AppStrings.appTitleForRole(appRole, language),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = AppStrings.appTitleForRole(appRole, language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (appRole) {
                                AppRole.DRIVER -> Color(0xFFE65100)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (appRole != AppRole.CUSTOMER) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = if (appRole == AppRole.ADMIN) Icons.Rounded.AdminPanelSettings else Icons.Rounded.LocalShipping,
                                contentDescription = null,
                                tint = if (appRole == AppRole.DRIVER) Color(0xFFE65100) else Color(0xFF064E30),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "1 USD = ${PriceFormatter.formatLbpExact(exchangeRateLbp, language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onToggleLanguage)
                        .testTag("header_language_switcher")
                ) {
                    Text(
                        text = if (language == AppLanguage.EN) "عربي" else "EN",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }

                if (appRole == AppRole.CUSTOMER) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onToggleCurrency)
                            .testTag("header_currency_switcher")
                    ) {
                        Text(
                            text = if (currency == AppCurrency.USD) "$ USD" else "LBP ل.ل",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = AppStrings.signOut(language),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProductVisualArtwork(
    imageKey: String,
    category: ProductCategory,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (imageKey.startsWith("http://") || imageKey.startsWith("https://") || imageKey.startsWith("content://")) {
        AsyncImage(
            model = imageKey,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        val (bgStart, bgEnd, icon) = when (category) {
            ProductCategory.FRUITS_VEG -> Triple(Color(0xFFD9F7E4), Color(0xFFA7E9C2), Icons.Rounded.Eco)
            ProductCategory.DAIRY -> Triple(Color(0xFFE7F3FF), Color(0xFFC5E1FF), Icons.Rounded.EggAlt)
            ProductCategory.BAKERY -> Triple(Color(0xFFFFF0D9), Color(0xFFFFD8A6), Icons.Rounded.BakeryDining)
            ProductCategory.BEVERAGES -> Triple(Color(0xFFFFEBDC), Color(0xFFFFCAA6), Icons.Rounded.LocalDrink)
            ProductCategory.HOUSEHOLD -> Triple(Color(0xFFE9F4F2), Color(0xFFC4E4DD), Icons.Rounded.CleaningServices)
        }
        val iconOverride = when (imageKey) {
            "olive_oil", "juice" -> Icons.Rounded.LocalCafe
            else -> icon
        }
        Box(
            modifier = modifier.background(
                Brush.linearGradient(colors = listOf(bgStart, bgEnd))
            ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconOverride,
                    contentDescription = contentDescription,
                    tint = Color(0xFF0D7A4B),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun OrderStatusBadge(
    status: OrderStatus,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status) {
        OrderStatus.PENDING -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        OrderStatus.PREPARING -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        OrderStatus.OUT_FOR_DELIVERY -> Color(0xFFE8F5E9) to Color(0xFF0D7A4B)
        OrderStatus.DELIVERED -> Color(0xFFD6F5E5) to Color(0xFF064E30)
        OrderStatus.CANCELLED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = AppStrings.statusLabel(status, language),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_basket),
            contentDescription = title,
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Rounded.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(actionLabel)
            }
        }
    }
}

object ExternalActions {
    fun playNewOrderAlert() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        } catch (_: Exception) {
        }
    }

    fun dialPhone(context: Context, phone: String) {
        val clean = phone.trim()
        if (clean.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, clean, Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(context: Context, phone: String, message: String) {
        val digits = phone.filter { it.isDigit() }
        if (digits.isBlank()) return
        val encoded = URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://wa.me/$digits?text=$encoded")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(context, "WhatsApp not available", Toast.LENGTH_SHORT).show()
        }
    }

    fun openInGoogleMaps(context: Context, lat: Double, lng: Double, label: String) {
        val encodedLabel = Uri.encode(label)
        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedLabel)")
        val intent = Intent(Intent.ACTION_VIEW, geoUri)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val webUri = Uri.parse("https://maps.google.com/?q=$lat,$lng")
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, webUri)) }
        }
    }

    fun openInWaze(context: Context, lat: Double, lng: Double) {
        val wazeUri = Uri.parse("https://waze.com/ul?ll=$lat,$lng&navigate=yes")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, wazeUri))
        } catch (_: Exception) {
            openInGoogleMaps(context, lat, lng, "Aya Market Delivery")
        }
    }

    fun printOrderInvoice(context: Context, order: Order, language: AppLanguage) {
        val webView = WebView(context)
        val dir = if (language == AppLanguage.AR) "rtl" else "ltr"
        val rows = order.items.joinToString("") { item ->
            val name = item.localizedName(language)
            val lineUsd = item.unitPriceUsd * item.quantity
            "<tr><td>$name</td><td>${item.quantity}</td><td>$${"%.2f".format(lineUsd)}</td></tr>"
        }
        val html = """
            <!DOCTYPE html>
            <html dir="$dir">
            <head>
              <meta charset="utf-8"/>
              <style>
                body { font-family: sans-serif; padding: 20px; color: #10241B; }
                h1 { color: #0D7A4B; margin-bottom: 4px; }
                table { width: 100%; border-collapse: collapse; margin-top: 16px; }
                th, td { border-bottom: 1px solid #ddd; padding: 8px; text-align: start; }
                .total { font-size: 18px; font-weight: bold; margin-top: 16px; }
              </style>
            </head>
            <body>
              <h1>Aya Market / أيا ماركت</h1>
              <p><strong>Order #${order.id.take(8).uppercase()}</strong></p>
              <p>Customer: ${order.customerName} (${order.customerPhone})</p>
              <p>Address: ${order.deliveryAddress}</p>
              <p>Payment: ${AppStrings.paymentLabel(order.parsedPaymentMethod, language)}</p>
              <table>
                <tr><th>Item</th><th>Qty</th><th>USD</th></tr>
                $rows
              </table>
              <p class="total">Total USD: $${"%.2f".format(order.totalUsd)}</p>
              <p class="total">Total LBP: ${PriceFormatter.formatLbpExact(order.totalLbp, language)}</p>
            </body>
            </html>
        """.trimIndent()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = false
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
                val jobName = "AyaMarket_Invoice_${order.id.take(8)}"
                val adapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, adapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    }
}

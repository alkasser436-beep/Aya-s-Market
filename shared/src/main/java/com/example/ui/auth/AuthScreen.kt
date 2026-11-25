package com.example.ui.auth

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.shared.R
import com.example.data.model.AppLanguage
import com.example.data.model.AppRole
import com.example.data.remote.AyaMarketRepository
import com.example.localization.AppStrings

@Composable
fun AuthScreen(
    appRole: AppRole = AppRole.CUSTOMER,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    externalError: String? = null,
    signedInUnauthorizedEmail: String? = null,
    firebaseReady: Boolean = true,
    onSignOutClick: () -> Unit = {},
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(appRole, firebaseReady) {
        if (!firebaseReady) return@LaunchedEffect
        val currentUser = safeCurrentFirebaseUser(context)
        if (currentUser != null) {
            if (appRole != AppRole.ADMIN || AyaMarketRepository.isAuthorizedOwner(currentUser)) {
                onAuthSuccess()
            }
        } else {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    val user = safeCurrentFirebaseUser(context)
                    if (appRole != AppRole.ADMIN || AyaMarketRepository.isAuthorizedOwner(user)) {
                        onAuthSuccess()
                    }
                },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val appTitle = AppStrings.appTitleForRole(appRole, language)
    val appSubtitle = AppStrings.appSubtitleForRole(appRole, language)
    val displayedError = externalError ?: errorMessage

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        // Subtle branded logo watermark background
        Image(
            painter = painterResource(id = R.drawable.aya_market_icon),
            contentDescription = null,
            alpha = 0.06f,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFFBFBFB),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.aya_market_icon),
                            contentDescription = appTitle,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = appTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (appRole == AppRole.DRIVER) Color(0xFFE65100) else MaterialTheme.colorScheme.primary
                    )
                }

                FilledTonalButton(
                    onClick = onToggleLanguage,
                    modifier = Modifier.testTag("auth_language_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Translate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (language == AppLanguage.EN) "العربية" else "English")
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    // Branded Hero Header with the Official Aya's Market Emblem
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFBFBFB))
                            .padding(top = 14.dp, bottom = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = when (appRole) {
                                        AppRole.CUSTOMER -> if (language == AppLanguage.AR) "توصيل خلال ٣٠ دقيقة" else "30-Min Express Delivery"
                                        AppRole.ADMIN -> if (language == AppLanguage.AR) "بوابة الإدارة • للمالك فقط" else "Owner-Only Control Panel"
                                        AppRole.DRIVER -> if (language == AppLanguage.AR) "شريك التوصيل المعتمد" else "Delivery Partner App"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }

                            Image(
                                painter = painterResource(id = R.drawable.aya_market_icon),
                                contentDescription = appTitle,
                                modifier = Modifier
                                    .height(170.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                contentScale = ContentScale.Fit
                            )

                            Text(
                                text = appSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = when (appRole) {
                                    AppRole.DRIVER -> Color(0xFFE65100)
                                    else -> Color(0xFF064E30)
                                },
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = when (appRole) {
                                AppRole.ADMIN -> Color(0xFFE8F5E9)
                                AppRole.DRIVER -> Color(0xFFFFF3E0)
                                AppRole.CUSTOMER -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (appRole) {
                                        AppRole.ADMIN -> Icons.Rounded.Lock
                                        AppRole.DRIVER -> Icons.Rounded.LocalShipping
                                        AppRole.CUSTOMER -> Icons.Rounded.Storefront
                                    },
                                    contentDescription = null,
                                    tint = when (appRole) {
                                        AppRole.ADMIN -> Color(0xFF064E30)
                                        AppRole.DRIVER -> Color(0xFFE65100)
                                        AppRole.CUSTOMER -> MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = when (appRole) {
                                        AppRole.ADMIN -> if (language == AppLanguage.AR)
                                            "تطبيق الإدارة مستقل ومحـصور بحسابات المالك المعتمدة فقط ولا يمكن لأي شخص آخر إنشاء حساب إداري."
                                        else
                                            "Standalone Admin app locked strictly to authorized store owner Google accounts."
                                        AppRole.DRIVER -> if (language == AppLanguage.AR)
                                            "تطبيق السائق المستقل لاستلام الطلبات وتحديث حالة التوصيل وتحصيل الدفعات."
                                        else
                                            "Standalone Driver app for assigned deliveries, GPS navigation, and cash collection."
                                        AppRole.CUSTOMER -> if (language == AppLanguage.AR)
                                            "تطبيق المتسوق المستقل لطلب المنتجات الطازجة بالدولار والليرة اللبنانية."
                                        else
                                            "Standalone Customer store app for fresh groceries in USD & LBP."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = when (appRole) {
                                        AppRole.ADMIN -> Color(0xFF064E30)
                                        AppRole.DRIVER -> Color(0xFFBF360C)
                                        AppRole.CUSTOMER -> MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (appRole) {
                                AppRole.CUSTOMER -> {
                                    RolePillPreview(
                                        icon = Icons.Rounded.ShoppingBasket,
                                        label = if (language == AppLanguage.AR) "ليرة ودولار" else "USD & LBP",
                                        modifier = Modifier.weight(1f)
                                    )
                                    RolePillPreview(
                                        icon = Icons.Rounded.QrCodeScanner,
                                        label = if (language == AppLanguage.AR) "ماسح باركود" else "Barcode Scan",
                                        modifier = Modifier.weight(1f)
                                    )
                                    RolePillPreview(
                                        icon = Icons.Rounded.Map,
                                        label = if (language == AppLanguage.AR) "تحديد الموقع" else "GPS Pinning",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                AppRole.ADMIN -> {
                                    RolePillPreview(
                                        icon = Icons.Rounded.CurrencyExchange,
                                        label = if (language == AppLanguage.AR) "سعر الصرف" else "Live Rate",
                                        modifier = Modifier.weight(1f)
                                    )
                                    RolePillPreview(
                                        icon = Icons.Rounded.Inventory2,
                                        label = if (language == AppLanguage.AR) "إدارة المخزون" else "Inventory",
                                        modifier = Modifier.weight(1f)
                                    )
                                    RolePillPreview(
                                        icon = Icons.Rounded.ReceiptLong,
                                        label = if (language == AppLanguage.AR) "طباعة فواتير" else "Print Invoices",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                AppRole.DRIVER -> {
                                    RolePillPreview(
                                        icon = Icons.Rounded.LocalShipping,
                                        label = if (language == AppLanguage.AR) "طلبات فورية" else "Live Tasks",
                                        modifier = Modifier.weight(1f)
                                    )
                                    RolePillPreview(
                                        icon = Icons.Rounded.Map,
                                        label = if (language == AppLanguage.AR) "خرائط وويز" else "Maps & Waze",
                                        modifier = Modifier.weight(1f)
                                    )
                                    RolePillPreview(
                                        icon = Icons.Rounded.Payments,
                                        label = if (language == AppLanguage.AR) "تأكيد التحصيل" else "Cash Collect",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        displayedError?.let { msg ->
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (signedInUnauthorizedEmail != null && appRole == AppRole.ADMIN) {
                            OutlinedButton(
                                onClick = {
                                    errorMessage = null
                                    signOutUser(context, credentialManager, scope) {
                                        onSignOutClick()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.AR)
                                        "تبديل الحساب ($signedInUnauthorizedEmail)"
                                    else
                                        "Switch Account ($signedInUnauthorizedEmail)"
                                )
                            }
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                signOutUser(context, credentialManager, scope) {
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            isLoading = false
                                            val user = safeCurrentFirebaseUser(context)
                                            if (appRole == AppRole.ADMIN && !AyaMarketRepository.isAuthorizedOwner(user)) {
                                                val attempted = user?.email ?: ""
                                                signOutUser(context, credentialManager, scope) {
                                                    errorMessage = if (language == AppLanguage.AR)
                                                        "دخول مرفوض ($attempted): تطبيق الإدارة محصور بحسابات مالك المتجر فقط."
                                                    else
                                                        "Access denied ($attempted): Admin app is locked to authorized store owner accounts only."
                                                }
                                            } else {
                                                onAuthSuccess()
                                            }
                                        },
                                        onAuthError = { msg ->
                                            isLoading = false
                                            errorMessage = msg
                                        },
                                        scope = scope,
                                        onAuthCancelled = { isLoading = false }
                                    )
                                }
                            },
                            enabled = firebaseReady && !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (appRole) {
                                    AppRole.DRIVER -> Color(0xFFE65100)
                                    AppRole.ADMIN -> Color(0xFF064E30)
                                    AppRole.CUSTOMER -> MaterialTheme.colorScheme.primary
                                }
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    text = when (appRole) {
                                        AppRole.ADMIN -> if (language == AppLanguage.AR) "تسجيل دخول المالك عبر Google" else "Owner Sign in with Google"
                                        else -> AppStrings.signInWithGoogle(language)
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RolePillPreview(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Phone
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import kotlinx.coroutines.launch
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
    var isSignUpMode by remember(appRole) { mutableStateOf(false) }
    var accountEmail by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var addressLine by remember { mutableStateOf("") }

    LaunchedEffect(appRole, firebaseReady) {
        if (!firebaseReady) return@LaunchedEffect
        val currentUser = safeCurrentFirebaseUser(context)
        if (currentUser != null) {
            if (appRole != AppRole.ADMIN || AyaMarketRepository.isAuthorizedOwner(currentUser)) {
                onAuthSuccess()
            }
        } else if (appRole != AppRole.ADMIN) {
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
                                            "تطبيق السائق مستقل ومخصص للسائقين المضافين من قِبل الإدارة فقط."
                                        else
                                            "Standalone Driver app reserved strictly for drivers added by the Store Admin."
                                        AppRole.CUSTOMER -> if (language == AppLanguage.AR)
                                            "سجّل الدخول بحسابك أو أنشئ حساب زبون جديد للتسوق بالدولار والليرة."
                                        else
                                            "Sign in or create a new customer account to shop in USD & LBP."
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

                        if (appRole == AppRole.CUSTOMER) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val signInLabel = if (language == AppLanguage.AR) "تسجيل الدخول" else "Sign In"
                                val signUpLabel = if (language == AppLanguage.AR) "إنشاء حساب" else "Create Account"

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            isSignUpMode = false
                                            errorMessage = null
                                        }
                                        .testTag("auth_mode_signin"),
                                    color = if (!isSignUpMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = signInLabel,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isSignUpMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            isSignUpMode = true
                                            errorMessage = null
                                        }
                                        .testTag("auth_mode_signup"),
                                    color = if (isSignUpMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = signUpLabel,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSignUpMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }

                        if (appRole == AppRole.CUSTOMER && isSignUpMode) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = accountEmail,
                                    onValueChange = { accountEmail = it },
                                    label = { Text(if (language == AppLanguage.AR) "البريد الإلكتروني" else "Email Address") },
                                    placeholder = { Text("name@gmail.com") },
                                    leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_email_input")
                                )
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text(if (language == AppLanguage.AR) "الاسم الكامل" else "Full Name") },
                                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_name_input")
                                )
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text(if (language == AppLanguage.AR) "رقم الهاتف" else "Phone Number") },
                                    placeholder = { Text("+961 71 123 456") },
                                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_phone_input")
                                )
                                OutlinedTextField(
                                    value = city,
                                    onValueChange = { city = it },
                                    label = { Text(if (language == AppLanguage.AR) "المدينة / المنطقة" else "City / Area") },
                                    placeholder = { Text(if (language == AppLanguage.AR) "بيروت" else "Beirut") },
                                    leadingIcon = { Icon(Icons.Rounded.LocationCity, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_city_input")
                                )
                                OutlinedTextField(
                                    value = addressLine,
                                    onValueChange = { addressLine = it },
                                    label = { Text(if (language == AppLanguage.AR) "العنوان التفصيلي (اختياري)" else "Street Address (Optional)") },
                                    leadingIcon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("signup_address_input")
                                )
                            }
                        } else {
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

                        val launchGoogleAuth = {
                            val startSignIn = {
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        val user = safeCurrentFirebaseUser(context)
                                        if (appRole == AppRole.ADMIN && !AyaMarketRepository.isAuthorizedOwner(user)) {
                                            isLoading = false
                                            val attempted = AyaMarketRepository.resolveUserEmail(user) ?: (user?.uid ?: "")
                                            val ownerEmail = AyaMarketRepository.ADMIN_OWNER_EMAILS.first()
                                            signOutUser(context, credentialManager, scope) {
                                                errorMessage = if (language == AppLanguage.AR)
                                                    "دخول مرفوض ($attempted): تطبيق الإدارة محصور بحساب المالك ($ownerEmail) فقط."
                                                else
                                                    "Access denied ($attempted): Admin app is locked to $ownerEmail only."
                                            }
                                        } else if (appRole == AppRole.CUSTOMER && isSignUpMode) {
                                            scope.launch {
                                                val repo = AyaMarketRepository(context)
                                                val res = repo.saveCustomerProfile(
                                                    fullName = fullName,
                                                    phone = phone,
                                                    city = city,
                                                    defaultAddressLine = addressLine,
                                                    preferredEmail = accountEmail
                                                )
                                                isLoading = false
                                                res.onSuccess {
                                                    onAuthSuccess()
                                                }.onFailure { err ->
                                                    errorMessage = err.localizedMessage ?: "Failed to save account profile"
                                                }
                                            }
                                        } else {
                                            isLoading = false
                                            onAuthSuccess()
                                        }
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                        errorMessage = if (language == AppLanguage.AR) {
                                            "لم يكتمل اختيار حساب Google. تأكد من وجود حساب Google مسجل في جهازك وحاول مرة أخرى."
                                        } else {
                                            "Google account selection was cancelled. Ensure a Google account is added on your device and try again."
                                        }
                                    }
                                )
                            }

                            if (safeCurrentFirebaseUser(context) != null) {
                                signOutUser(context, credentialManager, scope) {
                                    startSignIn()
                                }
                            } else {
                                startSignIn()
                            }
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                if (appRole == AppRole.CUSTOMER && isSignUpMode) {
                                    val trimmedEmail = accountEmail.trim()
                                    if (trimmedEmail.length < 5 || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
                                        errorMessage = if (language == AppLanguage.AR) "يرجى إدخال بريد إلكتروني صحيح" else "Please enter a valid email address"
                                        return@Button
                                    }
                                    if (fullName.trim().length < 2) {
                                        errorMessage = if (language == AppLanguage.AR) "يرجى إدخال الاسم الكامل" else "Please enter your full name"
                                        return@Button
                                    }
                                    if (phone.trim().length < 6) {
                                        errorMessage = if (language == AppLanguage.AR) "يرجى إدخال رقم هاتف صحيح" else "Please enter a valid phone number"
                                        return@Button
                                    }
                                    if (city.trim().length < 2) {
                                        errorMessage = if (language == AppLanguage.AR) "يرجى إدخال المدينة أو المنطقة" else "Please enter your city or area"
                                        return@Button
                                    }
                                }
                                isLoading = true
                                launchGoogleAuth()
                            },
                            enabled = firebaseReady && !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag(if (appRole == AppRole.CUSTOMER && isSignUpMode) "create_account_button" else "google_sign_in_button"),
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
                                if (appRole == AppRole.CUSTOMER && isSignUpMode) {
                                    Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = when {
                                        appRole == AppRole.ADMIN -> if (language == AppLanguage.AR) "تسجيل دخول المالك عبر Google" else "Owner Sign in with Google"
                                        appRole == AppRole.CUSTOMER && isSignUpMode -> if (language == AppLanguage.AR) "إنشاء حساب عبر Google" else "Create Account with Google"
                                        else -> AppStrings.signInWithGoogle(language)
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        if (appRole == AppRole.CUSTOMER) {
                            TextButton(
                                onClick = {
                                    isSignUpMode = !isSignUpMode
                                    errorMessage = null
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text = if (isSignUpMode) {
                                        if (language == AppLanguage.AR) "لديك حساب بالفعل؟ سجّل الدخول" else "Already have an account? Sign In"
                                    } else {
                                        if (language == AppLanguage.AR) "مستخدم جديد؟ أنشئ حساب زبون جديد" else "New customer? Create a new account"
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

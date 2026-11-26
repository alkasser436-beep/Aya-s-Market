package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.shared.R
import com.example.data.model.AppLanguage
import com.example.data.model.AppRole
import com.example.data.model.StoreConfig
import com.example.data.model.UiState
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.AyaMarketRepository
import com.example.localization.AppStrings
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.isFirebaseReady
import com.example.ui.auth.safeCurrentFirebaseUser
import com.example.ui.auth.signOutUser
import com.example.ui.components.AyaHeaderBar
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.InteractiveLocationPickerDialog
import com.example.ui.customer.CustomerCartCheckoutScreen
import com.example.ui.customer.CustomerHomeScreen
import com.example.ui.customer.CustomerOrdersScreen
import com.example.ui.driver.DriverAppScreen
import com.example.ui.theme.AyaMarketTheme
import com.example.ui.viewmodel.AyaMarketViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun AyaMarketRoot(appRole: AppRole = AppRole.CUSTOMER) {
    val context = LocalContext.current
    val prefsRepo = remember(context) { UserPreferencesRepository(context.applicationContext) }
    val userPrefs by prefsRepo.prefsFlow.collectAsStateWithLifecycle(
        initialValue = com.example.data.preferences.UserPrefs()
    )
    val scope = rememberCoroutineScope()
    val firebaseReady = remember(context) { isFirebaseReady(context) }

    var currentUser by remember(firebaseReady) {
        mutableStateOf(if (firebaseReady) safeCurrentFirebaseUser(context) else null)
    }
    DisposableEffect(firebaseReady) {
        if (!firebaseReady) {
            onDispose { }
        } else {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                currentUser = auth.currentUser
            }
            Firebase.auth.addAuthStateListener(listener)
            onDispose { Firebase.auth.removeAuthStateListener(listener) }
        }
    }

    val isArabic = userPrefs.language == AppLanguage.AR
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        AyaMarketTheme(isArabic = isArabic) {
            val user = currentUser
            val isAuthorizedForRole = firebaseReady && user != null && (
                appRole != AppRole.ADMIN || AyaMarketRepository.isAuthorizedOwner(user)
            )

            if (!isAuthorizedForRole) {
                val unauthorizedEmail = if (firebaseReady && user != null && appRole == AppRole.ADMIN) {
                    user.email ?: user.uid
                } else null

                val authBannerError = when {
                    !firebaseReady -> if (isArabic) {
                        "إعدادات Firebase (google-services.json) غير مدمجة في ملف APK هذا. أضف GOOGLE_SERVICES_JSON إلى GitHub Secrets قبل البناء أو صدّر ملف APK مباشرة من AI Studio."
                    } else {
                        "Firebase is not configured in this APK (missing google-services.json). Add GOOGLE_SERVICES_JSON to GitHub Secrets before building, or download the APK from AI Studio."
                    }
                    unauthorizedEmail != null -> if (isArabic) {
                        "دخول مرفوض ($unauthorizedEmail): تطبيق الإدارة حصري لحسابات مالك المتجر فقط. قم بتبديل الحساب لحساب المالك."
                    } else {
                        "Access denied ($unauthorizedEmail): Admin app is locked to authorized store owner accounts only. Switch to an owner account."
                    }
                    else -> null
                }

                AuthScreen(
                    appRole = appRole,
                    language = userPrefs.language,
                    onToggleLanguage = {
                        scope.launch {
                            val next = if (userPrefs.language == AppLanguage.EN) AppLanguage.AR else AppLanguage.EN
                            prefsRepo.setLanguage(next)
                        }
                    },
                    externalError = authBannerError,
                    signedInUnauthorizedEmail = unauthorizedEmail,
                    firebaseReady = firebaseReady,
                    onSignOutClick = {
                        currentUser = safeCurrentFirebaseUser(context)
                    },
                    onAuthSuccess = {
                        currentUser = safeCurrentFirebaseUser(context)
                    }
                )
            } else {
                val signedUser = checkNotNull(user)
                val uid = signedUser.uid
                key(uid, appRole) {
                    AyaMarketAuthenticatedApp(
                        appRole = appRole,
                        uid = uid,
                        email = signedUser.email ?: "",
                        displayName = signedUser.displayName ?: "",
                        prefsRepo = prefsRepo
                    )
                }
            }
        }
    }
}

@Composable
private fun AyaMarketAuthenticatedApp(
    appRole: AppRole,
    uid: String,
    email: String,
    displayName: String,
    prefsRepo: UserPreferencesRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: AyaMarketViewModel = viewModel(
        key = "${uid}_${appRole.name}",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AyaMarketViewModel(
                    repository = AyaMarketRepository(db),
                    prefsRepo = prefsRepo,
                    currentUserId = uid,
                    currentUserEmail = email,
                    currentUserName = displayName,
                    initialRole = appRole
                )
            }
        }
    )

    val language by viewModel.language.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val storeConfigState by viewModel.storeConfigState.collectAsStateWithLifecycle()
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val promosState by viewModel.promosState.collectAsStateWithLifecycle()
    val customerProfileState by viewModel.customerProfileState.collectAsStateWithLifecycle()
    val addressesState by viewModel.addressesState.collectAsStateWithLifecycle()
    val customerOrdersState by viewModel.customerOrdersState.collectAsStateWithLifecycle()
    val driverOrdersState by viewModel.driverOrdersState.collectAsStateWithLifecycle()
    val adminOrdersState by viewModel.adminOrdersState.collectAsStateWithLifecycle()
    val driversState by viewModel.driversState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val pinnedLocation by viewModel.pinnedLocation.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.adminSoundEnabled.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val storeConfig = (storeConfigState as? UiState.Success)?.data ?: StoreConfig()
    val customerProfile = (customerProfileState as? UiState.Success)?.data
    val savedAddresses = (addressesState as? UiState.Success)?.data ?: emptyList()
    val products = (productsState as? UiState.Success)?.data ?: emptyList()

    var customerTab by remember { mutableIntStateOf(0) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showBarcodeDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // BackHandler for Customer secondary tabs
    BackHandler(enabled = appRole == AppRole.CUSTOMER && customerTab != 0) {
        customerTab = 0
    }

    val cartCount = cart.values.sumOf { it.quantity }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp && appRole == AppRole.CUSTOMER

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                AyaHeaderBar(
                    language = language,
                    currency = currency,
                    appRole = appRole,
                    exchangeRateLbp = storeConfig.exchangeRateLbp,
                    onToggleLanguage = viewModel::toggleLanguage,
                    onToggleCurrency = viewModel::toggleCurrency,
                    onSignOut = { signOutUser(context, credentialManager, scope) }
                )
            },
            bottomBar = {
                if (appRole == AppRole.CUSTOMER && !useNavRail) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = customerTab == 0,
                            onClick = { customerTab = 0 },
                            icon = { Icon(Icons.Rounded.Storefront, contentDescription = null) },
                            label = { Text(AppStrings.shopTab(language)) },
                            modifier = Modifier.testTag("nav_shop")
                        )
                        NavigationBarItem(
                            selected = customerTab == 1,
                            onClick = { customerTab = 1 },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (cartCount > 0) {
                                            Badge { Text("$cartCount") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Rounded.ShoppingCart, contentDescription = null)
                                }
                            },
                            label = { Text(AppStrings.cartTab(language)) },
                            modifier = Modifier.testTag("nav_cart")
                        )
                        NavigationBarItem(
                            selected = customerTab == 2,
                            onClick = { customerTab = 2 },
                            icon = { Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null) },
                            label = { Text(AppStrings.ordersTab(language)) },
                            modifier = Modifier.testTag("nav_orders")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        NavigationRailItem(
                            selected = customerTab == 0,
                            onClick = { customerTab = 0 },
                            icon = { Icon(Icons.Rounded.Storefront, contentDescription = null) },
                            label = { Text(AppStrings.shopTab(language)) }
                        )
                        NavigationRailItem(
                            selected = customerTab == 1,
                            onClick = { customerTab = 1 },
                            icon = {
                                BadgedBox(
                                    badge = { if (cartCount > 0) Badge { Text("$cartCount") } }
                                ) {
                                    Icon(Icons.Rounded.ShoppingCart, contentDescription = null)
                                }
                            },
                            label = { Text(AppStrings.cartTab(language)) }
                        )
                        NavigationRailItem(
                            selected = customerTab == 2,
                            onClick = { customerTab = 2 },
                            icon = { Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null) },
                            label = { Text(AppStrings.ordersTab(language)) }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.aya_market_icon),
                        contentDescription = null,
                        alpha = 0.045f,
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.Center)
                            .padding(32.dp)
                            .fillMaxSize()
                    )
                    when (appRole) {
                        AppRole.CUSTOMER -> {
                            when (customerTab) {
                                0 -> CustomerHomeScreen(
                                    language = language,
                                    currency = currency,
                                    storeConfig = storeConfig,
                                    productsState = productsState,
                                    promosState = promosState,
                                    pinnedLocation = pinnedLocation,
                                    searchQuery = searchQuery,
                                    selectedCategory = selectedCategory,
                                    cart = cart,
                                    isAdminAuthorized = viewModel.isAdminAuthorized,
                                    onSearchChange = viewModel::setSearchQuery,
                                    onCategorySelected = viewModel::selectCategory,
                                    onOpenLocationPicker = { showLocationDialog = true },
                                    onOpenBarcodeScanner = { showBarcodeDialog = true },
                                    onAddToCart = viewModel::addToCart,
                                    onDecrementCart = viewModel::decrementFromCart,
                                    onSeedCatalog = viewModel::seedStarterStore
                                )
                                1 -> CustomerCartCheckoutScreen(
                                    language = language,
                                    currency = currency,
                                    storeConfig = storeConfig,
                                    cart = cart,
                                    pinnedLocation = pinnedLocation,
                                    savedAddresses = savedAddresses,
                                    defaultCustomerName = customerProfile?.fullName?.ifBlank { displayName } ?: displayName,
                                    defaultCustomerPhone = customerProfile?.phone ?: "",
                                    isBusy = isBusy,
                                    onAddToCart = viewModel::addToCart,
                                    onDecrementCart = viewModel::decrementFromCart,
                                    onRemoveFromCart = viewModel::removeFromCart,
                                    onOpenLocationPicker = { showLocationDialog = true },
                                    onSelectSavedAddress = viewModel::setPinnedLocation,
                                    onBrowseMarket = { customerTab = 0 },
                                    onPlaceOrder = { name, phone, addr, notes, lat, lng, method ->
                                        viewModel.placeOrder(
                                            customerName = name,
                                            customerPhone = phone,
                                            deliveryAddress = addr,
                                            deliveryNotes = notes,
                                            latitude = lat,
                                            longitude = lng,
                                            paymentMethod = method
                                        ) {
                                            customerTab = 2
                                        }
                                    }
                                )
                                else -> CustomerOrdersScreen(
                                    language = language,
                                    currency = currency,
                                    ordersState = customerOrdersState,
                                    onBrowseMarket = { customerTab = 0 },
                                    onCancelOrder = viewModel::cancelOrder
                                )
                            }
                        }
                        AppRole.ADMIN -> AdminDashboardScreen(
                            language = language,
                            isAdminAuthorized = viewModel.isAdminAuthorized,
                            currentUserId = uid,
                            currentUserName = displayName,
                            storeConfig = storeConfig,
                            productsState = productsState,
                            ordersState = adminOrdersState,
                            promosState = promosState,
                            driversState = driversState,
                            soundEnabled = soundEnabled,
                            onToggleSound = viewModel::toggleAdminSound,
                            onUpdateRate = viewModel::updateExchangeRate,
                            onSaveProduct = viewModel::saveProduct,
                            onDeleteProduct = viewModel::deleteProduct,
                            onUpdateOrderStatus = viewModel::updateOrderStatus,
                            onAssignDriver = viewModel::assignDriver,
                            onSavePromo = viewModel::savePromo,
                            onDeletePromo = viewModel::deletePromo,
                            onSaveDriver = viewModel::saveDriver,
                            onDeleteDriver = viewModel::deleteDriver,
                            onSeedCatalog = viewModel::seedStarterStore
                        )
                        AppRole.DRIVER -> DriverAppScreen(
                            language = language,
                            currentUserId = uid,
                            currentUserEmail = email,
                            driverOrdersState = driverOrdersState,
                            driversState = driversState,
                            onClaimDriverUid = viewModel::claimDriverUid,
                            onToggleAvailability = viewModel::toggleDriverAvailability,
                            onMarkPickedUp = viewModel::driverMarkPickedUp,
                            onConfirmDelivered = viewModel::driverConfirmDelivered,
                            onSignOut = { signOutUser(context, credentialManager, scope) }
                        )
                    }
                }
            }
        }
    }

    if (appRole == AppRole.CUSTOMER && showLocationDialog) {
        InteractiveLocationPickerDialog(
            language = language,
            currentPinned = pinnedLocation,
            savedAddresses = savedAddresses,
            onSelectLocation = viewModel::setPinnedLocation,
            onSaveAddress = { addr -> viewModel.saveAddress(addr) },
            onDeleteAddress = viewModel::deleteAddress,
            onDismiss = { showLocationDialog = false }
        )
    }

    if (appRole == AppRole.CUSTOMER && showBarcodeDialog) {
        BarcodeScannerDialog(
            language = language,
            products = products,
            onBarcodeDetected = { code ->
                showBarcodeDialog = false
                val match = products.firstOrNull { it.barcode.equals(code, ignoreCase = true) }
                if (match != null) {
                    viewModel.addToCart(match, 1)
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (language == AppLanguage.AR) "تمت إضافة ${match.localizedName(language)}"
                            else "Added ${match.localizedName(language)} to cart"
                        )
                    }
                } else {
                    viewModel.setSearchQuery(code)
                }
            },
            onDismiss = { showBarcodeDialog = false }
        )
    }
}

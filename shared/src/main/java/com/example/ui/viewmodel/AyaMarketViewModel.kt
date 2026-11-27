package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppCurrency
import com.example.data.model.AppLanguage
import com.example.data.model.AppRole
import com.example.data.model.CartItem
import com.example.data.model.CustomerProfile
import com.example.data.model.DriverProfile
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.PromoBanner
import com.example.data.model.StoreConfig
import com.example.data.model.UiState
import com.example.data.model.UserAddress
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.AyaMarketRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AyaMarketViewModel(
    private val repository: AyaMarketRepository,
    private val prefsRepo: UserPreferencesRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserName: String,
    initialRole: AppRole = AppRole.CUSTOMER
) : ViewModel() {

    val appRole: AppRole = initialRole
    val isAdminAuthorized: Boolean = repository.isCurrentUserBootstrappedAdmin()

    val language: StateFlow<AppLanguage> = prefsRepo.prefsFlow
        .map { it.language }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppLanguage.EN)

    val currency: StateFlow<AppCurrency> = prefsRepo.prefsFlow
        .map { it.currency }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppCurrency.USD)

    val storeConfigState: StateFlow<UiState<StoreConfig>> = repository.observeStoreConfig()
        .toUiState("store_config")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val productsState: StateFlow<UiState<List<Product>>> =
        if (initialRole != AppRole.DRIVER) {
            repository.observeProducts().toUiState("products")
        } else {
            flowOf(UiState.Success(emptyList()))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val promosState: StateFlow<UiState<List<PromoBanner>>> =
        if (initialRole != AppRole.DRIVER) {
            repository.observePromos().toUiState("promos")
        } else {
            flowOf(UiState.Success(emptyList()))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val customerProfileState: StateFlow<UiState<CustomerProfile?>> =
        if (initialRole == AppRole.CUSTOMER) {
            repository.observeCustomerProfile(currentUserId).toUiState("customer_profile")
        } else {
            flowOf(UiState.Success(null))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val addressesState: StateFlow<UiState<List<UserAddress>>> =
        if (initialRole == AppRole.CUSTOMER) {
            repository.observeAddresses(currentUserId).toUiState("addresses")
        } else {
            flowOf(UiState.Success(emptyList()))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val customerOrdersState: StateFlow<UiState<List<Order>>> =
        if (initialRole == AppRole.CUSTOMER) {
            repository.observeCustomerOrders(currentUserId).toUiState("customer_orders")
        } else {
            flowOf(UiState.Success(emptyList()))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val driverOrdersState: StateFlow<UiState<List<Order>>> =
        if (initialRole == AppRole.DRIVER) {
            repository.observeDriverOrders(currentUserId, currentUserEmail).toUiState("driver_orders")
        } else {
            flowOf(UiState.Success(emptyList()))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val driversState: StateFlow<UiState<List<DriverProfile>>> =
        if (initialRole == AppRole.ADMIN || initialRole == AppRole.DRIVER) {
            repository.observeDrivers(currentUserId, currentUserEmail, isAdminAuthorized).toUiState("drivers")
        } else {
            flowOf(UiState.Success(emptyList()))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    // Only attach unfiltered admin order listener in Admin app when authorized admin is signed in
    val adminOrdersState: StateFlow<UiState<List<Order>>> =
        if (initialRole == AppRole.ADMIN && isAdminAuthorized) {
            repository.observeAllOrdersForAdmin().toUiState("admin_orders")
        } else {
            flowOf(UiState.Success(emptyList()))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    // Customer shop state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ProductCategory?>(null)
    val selectedCategory: StateFlow<ProductCategory?> = _selectedCategory.asStateFlow()

    private val _cart = MutableStateFlow<Map<String, CartItem>>(emptyMap())
    val cart: StateFlow<Map<String, CartItem>> = _cart.asStateFlow()

    private val _pinnedLocation = MutableStateFlow(
        UserAddress(
            label = "Beirut Center",
            addressLine = "Hamra Street, Beirut",
            city = "Beirut",
            phone = "+961 70 000 000",
            latitude = 33.8963,
            longitude = 35.4819
        )
    )
    val pinnedLocation: StateFlow<UserAddress> = _pinnedLocation.asStateFlow()

    private val _adminSoundEnabled = MutableStateFlow(true)
    val adminSoundEnabled: StateFlow<Boolean> = _adminSoundEnabled.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    private var autoSeedAttempted = false

    init {
        // Automatically seed starter catalog once when admin opens an empty database
        if (isAdminAuthorized && initialRole != AppRole.DRIVER) {
            viewModelScope.launch {
                productsState.collect { state ->
                    if (!autoSeedAttempted && state is UiState.Success && state.data.isEmpty()) {
                        autoSeedAttempted = true
                        repository.seedStarterStoreIfAdmin()
                    }
                }
            }
        }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (language.value == AppLanguage.EN) AppLanguage.AR else AppLanguage.EN
            prefsRepo.setLanguage(next)
        }
    }

    fun toggleCurrency() {
        viewModelScope.launch {
            val next = if (currency.value == AppCurrency.USD) AppCurrency.LBP else AppCurrency.USD
            prefsRepo.setCurrency(next)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: ProductCategory?) {
        _selectedCategory.value = category
    }

    fun addToCart(product: Product, qty: Int = 1) {
        if (product.stock <= 0L) return
        _cart.update { current ->
            val existing = current[product.id]
            val newQty = ((existing?.quantity ?: 0) + qty).coerceAtMost(product.stock.toInt())
            current + (product.id to CartItem(product, newQty))
        }
    }

    fun decrementFromCart(productId: String) {
        _cart.update { current ->
            val existing = current[productId] ?: return@update current
            if (existing.quantity <= 1) {
                current - productId
            } else {
                current + (productId to existing.copy(quantity = existing.quantity - 1))
            }
        }
    }

    fun removeFromCart(productId: String) {
        _cart.update { it - productId }
    }

    fun clearCart() {
        _cart.value = emptyMap()
    }

    fun setPinnedLocation(address: UserAddress) {
        _pinnedLocation.value = address
    }

    fun toggleAdminSound() {
        _adminSoundEnabled.update { !it }
    }

    fun saveCustomerProfile(
        fullName: String,
        phone: String,
        city: String,
        defaultAddressLine: String = "",
        preferredEmail: String = "",
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveCustomerProfile(
                fullName = fullName,
                phone = phone,
                city = city,
                defaultAddressLine = defaultAddressLine,
                preferredEmail = preferredEmail
            )
            _isBusy.value = false
            res.onSuccess {
                emitMsg(if (language.value == AppLanguage.AR) "تم حفظ بيانات الحساب بنجاح" else "Account profile saved")
                onDone()
            }.onFailure { err ->
                emitMsg(err.localizedMessage ?: "Failed to save profile")
            }
        }
    }

    fun saveAddress(address: UserAddress, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveAddress(currentUserId, address)
            _isBusy.value = false
            result.onSuccess {
                _pinnedLocation.value = address
                emitMsg(if (language.value == AppLanguage.AR) "تم حفظ العنوان بنجاح" else "Address saved")
                onSuccess()
            }.onFailure { err ->
                emitMsg(err.localizedMessage ?: "Failed to save address")
            }
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            repository.deleteAddress(currentUserId, addressId)
                .onFailure { emitMsg(it.localizedMessage ?: "Failed to delete address") }
        }
    }

    fun placeOrder(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        deliveryNotes: String,
        latitude: Double,
        longitude: Double,
        paymentMethod: PaymentMethod,
        onOrderPlaced: (String) -> Unit
    ) {
        val items = _cart.value.values.toList()
        if (items.isEmpty()) return
        val cfg = (storeConfigState.value as? UiState.Success)?.data ?: StoreConfig()
        val subtotal = items.sumOf { it.lineTotalUsd }
        val fee = if (subtotal >= cfg.freeDeliveryThresholdUsd) 0.0 else cfg.deliveryFeeUsd

        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.placeOrder(
                customerName = customerName.ifBlank { currentUserName.ifBlank { "Aya Customer" } },
                customerPhone = customerPhone,
                deliveryAddress = deliveryAddress,
                deliveryNotes = deliveryNotes,
                latitude = latitude,
                longitude = longitude,
                currency = currency.value,
                exchangeRateAtOrder = cfg.exchangeRateLbp,
                paymentMethod = paymentMethod,
                subtotalUsd = subtotal,
                deliveryFeeUsd = fee,
                items = items.map {
                    OrderItem(
                        productId = it.product.id,
                        nameEn = it.product.nameEn,
                        nameAr = it.product.nameAr,
                        quantity = it.quantity.toLong(),
                        unitPriceUsd = it.product.effectivePriceUsd
                    )
                }
            )
            _isBusy.value = false
            result.onSuccess { orderId ->
                clearCart()
                emitMsg(if (language.value == AppLanguage.AR) "تم إرسال طلبك بنجاح!" else "Order placed successfully!")
                onOrderPlaced(orderId)
            }.onFailure { err ->
                emitMsg(err.localizedMessage ?: "Order submission failed")
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            repository.customerCancelPendingOrder(orderId)
                .onSuccess {
                    emitMsg(if (language.value == AppLanguage.AR) "تم إلغاء الطلب" else "Order cancelled")
                }
                .onFailure { emitMsg(it.localizedMessage ?: "Unable to cancel order") }
        }
    }

    // --- Admin Actions ---
    fun updateExchangeRate(rate: Double, deliveryFeeUsd: Double, freeThresholdUsd: Double) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateStoreConfig(rate, deliveryFeeUsd, freeThresholdUsd)
            _isBusy.value = false
            res.onSuccess {
                emitMsg(if (language.value == AppLanguage.AR) "تم تحديث سعر الصرف لحظياً" else "Exchange rate updated live")
            }.onFailure {
                emitMsg(it.localizedMessage ?: "Failed to update exchange rate")
            }
        }
    }

    fun saveProduct(product: Product, isNew: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveProduct(product, isNew)
            _isBusy.value = false
            res.onSuccess {
                emitMsg(if (language.value == AppLanguage.AR) "تم حفظ المنتج" else "Product saved")
                onDone()
            }.onFailure {
                emitMsg(it.localizedMessage ?: "Failed to save product")
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
                .onSuccess { emitMsg(if (language.value == AppLanguage.AR) "تم حذف المنتج" else "Product deleted") }
                .onFailure { emitMsg(it.localizedMessage ?: "Failed to delete product") }
        }
    }

    fun savePromo(promo: PromoBanner, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.savePromo(promo)
                .onSuccess {
                    emitMsg(if (language.value == AppLanguage.AR) "تم حفظ العرض" else "Promo saved")
                    onDone()
                }
                .onFailure { emitMsg(it.localizedMessage ?: "Failed to save promo") }
        }
    }

    fun deletePromo(promoId: String) {
        viewModelScope.launch {
            repository.deletePromo(promoId)
                .onFailure { emitMsg(it.localizedMessage ?: "Failed to delete promo") }
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            repository.adminUpdateOrderStatus(orderId, status)
                .onFailure { emitMsg(it.localizedMessage ?: "Failed to update order") }
        }
    }

    fun assignDriver(orderId: String, driver: DriverProfile) {
        viewModelScope.launch {
            repository.adminAssignDriver(orderId, driver)
                .onSuccess {
                    emitMsg(if (language.value == AppLanguage.AR) "تم تعيين السائق للطلب" else "Driver assigned")
                }
                .onFailure { emitMsg(it.localizedMessage ?: "Failed to assign driver") }
        }
    }

    fun saveDriver(driver: DriverProfile, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveDriver(driver)
                .onSuccess {
                    emitMsg(if (language.value == AppLanguage.AR) "تم حفظ بيانات السائق" else "Driver saved")
                    onDone()
                }
                .onFailure { emitMsg(it.localizedMessage ?: "Failed to save driver") }
        }
    }

    fun deleteDriver(driverId: String) {
        viewModelScope.launch {
            repository.deleteDriver(driverId)
                .onSuccess {
                    emitMsg(if (language.value == AppLanguage.AR) "تم حذف السائق" else "Driver removed")
                }
                .onFailure { emitMsg(it.localizedMessage ?: "Failed to delete driver") }
        }
    }

    fun seedStarterStore() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.seedStarterStoreIfAdmin()
            _isBusy.value = false
            res.onSuccess {
                emitMsg(if (language.value == AppLanguage.AR) "تمت إضافة المنتجات الافتراضية" else "Starter catalog loaded")
            }.onFailure {
                emitMsg(it.localizedMessage ?: "Admin privileges required")
            }
        }
    }

    // --- Driver Actions ---
    fun claimDriverUid(driverId: String) {
        viewModelScope.launch {
            repository.claimDriverUid(driverId)
        }
    }

    fun toggleDriverAvailability(driverId: String, available: Boolean) {
        viewModelScope.launch {
            repository.toggleDriverAvailability(driverId, available)
                .onFailure { emitMsg(it.localizedMessage ?: "Failed to update availability") }
        }
    }

    fun driverMarkPickedUp(orderId: String) {
        viewModelScope.launch {
            repository.driverMarkPickedUp(orderId)
                .onSuccess {
                    emitMsg(if (language.value == AppLanguage.AR) "الطلب في الطريق الآن" else "Marked as Picked Up")
                }
                .onFailure { emitMsg(it.localizedMessage ?: "Failed to update status") }
        }
    }

    fun driverConfirmDelivered(orderId: String) {
        viewModelScope.launch {
            repository.driverConfirmDelivered(orderId)
                .onSuccess {
                    emitMsg(if (language.value == AppLanguage.AR) "تم تأكيد التسليم والتحصيل" else "Delivery & cash confirmed")
                }
                .onFailure { emitMsg(it.localizedMessage ?: "Failed to confirm delivery") }
        }
    }

    private fun emitMsg(msg: String) {
        _messages.tryEmit(msg)
    }

    private fun <T> Flow<T>.toUiState(label: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $label"))
            }

    private companion object {
        const val TAG = "AyaMarketVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}

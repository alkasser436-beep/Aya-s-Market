package com.example.data.remote

import android.content.Context
import com.example.shared.R
import com.example.data.model.AppCurrency
import com.example.data.model.DriverProfile
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.ProductCategory
import com.example.data.model.PromoBanner
import com.example.data.model.StoreConfig
import com.example.data.model.UserAddress
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AyaMarketRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    fun isCurrentUserBootstrappedAdmin(): Boolean = isAuthorizedOwner(auth.currentUser)

    companion object {
        val ADMIN_OWNER_EMAILS = setOf(
            "ayasmarket44@gmail.com"
        )

        fun isAuthorizedOwner(user: com.google.firebase.auth.FirebaseUser?): Boolean {
            if (user == null || !user.isEmailVerified) return false
            val normalizedEmail = user.email?.trim()?.lowercase() ?: return false
            return normalizedEmail in ADMIN_OWNER_EMAILS
        }
    }

    // --- Store Config (singleton main) ---
    fun observeStoreConfig(): Flow<StoreConfig> = flow {
        val uid = requireUserId()
        val docRef = db.collection("store_config").document("main")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        StoreConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: StoreConfig()
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, docRef.path, uid)
                    throw error
                }
        )
    }

    suspend fun updateStoreConfig(
        exchangeRateLbp: Double,
        deliveryFeeUsd: Double,
        freeDeliveryThresholdUsd: Double
    ): Result<Unit> {
        val path = "store_config/main"
        return try {
            val uid = requireUserId()
            require(exchangeRateLbp in 1000.0..10_000_000.0) { "Exchange rate out of range" }
            require(deliveryFeeUsd in 0.0..100.0) { "Delivery fee out of range" }
            require(freeDeliveryThresholdUsd in 0.0..1000.0) { "Threshold out of range" }
            val payload = mapOf(
                "exchangeRateLbp" to exchangeRateLbp,
                "deliveryFeeUsd" to deliveryFeeUsd,
                "freeDeliveryThresholdUsd" to freeDeliveryThresholdUsd,
                "storeNameEn" to "Aya Market",
                "storeNameAr" to "أيا ماركت",
                "supportPhone" to "+961 70 123 456",
                "updatedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("store_config").document("main").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Products ---
    fun observeProducts(): Flow<List<Product>> = flow {
        val uid = requireUserId()
        val path = "products"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Product::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.nameEn.lowercase() }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveProduct(product: Product, isNew: Boolean): Result<Unit> {
        var docPath = "products"
        return try {
            requireUserId()
            require(product.nameEn.trim().length in 1..100) { "English name must be 1-100 chars" }
            require(product.nameAr.trim().length in 1..100) { "Arabic name must be 1-100 chars" }
            require(product.priceUsd > 0.0 && product.priceUsd <= 10000.0) { "Invalid USD price" }
            require(product.discountPercent in 0L..90L) { "Discount must be 0-90%" }
            require(product.stock in 0L..100000L) { "Stock must be 0-100000" }

            val docRef = if (isNew || product.id.isBlank()) {
                db.collection("products").document()
            } else {
                db.collection("products").document(product.id)
            }
            docPath = docRef.path

            val baseFields = mutableMapOf<String, Any?>(
                "nameEn" to product.nameEn.trim().take(100),
                "nameAr" to product.nameAr.trim().take(100),
                "descriptionEn" to product.descriptionEn.trim().take(300),
                "descriptionAr" to product.descriptionAr.trim().take(300),
                "category" to product.parsedCategory.name,
                "priceUsd" to product.priceUsd,
                "discountPercent" to product.discountPercent,
                "stock" to product.stock,
                "unitEn" to product.unitEn.trim().ifBlank { "1 pc" }.take(30),
                "unitAr" to product.unitAr.trim().ifBlank { "١ حبة" }.take(30),
                "barcode" to product.barcode.trim().take(40),
                "imageKey" to product.imageKey.trim().take(500),
                "active" to product.active,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (isNew || product.id.isBlank()) {
                baseFields["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(baseFields.filterValues { it != null }).await()
            } else {
                docRef.update(baseFields.filterValues { it != null }).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val docPath = "products/$productId"
        return try {
            requireUserId()
            require(productId.isNotBlank()) { "Product ID must not be blank" }
            db.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- Promos ---
    fun observePromos(): Flow<List<PromoBanner>> = flow {
        val uid = requireUserId()
        val path = "promos"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PromoBanner::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun savePromo(promo: PromoBanner): Result<Unit> {
        var docPath = "promos"
        return try {
            requireUserId()
            require(promo.titleEn.trim().length in 1..100) { "Title EN required" }
            require(promo.titleAr.trim().length in 1..100) { "Title AR required" }
            val isNew = promo.id.isBlank()
            val docRef = if (isNew) db.collection("promos").document() else db.collection("promos").document(promo.id)
            docPath = docRef.path
            val payload = mutableMapOf<String, Any?>(
                "titleEn" to promo.titleEn.trim().take(100),
                "titleAr" to promo.titleAr.trim().take(100),
                "subtitleEn" to promo.subtitleEn.trim().take(200),
                "subtitleAr" to promo.subtitleAr.trim().take(200),
                "badgeText" to promo.badgeText.trim().take(30),
                "category" to promo.category,
                "bannerKey" to promo.bannerKey.trim().take(500),
                "active" to promo.active,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isNew) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload.filterValues { it != null }).await()
            } else {
                docRef.update(payload.filterValues { it != null }).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePromo(promoId: String): Result<Unit> {
        val path = "promos/$promoId"
        return try {
            requireUserId()
            db.collection("promos").document(promoId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Saved Addresses (User-scoped PII) ---
    fun observeAddresses(userId: String): Flow<List<UserAddress>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "User ID mismatch" }
        val path = "users/$userId/addresses"
        emitAll(
            db.collection("users").document(userId).collection("addresses")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserAddress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.defaultAddress }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveAddress(userId: String, address: UserAddress): Result<Unit> {
        var path = "users/$userId/addresses"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user address write" }
            require(address.label.trim().length in 1..40) { "Address label required" }
            require(address.addressLine.trim().length in 1..200) { "Address line required" }
            require(address.city.trim().length in 1..60) { "City required" }
            require(address.phone.trim().length in 3..30) { "Phone required" }

            val isNew = address.id.isBlank()
            val colRef = db.collection("users").document(userId).collection("addresses")
            val docRef = if (isNew) colRef.document() else colRef.document(address.id)
            path = docRef.path

            val payload = mutableMapOf<String, Any?>(
                "label" to address.label.trim().take(40),
                "addressLine" to address.addressLine.trim().take(200),
                "city" to address.city.trim().take(60),
                "phone" to address.phone.trim().take(30),
                "notes" to address.notes.trim().take(200),
                "latitude" to address.latitude.coerceIn(-90.0, 90.0),
                "longitude" to address.longitude.coerceIn(-180.0, 180.0),
                "defaultAddress" to address.defaultAddress,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isNew) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload.filterValues { it != null }).await()
            } else {
                docRef.update(payload.filterValues { it != null }).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(userId: String, addressId: String): Result<Unit> {
        val path = "users/$userId/addresses/$addressId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user address delete" }
            db.collection("users").document(userId).collection("addresses").document(addressId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Drivers ---
    fun observeDrivers(userId: String, isAdmin: Boolean): Flow<List<DriverProfile>> = flow {
        val uid = requireUserId()
        val path = "drivers"
        val query: Query = if (isAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("uid", userId)
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DriverProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun saveDriver(driver: DriverProfile): Result<Unit> {
        var path = "drivers"
        return try {
            requireUserId()
            require(driver.uid.trim().length in 1..128) { "Driver UID required" }
            require(driver.nameEn.trim().length in 1..80) { "Driver Name EN required" }
            require(driver.nameAr.trim().length in 1..80) { "Driver Name AR required" }
            require(driver.phone.trim().length in 3..30) { "Driver phone required" }

            val docId = driver.id.ifBlank { driver.uid.trim() }
            val docRef = db.collection("drivers").document(docId)
            path = docRef.path
            val existingSnap = docRef.get().await()

            val payload = mutableMapOf<String, Any?>(
                "uid" to driver.uid.trim().take(128),
                "nameEn" to driver.nameEn.trim().take(80),
                "nameAr" to driver.nameAr.trim().take(80),
                "phone" to driver.phone.trim().take(30),
                "vehicleLabel" to driver.vehicleLabel.trim().take(60),
                "available" to driver.available,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!existingSnap.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload.filterValues { it != null }).await()
            } else {
                docRef.update(payload.filterValues { it != null }).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleDriverAvailability(driverId: String, available: Boolean): Result<Unit> {
        val path = "drivers/$driverId"
        return try {
            requireUserId()
            db.collection("drivers").document(driverId).update(
                mapOf(
                    "available" to available,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Orders ---
    fun observeCustomerOrders(customerUid: String): Flow<List<Order>> = flow {
        val uid = requireUserId()
        check(uid == customerUid)
        val path = "orders"
        emitAll(
            db.collection(path)
                .whereEqualTo("customerUid", customerUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeDriverOrders(driverUid: String): Flow<List<Order>> = flow {
        val uid = requireUserId()
        check(uid == driverUid)
        val path = "orders"
        emitAll(
            db.collection(path)
                .whereEqualTo("driverUid", driverUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAllOrdersForAdmin(): Flow<List<Order>> = flow {
        val uid = requireUserId()
        val path = "orders"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Order::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun placeOrder(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        deliveryNotes: String,
        latitude: Double,
        longitude: Double,
        currency: AppCurrency,
        exchangeRateAtOrder: Double,
        paymentMethod: PaymentMethod,
        subtotalUsd: Double,
        deliveryFeeUsd: Double,
        items: List<OrderItem>
    ): Result<String> {
        var docPath = "orders"
        return try {
            val uid = requireUserId()
            require(customerName.trim().length in 1..80) { "Customer name required" }
            require(customerPhone.trim().length in 3..30) { "Phone number required" }
            require(deliveryAddress.trim().length in 1..250) { "Delivery address required" }
            require(items.isNotEmpty() && items.size <= 50) { "Order must contain 1-50 items" }

            val totalUsd = (subtotalUsd + deliveryFeeUsd).coerceAtLeast(0.01)
            val totalLbp = (totalUsd * exchangeRateAtOrder).coerceAtLeast(1000.0)
            val docRef = db.collection("orders").document()
            docPath = docRef.path

            val payload = mapOf(
                "customerUid" to uid,
                "customerName" to customerName.trim().take(80),
                "customerPhone" to customerPhone.trim().take(30),
                "deliveryAddress" to deliveryAddress.trim().take(250),
                "deliveryNotes" to deliveryNotes.trim().take(300),
                "latitude" to latitude.coerceIn(-90.0, 90.0),
                "longitude" to longitude.coerceIn(-180.0, 180.0),
                "currency" to currency.name,
                "exchangeRateAtOrder" to exchangeRateAtOrder,
                "paymentMethod" to paymentMethod.name,
                "subtotalUsd" to subtotalUsd,
                "deliveryFeeUsd" to deliveryFeeUsd,
                "totalUsd" to totalUsd,
                "totalLbp" to totalLbp,
                "status" to OrderStatus.PENDING.name,
                "items" to items.map { it.toMap() },
                "cashCollected" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateOrderStatus(orderId: String, newStatus: OrderStatus): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            db.collection("orders").document(orderId).update(
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminAssignDriver(
        orderId: String,
        driver: DriverProfile
    ): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            db.collection("orders").document(orderId).update(
                mapOf(
                    "driverUid" to driver.uid,
                    "driverName" to driver.nameEn,
                    "driverPhone" to driver.phone,
                    "status" to OrderStatus.PREPARING.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun driverMarkPickedUp(orderId: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            db.collection("orders").document(orderId).update(
                mapOf(
                    "status" to OrderStatus.OUT_FOR_DELIVERY.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun driverConfirmDelivered(orderId: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            db.collection("orders").document(orderId).update(
                mapOf(
                    "status" to OrderStatus.DELIVERED.name,
                    "cashCollected" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun customerCancelPendingOrder(orderId: String): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            db.collection("orders").document(orderId).update(
                mapOf(
                    "status" to OrderStatus.CANCELLED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Admin Starter Seed ---
    suspend fun seedStarterStoreIfAdmin(): Result<Unit> {
        val path = "products"
        return try {
            val uid = requireUserId()
            if (!isCurrentUserBootstrappedAdmin()) {
                return Result.failure(IllegalStateException("Admin privileges required"))
            }
            val currentUser = auth.currentUser!!
            val batch = db.batch()

            val configRef = db.collection("store_config").document("main")
            batch.set(
                configRef,
                mapOf(
                    "exchangeRateLbp" to 89500.0,
                    "deliveryFeeUsd" to 2.0,
                    "freeDeliveryThresholdUsd" to 25.0,
                    "storeNameEn" to "Aya Market",
                    "storeNameAr" to "أيا ماركت",
                    "supportPhone" to "+961 70 123 456",
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val starterProducts = listOf(
                Product(
                    id = "aya_tomatoes",
                    nameEn = "Organic Vine Tomatoes",
                    nameAr = "بندورة بلدية طازجة",
                    descriptionEn = "Sun-ripened Bekaa valley tomatoes picked daily.",
                    descriptionAr = "بندورة بلدية طازجة مقطوفة يومياً من سهل البقاع.",
                    category = ProductCategory.FRUITS_VEG.name,
                    priceUsd = 2.40,
                    discountPercent = 15L,
                    stock = 60L,
                    unitEn = "1 kg",
                    unitAr = "١ كغ",
                    barcode = "5281000100011",
                    imageKey = "tomatoes",
                    active = true
                ),
                Product(
                    id = "aya_avocados",
                    nameEn = "Hass Avocados (3 Pack)",
                    nameAr = "أفوكادو هاس (٣ حبات)",
                    descriptionEn = "Creamy ripe Hass avocados ready to slice.",
                    descriptionAr = "أفوكادو هاس طازج وكريمي جاهز للأكل.",
                    category = ProductCategory.FRUITS_VEG.name,
                    priceUsd = 4.20,
                    discountPercent = 0L,
                    stock = 35L,
                    unitEn = "3 pcs",
                    unitAr = "٣ حبات",
                    barcode = "5281000100028",
                    imageKey = "avocado",
                    active = true
                ),
                Product(
                    id = "aya_labneh",
                    nameEn = "Traditional Baladi Labneh",
                    nameAr = "لبنة بلدية أصلية",
                    descriptionEn = "Thick, creamy strained yogurt made from local farm milk.",
                    descriptionAr = "لبنة بلدية غنية وكريمية محضرة من الحليب الطازج.",
                    category = ProductCategory.DAIRY.name,
                    priceUsd = 3.75,
                    discountPercent = 10L,
                    stock = 45L,
                    unitEn = "500 g",
                    unitAr = "٥٠٠ غ",
                    barcode = "5281000200018",
                    imageKey = "labneh",
                    active = true
                ),
                Product(
                    id = "aya_halloumi",
                    nameEn = "Grilled Halloumi Cheese",
                    nameAr = "جبنة حلوم للشوي",
                    descriptionEn = "Authentic semi-hard halloumi cheese for breakfast or grilling.",
                    descriptionAr = "جبنة حلوم أصلية مثالية للفطور والشوي.",
                    category = ProductCategory.DAIRY.name,
                    priceUsd = 4.80,
                    discountPercent = 0L,
                    stock = 28L,
                    unitEn = "250 g",
                    unitAr = "٢٥٠ غ",
                    barcode = "5281000200025",
                    imageKey = "cheese",
                    active = true
                ),
                Product(
                    id = "aya_pita",
                    nameEn = "Artisan Lebanese Flatbread",
                    nameAr = "خبز عربي طازج",
                    descriptionEn = "Baked fresh every morning with golden stone-oven char.",
                    descriptionAr = "مخبوز طازجاً كل صباح على الطريقة التقليدية.",
                    category = ProductCategory.BAKERY.name,
                    priceUsd = 1.10,
                    discountPercent = 0L,
                    stock = 90L,
                    unitEn = "800 g",
                    unitAr = "٨٠٠ غ",
                    barcode = "5281000300015",
                    imageKey = "bread",
                    active = true
                ),
                Product(
                    id = "aya_croissant",
                    nameEn = "Butter Croissants (4 Pack)",
                    nameAr = "كرواسون بالزبدة (٤ قطع)",
                    descriptionEn = "Flaky French-butter croissants baked daily at Aya Bakery.",
                    descriptionAr = "كرواسون مقرمش بالزبدة الطازجة مخبوز يومياً.",
                    category = ProductCategory.BAKERY.name,
                    priceUsd = 3.50,
                    discountPercent = 20L,
                    stock = 24L,
                    unitEn = "4 pcs",
                    unitAr = "٤ قطع",
                    barcode = "5281000300022",
                    imageKey = "croissant",
                    active = true
                ),
                Product(
                    id = "aya_olive_oil",
                    nameEn = "Cold-Pressed Olive Oil",
                    nameAr = "زيت زيتون بكر ممتاز",
                    descriptionEn = "First cold-press South Lebanon extra virgin olive oil.",
                    descriptionAr = "زيت زيتون بكر ممتاز معصور على البارد من جنوب لبنان.",
                    category = ProductCategory.HOUSEHOLD.name,
                    priceUsd = 11.50,
                    discountPercent = 10L,
                    stock = 30L,
                    unitEn = "1 L",
                    unitAr = "١ لتر",
                    barcode = "5281000500019",
                    imageKey = "olive_oil",
                    active = true
                ),
                Product(
                    id = "aya_orange_juice",
                    nameEn = "Fresh Pressed Orange Juice",
                    nameAr = "عصير برتقال طبيعي طازج",
                    descriptionEn = "100% pure squeezed citrus with zero added sugar.",
                    descriptionAr = "عصير برتقال طبيعي ١٠٠٪ بدون أي سكر مضاف.",
                    category = ProductCategory.BEVERAGES.name,
                    priceUsd = 3.20,
                    discountPercent = 0L,
                    stock = 40L,
                    unitEn = "1 L",
                    unitAr = "١ لتر",
                    barcode = "5281000400012",
                    imageKey = "juice",
                    active = true
                ),
                Product(
                    id = "aya_sparkling",
                    nameEn = "Cedar Spring Sparkling Water",
                    nameAr = "مياه معدنية فوارة (٦ عبوات)",
                    descriptionEn = "Crisp mountain spring sparkling water multipack.",
                    descriptionAr = "مياه ينابيع جبلية فوارة منعشة.",
                    category = ProductCategory.BEVERAGES.name,
                    priceUsd = 3.90,
                    discountPercent = 15L,
                    stock = 55L,
                    unitEn = "6 x 330 ml",
                    unitAr = "٦ × ٣٣٠ مل",
                    barcode = "5281000400029",
                    imageKey = "water",
                    active = true
                ),
                Product(
                    id = "aya_detergent",
                    nameEn = "Botanical Dish & Home Cleaner",
                    nameAr = "سائل جلي وتنظيف نباتي",
                    descriptionEn = "Lemon-mint concentrated formula gentle on hands.",
                    descriptionAr = "تركيبة مركزة بالليمون والنعناع لطيفة على اليدين.",
                    category = ProductCategory.HOUSEHOLD.name,
                    priceUsd = 2.95,
                    discountPercent = 0L,
                    stock = 42L,
                    unitEn = "750 ml",
                    unitAr = "٧٥٠ مل",
                    barcode = "5281000500026",
                    imageKey = "cleaner",
                    active = true
                )
            )

            for (item in starterProducts) {
                val ref = db.collection("products").document(item.id)
                batch.set(
                    ref,
                    mapOf(
                        "nameEn" to item.nameEn,
                        "nameAr" to item.nameAr,
                        "descriptionEn" to item.descriptionEn,
                        "descriptionAr" to item.descriptionAr,
                        "category" to item.category,
                        "priceUsd" to item.priceUsd,
                        "discountPercent" to item.discountPercent,
                        "stock" to item.stock,
                        "unitEn" to item.unitEn,
                        "unitAr" to item.unitAr,
                        "barcode" to item.barcode,
                        "imageKey" to item.imageKey,
                        "active" to item.active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.set(
                db.collection("promos").document("promo_fresh_week"),
                mapOf(
                    "titleEn" to "Fresh Harvest Week",
                    "titleAr" to "أسبوع المحاصيل الطازجة",
                    "subtitleEn" to "Up to 20% off Bekaa vegetables & organic fruit",
                    "subtitleAr" to "خصومات لغاية ٢٠٪ على الخضار والفواكه الطازجة",
                    "badgeText" to "20% OFF",
                    "category" to ProductCategory.FRUITS_VEG.name,
                    "bannerKey" to "fresh_produce",
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("promos").document("promo_dairy_morning"),
                mapOf(
                    "titleEn" to "Morning Bakery & Labneh",
                    "titleAr" to "فطور الصباح والألبان",
                    "subtitleEn" to "Warm flatbreads & artisanal dairy delivered in 30 mins",
                    "subtitleAr" to "خبز ساخن وألبان بلدية تصلك خلال ٣٠ دقيقة",
                    "badgeText" to "FRESH",
                    "category" to ProductCategory.DAIRY.name,
                    "bannerKey" to "dairy_bakery",
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Register current admin as an active test driver too so Driver Mode works immediately
            batch.set(
                db.collection("drivers").document(uid),
                mapOf(
                    "uid" to uid,
                    "nameEn" to (currentUser.displayName ?: "Aya Express Driver"),
                    "nameAr" to (currentUser.displayName ?: "سائق أيا إكسبرس"),
                    "phone" to "+961 70 888 436",
                    "vehicleLabel" to "Green Van #01",
                    "available" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun logStreamError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}

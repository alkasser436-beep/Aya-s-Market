package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppLanguage { EN, AR }

enum class AppCurrency { USD, LBP }

enum class AppRole { CUSTOMER, ADMIN, DRIVER }

enum class ProductCategory {
    FRUITS_VEG,
    DAIRY,
    BAKERY,
    BEVERAGES,
    HOUSEHOLD;

    companion object {
        fun fromRaw(raw: String?): ProductCategory =
            entries.firstOrNull { it.name == raw } ?: FRUITS_VEG
    }
}

enum class OrderStatus {
    PENDING,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    companion object {
        fun fromRaw(raw: String?): OrderStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class PaymentMethod {
    CASH_ON_DELIVERY,
    WHISH_MONEY,
    CARD;

    companion object {
        fun fromRaw(raw: String?): PaymentMethod =
            entries.firstOrNull { it.name == raw } ?: CASH_ON_DELIVERY
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class StoreConfig(
    val exchangeRateLbp: Double = 89500.0,
    val deliveryFeeUsd: Double = 2.0,
    val freeDeliveryThresholdUsd: Double = 25.0,
    val storeNameEn: String = "Aya Market",
    val storeNameAr: String = "أيا ماركت",
    val supportPhone: String = "+961 70 123 456",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val nameEn: String = "",
    val nameAr: String = "",
    val descriptionEn: String = "",
    val descriptionAr: String = "",
    val category: String = ProductCategory.FRUITS_VEG.name,
    val priceUsd: Double = 0.0,
    val discountPercent: Long = 0L,
    val stock: Long = 0L,
    val unitEn: String = "1 pc",
    val unitAr: String = "١ حبة",
    val barcode: String = "",
    val imageKey: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedCategory: ProductCategory
        get() = ProductCategory.fromRaw(category)

    val effectivePriceUsd: Double
        get() {
            val pct = discountPercent.coerceIn(0L, 90L)
            return priceUsd * (1.0 - pct / 100.0)
        }

    fun localizedName(language: AppLanguage): String =
        if (language == AppLanguage.AR) nameAr.ifBlank { nameEn } else nameEn.ifBlank { nameAr }

    fun localizedDescription(language: AppLanguage): String =
        if (language == AppLanguage.AR) descriptionAr.ifBlank { descriptionEn } else descriptionEn.ifBlank { descriptionAr }

    fun localizedUnit(language: AppLanguage): String =
        if (language == AppLanguage.AR) unitAr.ifBlank { unitEn } else unitEn.ifBlank { unitAr }
}

@Keep
@IgnoreExtraProperties
data class PromoBanner(
    @DocumentId val id: String = "",
    val titleEn: String = "",
    val titleAr: String = "",
    val subtitleEn: String = "",
    val subtitleAr: String = "",
    val badgeText: String = "",
    val category: String = "ALL",
    val bannerKey: String = "fresh_produce",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun localizedTitle(language: AppLanguage): String =
        if (language == AppLanguage.AR) titleAr.ifBlank { titleEn } else titleEn.ifBlank { titleAr }

    fun localizedSubtitle(language: AppLanguage): String =
        if (language == AppLanguage.AR) subtitleAr.ifBlank { subtitleEn } else subtitleEn.ifBlank { subtitleAr }
}

@Keep
@IgnoreExtraProperties
data class UserAddress(
    @DocumentId val id: String = "",
    val label: String = "Home",
    val addressLine: String = "",
    val city: String = "Beirut",
    val phone: String = "",
    val notes: String = "",
    val latitude: Double = 33.8938,
    val longitude: Double = 35.5018,
    val defaultAddress: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DriverProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val nameEn: String = "",
    val nameAr: String = "",
    val phone: String = "",
    val vehicleLabel: String = "",
    val available: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun localizedName(language: AppLanguage): String =
        if (language == AppLanguage.AR) nameAr.ifBlank { nameEn } else nameEn.ifBlank { nameAr }
}

@Keep
@IgnoreExtraProperties
data class OrderItem(
    val productId: String = "",
    val nameEn: String = "",
    val nameAr: String = "",
    val quantity: Long = 1L,
    val unitPriceUsd: Double = 0.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "nameEn" to nameEn,
        "nameAr" to nameAr,
        "quantity" to quantity,
        "unitPriceUsd" to unitPriceUsd
    )

    fun localizedName(language: AppLanguage): String =
        if (language == AppLanguage.AR) nameAr.ifBlank { nameEn } else nameEn.ifBlank { nameAr }
}

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val customerUid: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val deliveryNotes: String = "",
    val latitude: Double = 33.8938,
    val longitude: Double = 35.5018,
    val currency: String = AppCurrency.USD.name,
    val exchangeRateAtOrder: Double = 89500.0,
    val paymentMethod: String = PaymentMethod.CASH_ON_DELIVERY.name,
    val subtotalUsd: Double = 0.0,
    val deliveryFeeUsd: Double = 0.0,
    val totalUsd: Double = 0.0,
    val totalLbp: Double = 0.0,
    val status: String = OrderStatus.PENDING.name,
    val items: List<OrderItem> = emptyList(),
    val driverUid: String = "",
    val driverName: String = "",
    val driverPhone: String = "",
    val cashCollected: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedStatus: OrderStatus
        get() = OrderStatus.fromRaw(status)

    val parsedCurrency: AppCurrency
        get() = AppCurrency.entries.firstOrNull { it.name == currency } ?: AppCurrency.USD

    val parsedPaymentMethod: PaymentMethod
        get() = PaymentMethod.fromRaw(paymentMethod)
}

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val lineTotalUsd: Double
        get() = product.effectivePriceUsd * quantity
}

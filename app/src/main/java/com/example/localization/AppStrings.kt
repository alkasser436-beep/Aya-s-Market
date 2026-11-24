package com.example.localization

import com.example.data.model.AppCurrency
import com.example.data.model.AppLanguage
import com.example.data.model.AppRole
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductCategory
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

object PriceFormatter {
    fun format(
        usdAmount: Double,
        currency: AppCurrency,
        exchangeRateLbp: Double,
        language: AppLanguage
    ): String {
        return when (currency) {
            AppCurrency.USD -> {
                val nf = NumberFormat.getNumberInstance(Locale.US).apply {
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }
                "$${nf.format(usdAmount)}"
            }
            AppCurrency.LBP -> {
                val rawLbp = usdAmount * exchangeRateLbp
                val roundedLbp = ((rawLbp / 1000.0).roundToLong() * 1000L).coerceAtLeast(0L)
                val nf = NumberFormat.getIntegerInstance(Locale.US)
                val suffix = if (language == AppLanguage.AR) "ل.ل." else "LBP"
                "${nf.format(roundedLbp)} $suffix"
            }
        }
    }

    fun formatLbpExact(lbpAmount: Double, language: AppLanguage): String {
        val nf = NumberFormat.getIntegerInstance(Locale.US)
        val suffix = if (language == AppLanguage.AR) "ل.ل." else "LBP"
        return "${nf.format(lbpAmount.roundToLong())} $suffix"
    }
}

object AppStrings {
    fun appName(lang: AppLanguage) = if (lang == AppLanguage.AR) "أيا ماركت" else "Aya Market"

    fun appTitleForRole(role: AppRole, lang: AppLanguage): String = when (role) {
        AppRole.CUSTOMER -> if (lang == AppLanguage.AR) "أيا ماركت" else "Aya Market"
        AppRole.ADMIN -> if (lang == AppLanguage.AR) "إدارة أيا ماركت" else "Aya Admin"
        AppRole.DRIVER -> if (lang == AppLanguage.AR) "سائق أيا ماركت" else "Aya Driver"
    }

    fun appSubtitleForRole(role: AppRole, lang: AppLanguage): String = when (role) {
        AppRole.CUSTOMER -> if (lang == AppLanguage.AR) "متجر التسوق بالليرة والدولار" else "Dual-Currency Fresh Supermarket"
        AppRole.ADMIN -> if (lang == AppLanguage.AR) "لوحة إدارة الأسعار والمخزون والطلبات" else "Live Rates, Inventory & Dispatch"
        AppRole.DRIVER -> if (lang == AppLanguage.AR) "تطبيق السائق والتوصيل المباشر" else "Express Route & Cash Collection"
    }

    fun tagline(lang: AppLanguage) = if (lang == AppLanguage.AR)
        "خضار طازجة، ألبان، ومستلزمات يومية لباب بيتك"
    else
        "Fresh produce, dairy & daily essentials delivered fast"

    fun signInWithGoogle(lang: AppLanguage) = if (lang == AppLanguage.AR) "تسجيل الدخول عبر Google" else "Sign in with Google"
    fun signOut(lang: AppLanguage) = if (lang == AppLanguage.AR) "تسجيل الخروج" else "Sign Out"
    fun roleLabel(role: AppRole, lang: AppLanguage): String = when (role) {
        AppRole.CUSTOMER -> if (lang == AppLanguage.AR) "المتسوق" else "Customer"
        AppRole.ADMIN -> if (lang == AppLanguage.AR) "الإدارة" else "Admin"
        AppRole.DRIVER -> if (lang == AppLanguage.AR) "السائق" else "Driver"
    }

    fun shopTab(lang: AppLanguage) = if (lang == AppLanguage.AR) "المتجر" else "Shop"
    fun cartTab(lang: AppLanguage) = if (lang == AppLanguage.AR) "السلة" else "Cart"
    fun ordersTab(lang: AppLanguage) = if (lang == AppLanguage.AR) "طلباتي" else "Orders"
    fun deliverTo(lang: AppLanguage) = if (lang == AppLanguage.AR) "التوصيل إلى" else "Deliver to"
    fun searchPlaceholder(lang: AppLanguage) = if (lang == AppLanguage.AR) "ابحث عن منتج أو باركود..." else "Search groceries or barcode..."
    fun allCategories(lang: AppLanguage) = if (lang == AppLanguage.AR) "الكل" else "All"

    fun categoryLabel(category: ProductCategory, lang: AppLanguage): String = when (category) {
        ProductCategory.FRUITS_VEG -> if (lang == AppLanguage.AR) "فواكه وخضار" else "Fruits & Veg"
        ProductCategory.DAIRY -> if (lang == AppLanguage.AR) "ألبان وأجبان" else "Dairy"
        ProductCategory.BAKERY -> if (lang == AppLanguage.AR) "مخبوزات" else "Bakery"
        ProductCategory.BEVERAGES -> if (lang == AppLanguage.AR) "مشروبات" else "Beverages"
        ProductCategory.HOUSEHOLD -> if (lang == AppLanguage.AR) "منزلية" else "Household"
    }

    fun statusLabel(status: OrderStatus, lang: AppLanguage): String = when (status) {
        OrderStatus.PENDING -> if (lang == AppLanguage.AR) "قيد الانتظار" else "Pending"
        OrderStatus.PREPARING -> if (lang == AppLanguage.AR) "قيد التجهيز" else "Preparing"
        OrderStatus.OUT_FOR_DELIVERY -> if (lang == AppLanguage.AR) "في الطريق" else "Out for Delivery"
        OrderStatus.DELIVERED -> if (lang == AppLanguage.AR) "تم التوصيل" else "Delivered"
        OrderStatus.CANCELLED -> if (lang == AppLanguage.AR) "ملغي" else "Cancelled"
    }

    fun paymentLabel(method: PaymentMethod, lang: AppLanguage): String = when (method) {
        PaymentMethod.CASH_ON_DELIVERY -> if (lang == AppLanguage.AR) "الدفع عند الاستلام" else "Cash on Delivery"
        PaymentMethod.WHISH_MONEY -> if (lang == AppLanguage.AR) "ويش موني (Whish Money)" else "Whish Money"
        PaymentMethod.CARD -> if (lang == AppLanguage.AR) "بطاقة مصرفية" else "Card"
    }

    fun addToCart(lang: AppLanguage) = if (lang == AppLanguage.AR) "أضف للسلة" else "Add to Cart"
    fun outOfStock(lang: AppLanguage) = if (lang == AppLanguage.AR) "نفذت الكمية" else "Out of Stock"
    fun scanBarcode(lang: AppLanguage) = if (lang == AppLanguage.AR) "مسح الباركود" else "Scan Barcode"
    fun emptyCartTitle(lang: AppLanguage) = if (lang == AppLanguage.AR) "سلة التسوق فارغة" else "Your basket is empty"
    fun emptyCartSubtitle(lang: AppLanguage) = if (lang == AppLanguage.AR) "أضف بعض المنتجات الطازجة للبدء" else "Add fresh groceries to get started"
    fun browseMarket(lang: AppLanguage) = if (lang == AppLanguage.AR) "تصفح المتجر" else "Browse Market"
    fun checkoutStep1(lang: AppLanguage) = if (lang == AppLanguage.AR) "١. السلة" else "1. Cart"
    fun checkoutStep2(lang: AppLanguage) = if (lang == AppLanguage.AR) "٢. العنوان" else "2. Address"
    fun checkoutStep3(lang: AppLanguage) = if (lang == AppLanguage.AR) "٣. الدفع" else "3. Payment"
    fun next(lang: AppLanguage) = if (lang == AppLanguage.AR) "التالي" else "Next"
    fun back(lang: AppLanguage) = if (lang == AppLanguage.AR) "رجوع" else "Back"
    fun placeOrder(lang: AppLanguage) = if (lang == AppLanguage.AR) "تأكيد الطلب" else "Place Order"
    fun subtotal(lang: AppLanguage) = if (lang == AppLanguage.AR) "المجموع الفرعي" else "Subtotal"
    fun deliveryFee(lang: AppLanguage) = if (lang == AppLanguage.AR) "رسوم التوصيل" else "Delivery Fee"
    fun free(lang: AppLanguage) = if (lang == AppLanguage.AR) "مجاني" else "Free"
    fun total(lang: AppLanguage) = if (lang == AppLanguage.AR) "الإجمالي" else "Total"
    fun callDriver(lang: AppLanguage) = if (lang == AppLanguage.AR) "اتصال بالسائق" else "Call Driver"
    fun whatsappDriver(lang: AppLanguage) = if (lang == AppLanguage.AR) "واتساب" else "WhatsApp"
    fun openMaps(lang: AppLanguage) = if (lang == AppLanguage.AR) "خرائط غوغل" else "Google Maps"
    fun openWaze(lang: AppLanguage) = if (lang == AppLanguage.AR) "ويز (Waze)" else "Waze"
    fun exchangeRate(lang: AppLanguage) = if (lang == AppLanguage.AR) "سعر الصرف" else "Exchange Rate"
    fun updateRate(lang: AppLanguage) = if (lang == AppLanguage.AR) "تحديث السعر" else "Update Rate"
    fun seedCatalog(lang: AppLanguage) = if (lang == AppLanguage.AR) "تعبئة كتالوج تجريبي" else "Load Starter Catalog"
    fun printInvoice(lang: AppLanguage) = if (lang == AppLanguage.AR) "طباعة الفاتورة" else "Print Invoice"
    fun markPickedUp(lang: AppLanguage) = if (lang == AppLanguage.AR) "تم استلام الطلب - في الطريق" else "Mark Picked Up"
    fun confirmDelivered(lang: AppLanguage) = if (lang == AppLanguage.AR) "تأكيد التحصيل والتسليم" else "Confirm Cash & Delivered"
}

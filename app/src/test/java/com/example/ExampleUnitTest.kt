package com.example

import com.example.data.model.AppCurrency
import com.example.data.model.AppLanguage
import com.example.data.model.Product
import com.example.localization.PriceFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `discount price calculation works accurately`() {
        val product = Product(
            id = "p1",
            nameEn = "Tomatoes",
            priceUsd = 10.0,
            discountPercent = 20L
        )
        assertEquals(8.0, product.effectivePriceUsd, 0.001)
    }

    @Test
    fun `price formatter converts USD to LBP with exchange rate`() {
        val formattedUsd = PriceFormatter.format(
            usdAmount = 12.5,
            currency = AppCurrency.USD,
            exchangeRateLbp = 89500.0,
            language = AppLanguage.EN
        )
        val formattedLbp = PriceFormatter.format(
            usdAmount = 10.0,
            currency = AppCurrency.LBP,
            exchangeRateLbp = 89500.0,
            language = AppLanguage.EN
        )
        assertEquals("$12.50", formattedUsd)
        assertEquals("895,000 LBP", formattedLbp)
    }
}

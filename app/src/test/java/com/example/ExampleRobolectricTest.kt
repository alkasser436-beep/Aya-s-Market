package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aya Market", appName)
  }

  @Test
  fun `price formatter converts USD to LBP accurately`() {
    val lbp = com.example.localization.PriceFormatter.format(
      usdAmount = 10.0,
      currency = com.example.data.model.AppCurrency.LBP,
      exchangeRateLbp = 89500.0,
      language = com.example.data.model.AppLanguage.EN
    )
    assertEquals("895,000 LBP", lbp)
  }
}

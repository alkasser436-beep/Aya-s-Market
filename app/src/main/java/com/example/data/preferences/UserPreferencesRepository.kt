package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppCurrency
import com.example.data.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ayaPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aya_market_prefs"
)

data class UserPrefs(
    val language: AppLanguage = AppLanguage.EN,
    val currency: AppCurrency = AppCurrency.USD
)

class UserPreferencesRepository(private val context: Context) {
    private val keyLanguage = stringPreferencesKey("app_language")
    private val keyCurrency = stringPreferencesKey("app_currency")

    val prefsFlow: Flow<UserPrefs> = context.ayaPreferencesDataStore.data.map { prefs ->
        val lang = AppLanguage.entries.firstOrNull { it.name == prefs[keyLanguage] } ?: AppLanguage.EN
        val curr = AppCurrency.entries.firstOrNull { it.name == prefs[keyCurrency] } ?: AppCurrency.USD
        UserPrefs(language = lang, currency = curr)
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.ayaPreferencesDataStore.edit { prefs ->
            prefs[keyLanguage] = language.name
        }
    }

    suspend fun setCurrency(currency: AppCurrency) {
        context.ayaPreferencesDataStore.edit { prefs ->
            prefs[keyCurrency] = currency.name
        }
    }
}

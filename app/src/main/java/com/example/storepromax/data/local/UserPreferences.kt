package com.example.storepromax.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    fun saveRememberInfo(email: String, isRemember: Boolean) {
        prefs.edit()
            .putString("saved_email", if (isRemember) email else "")
            .putBoolean("is_remember", isRemember)
            .apply()
    }
    fun getSavedEmail(): String {
        return prefs.getString("saved_email", "") ?: ""
    }
    fun isRemembered(): Boolean {
        return prefs.getBoolean("is_remember", false)
    }
}
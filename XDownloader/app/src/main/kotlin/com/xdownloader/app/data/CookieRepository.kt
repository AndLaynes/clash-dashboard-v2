package com.xdownloader.app.data

import android.content.SharedPreferences
import android.util.Log
import com.xdownloader.app.domain.model.Cookie
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "XDL"
private const val KEY_AUTH_TOKEN = "auth_token"
private const val KEY_CT0 = "ct0"

@Singleton
class CookieRepository @Inject constructor(
    private val prefs: SharedPreferences
) {
    fun saveCookie(cookie: Cookie) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, cookie.authToken)
            .putString(KEY_CT0, cookie.ct0)
            .apply()
        Log.d(TAG, "Cookie saved")
    }

    fun loadCookie(): Cookie? {
        val authToken = prefs.getString(KEY_AUTH_TOKEN, null)
        val ct0 = prefs.getString(KEY_CT0, null)
        return if (authToken != null && ct0 != null) Cookie(authToken, ct0) else null
    }

    fun hasCookie(): Boolean = loadCookie() != null

    fun clearCookie() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_CT0)
            .apply()
        Log.d(TAG, "Cookie cleared")
    }
}

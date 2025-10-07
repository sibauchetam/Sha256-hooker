package com.example.xposeddigest

import de.robv.android.xposed.XSharedPreferences

object SettingsProvider {
    private var prefs: XSharedPreferences? = null

    // Call this once from the hook entry point
    fun loadPreferences() {
        if (prefs == null) {
            // The package name here should be the module's own package name
            prefs = XSharedPreferences("com.example.xposeddigest", MainActivity.PREFS_NAME)
            prefs?.makeWorldReadable() // Necessary for some Android versions
        } else {
            prefs?.reload()
        }
    }

    val isCryptoHooksEnabled: Boolean
        get() {
            loadPreferences()
            return prefs?.getBoolean(MainActivity.KEY_CRYPTO_HOOKS_ENABLED, true) ?: true
        }

    val isNetworkHooksEnabled: Boolean
        get() {
            loadPreferences()
            return prefs?.getBoolean(MainActivity.KEY_NETWORK_HOOKS_ENABLED, true) ?: true
        }
}
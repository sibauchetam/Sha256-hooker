package com.example.xposeddigest

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class MainHook : IXposedHookLoadPackage {

    private val targetPackage = "com.schools_by.app_parent"

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != targetPackage) {
            return
        }

        XposedBridge.log("XposedDigest: Successfully hooked into target package: ${lpparam.packageName}")

        // Load settings from the module's UI
        SettingsProvider.loadPreferences()

        if (SettingsProvider.isCryptoHooksEnabled) {
            XposedBridge.log("XposedDigest: Crypto hooks enabled, initializing.")
            CryptoHooks.init(lpparam)
        } else {
            XposedBridge.log("XposedDigest: Crypto hooks disabled by user.")
        }

        if (SettingsProvider.isNetworkHooksEnabled) {
            XposedBridge.log("XposedDigest: Network hooks enabled, initializing.")
            NetworkHooks.init(lpparam)
        } else {
            XposedBridge.log("XposedDigest: Network hooks disabled by user.")
        }
    }
}
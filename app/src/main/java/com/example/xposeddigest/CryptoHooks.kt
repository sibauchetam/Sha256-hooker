package com.example.xposeddigest

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.security.Key
import java.security.MessageDigest

object CryptoHooks {

    fun init(lpparam: LoadPackageParam) {
        val classLoader = lpparam.classLoader
        runCatching {
            hookMessageDigest(classLoader)
        }.onFailure {
            XposedBridge.log("XposedDigest: Failed to hook MessageDigest: ${it.message}")
        }
        runCatching {
            hookMac(classLoader)
        }.onFailure {
            XposedBridge.log("XposedDigest: Failed to hook Mac: ${it.message}")
        }
    }

    private fun hookMessageDigest(classLoader: ClassLoader) {
        // Hook for: public byte[] digest()
        XposedHelpers.findAndHookMethod("java.security.MessageDigest", classLoader, "digest", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val digest = param.thisObject as MessageDigest
                XposedBridge.log("XposedDigest: MessageDigest.digest() CALLED")
                XposedBridge.log("  Algorithm: ${digest.algorithm}")
                XposedBridge.log("  Output (Hex): ${bytesToHex(param.result as ByteArray)}")
            }
        })

        // Hook for: public byte[] digest(byte[] input)
        XposedHelpers.findAndHookMethod("java.security.MessageDigest", classLoader, "digest", ByteArray::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val digest = param.thisObject as MessageDigest
                val input = param.args[0] as ByteArray
                XposedBridge.log("XposedDigest: MessageDigest.digest(byte[]) CALLED")
                XposedBridge.log("  Algorithm: ${digest.algorithm}")
                XposedBridge.log("  Input (Hex): ${bytesToHex(input)}")
                XposedBridge.log("  Input (UTF-8): ${runCatching { String(input) }.getOrDefault("[Non-UTF8 data]")}")
            }
            override fun afterHookedMethod(param: MethodHookParam) {
                XposedBridge.log("  Output (Hex): ${bytesToHex(param.result as ByteArray)}")
            }
        })

        // Hook for: public void update(byte[] input)
        XposedHelpers.findAndHookMethod("java.security.MessageDigest", classLoader, "update", ByteArray::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val digest = param.thisObject as MessageDigest
                val input = param.args[0] as ByteArray
                XposedBridge.log("XposedDigest: MessageDigest.update(byte[]) CALLED")
                XposedBridge.log("  Algorithm: ${digest.algorithm}")
                XposedBridge.log("  Input Chunk (Hex): ${bytesToHex(input)}")
                XposedBridge.log("  Input Chunk (UTF-8): ${runCatching { String(input) }.getOrDefault("[Non-UTF8 data]")}")
            }
        })
    }

    private fun hookMac(classLoader: ClassLoader) {
        // Hook for: public final void init(Key key)
        XposedHelpers.findAndHookMethod("javax.crypto.Mac", classLoader, "init", java.security.Key::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val mac = param.thisObject as javax.crypto.Mac
                val key = param.args[0] as Key
                XposedBridge.log("XposedDigest: Mac.init(Key) CALLED")
                XposedBridge.log("  Algorithm: ${mac.algorithm}")
                key.encoded?.let {
                    XposedBridge.log("  Key (Hex): ${bytesToHex(it)}")
                } ?: XposedBridge.log("  Key: (Not directly accessible)")
            }
        })

        // Hook for: public final byte[] doFinal()
        XposedHelpers.findAndHookMethod("javax.crypto.Mac", classLoader, "doFinal", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val mac = param.thisObject as javax.crypto.Mac
                XposedBridge.log("XposedDigest: Mac.doFinal() CALLED")
                XposedBridge.log("  Algorithm: ${mac.algorithm}")
                XposedBridge.log("  Output (Hex): ${bytesToHex(param.result as ByteArray)}")
            }
        })

        // Hook for: public final byte[] doFinal(byte[] input)
        XposedHelpers.findAndHookMethod("javax.crypto.Mac", classLoader, "doFinal", ByteArray::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val mac = param.thisObject as javax.crypto.Mac
                val input = param.args[0] as ByteArray
                XposedBridge.log("XposedDigest: Mac.doFinal(byte[]) CALLED")
                XposedBridge.log("  Algorithm: ${mac.algorithm}")
                XposedBridge.log("  Input (Hex): ${bytesToHex(input)}")
                XposedBridge.log("  Input (UTF-8): ${runCatching { String(input) }.getOrDefault("[Non-UTF8 data]")}")
            }
            override fun afterHookedMethod(param: MethodHookParam) {
                XposedBridge.log("  Output (Hex): ${bytesToHex(param.result as ByteArray)}")
            }
        })

        // Hook for: public final void update(byte[] input)
        XposedHelpers.findAndHookMethod("javax.crypto.Mac", classLoader, "update", ByteArray::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val mac = param.thisObject as javax.crypto.Mac
                val input = param.args[0] as ByteArray
                XposedBridge.log("XposedDigest: Mac.update(byte[]) CALLED")
                XposedBridge.log("  Algorithm: ${mac.algorithm}")
                XposedBridge.log("  Input Chunk (Hex): ${bytesToHex(input)}")
                XposedBridge.log("  Input Chunk (UTF-8): ${runCatching { String(input) }.getOrDefault("[Non-UTF8 data]")}")
            }
        })
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
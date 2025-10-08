package com.example.xposeddigest

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.ByteArrayOutputStream
import java.security.Key
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac

object CryptoHooks {

    private val messageDigestBuffers = ConcurrentHashMap<MessageDigest, ByteArrayOutputStream>()
    private val macBuffers = ConcurrentHashMap<Mac, ByteArrayOutputStream>()

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
        val mdClass = XposedHelpers.findClass("java.security.MessageDigest", classLoader)

        // Hook for: public void update(byte[] input)
        XposedHelpers.findAndHookMethod(mdClass, "update", ByteArray::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val digest = param.thisObject as MessageDigest
                val input = param.args[0] as ByteArray
                val buffer = messageDigestBuffers.computeIfAbsent(digest) { ByteArrayOutputStream() }
                buffer.write(input)
            }
        })

        // Hook for: public void update(byte[] input, int offset, int len)
        XposedHelpers.findAndHookMethod(mdClass, "update", ByteArray::class.java, Int::class.java, Int::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val digest = param.thisObject as MessageDigest
                val input = param.args[0] as ByteArray
                val offset = param.args[1] as Int
                val len = param.args[2] as Int
                val buffer = messageDigestBuffers.computeIfAbsent(digest) { ByteArrayOutputStream() }
                buffer.write(input, offset, len)
            }
        })

        // Hook for: public byte[] digest()
        XposedHelpers.findAndHookMethod(mdClass, "digest", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val digest = param.thisObject as MessageDigest
                val output = param.result as ByteArray
                val buffer = messageDigestBuffers.remove(digest)

                XposedBridge.log("XposedDigest: MessageDigest.digest() CALLED")
                XposedBridge.log("  Algorithm: ${digest.algorithm}")
                if (buffer != null) {
                    val input = buffer.toByteArray()
                    XposedBridge.log("  Input (Hex): ${bytesToHex(input)}")
                    XposedBridge.log("  Input (UTF-8): ${runCatching { String(input) }.getOrDefault("[Non-UTF8 data]")}")
                }
                XposedBridge.log("  Output (Hex): ${bytesToHex(output)}")
            }
        })

        // Hook for: public byte[] digest(byte[] input)
        XposedHelpers.findAndHookMethod(mdClass, "digest", ByteArray::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val digest = param.thisObject as MessageDigest
                val input = param.args[0] as ByteArray
                // This method is final and calls digest(), so we just need to log the input
                XposedBridge.log("XposedDigest: MessageDigest.digest(byte[]) CALLED")
                XposedBridge.log("  Algorithm: ${digest.algorithm}")
                XposedBridge.log("  Input (Hex): ${bytesToHex(input)}")
                XposedBridge.log("  Input (UTF-8): ${runCatching { String(input) }.getOrDefault("[Non-UTF8 data]")}")
            }
            override fun afterHookedMethod(param: MethodHookParam) {
                XposedBridge.log("  Output (Hex): ${bytesToHex(param.result as ByteArray)}")
            }
        })

        // Hook for: public void reset()
        XposedHelpers.findAndHookMethod(mdClass, "reset", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                messageDigestBuffers.remove(param.thisObject as MessageDigest)
            }
        })
    }

    private fun hookMac(classLoader: ClassLoader) {
        val macClass = XposedHelpers.findClass("javax.crypto.Mac", classLoader)

        // Hook for: public final void init(Key key)
        XposedHelpers.findAndHookMethod(macClass, "init", Key::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val mac = param.thisObject as Mac
                val key = param.args[0] as Key
                macBuffers.remove(mac) // Reset buffer on re-initialization
                XposedBridge.log("XposedDigest: Mac.init(Key) CALLED")
                XposedBridge.log("  Algorithm: ${mac.algorithm}")
                key.encoded?.let {
                    XposedBridge.log("  Key (Hex): ${bytesToHex(it)}")
                } ?: XposedBridge.log("  Key: (Not directly accessible)")
            }
        })

        // Hook for: public final void update(byte[] input)
        XposedHelpers.findAndHookMethod(macClass, "update", ByteArray::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val mac = param.thisObject as Mac
                val input = param.args[0] as ByteArray
                val buffer = macBuffers.computeIfAbsent(mac) { ByteArrayOutputStream() }
                buffer.write(input)
            }
        })

        // Hook for: public final byte[] doFinal()
        XposedHelpers.findAndHookMethod(macClass, "doFinal", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val mac = param.thisObject as Mac
                val output = param.result as ByteArray
                val buffer = macBuffers.remove(mac)

                XposedBridge.log("XposedDigest: Mac.doFinal() CALLED")
                XposedBridge.log("  Algorithm: ${mac.algorithm}")
                if (buffer != null) {
                    val input = buffer.toByteArray()
                    XposedBridge.log("  Input (Hex): ${bytesToHex(input)}")
                    XposedBridge.log("  Input (UTF-8): ${runCatching { String(input) }.getOrDefault("[Non-UTF8 data]")}")
                }
                XposedBridge.log("  Output (Hex): ${bytesToHex(output)}")
            }
        })

        // Hook for: public final byte[] doFinal(byte[] input)
        XposedHelpers.findAndHookMethod(macClass, "doFinal", ByteArray::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val mac = param.thisObject as Mac
                val input = param.args[0] as ByteArray
                val buffer = macBuffers.remove(mac) // Previous buffer is now irrelevant

                XposedBridge.log("XposedDigest: Mac.doFinal(byte[]) CALLED")
                XposedBridge.log("  Algorithm: ${mac.algorithm}")
                if (buffer != null) {
                    XposedBridge.log("  NOTE: Prior buffered input from update() calls was discarded.")
                }
                XposedBridge.log("  Input (Hex): ${bytesToHex(input)}")
                XposedBridge.log("  Input (UTF-8): ${runCatching { String(input) }.getOrDefault("[Non-UTF8 data]")}")
            }
            override fun afterHookedMethod(param: MethodHookParam) {
                XposedBridge.log("  Output (Hex): ${bytesToHex(param.result as ByteArray)}")
            }
        })

        // Hook for: public void reset()
        XposedHelpers.findAndHookMethod(macClass, "reset", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                macBuffers.remove(param.thisObject as Mac)
            }
        })
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
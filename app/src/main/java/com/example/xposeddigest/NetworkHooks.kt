package com.example.xposeddigest

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.net.HttpURLConnection
import java.net.URL

object NetworkHooks {

    private const val DIGEST_HEADER = "x-digest"

    fun init(lpparam: LoadPackageParam) {
        val classLoader = lpparam.classLoader
        runCatching {
            hookOkHttp(classLoader)
        }.onFailure {
            // Log only if you expect OkHttp, otherwise it's just noise
            // XposedBridge.log("XposedDigest: OkHttp not found or failed to hook: ${it.message}")
        }
        runCatching {
            hookHttpURLConnection(classLoader)
        }.onFailure {
            XposedBridge.log("XposedDigest: Failed to hook HttpURLConnection: ${it.message}")
        }
    }

    private fun hookOkHttp(classLoader: ClassLoader) {
        val requestBuilderClass = XposedHelpers.findClassIfExists("okhttp3.Request\$Builder", classLoader) ?: return

        val headerHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val name = param.args[0] as? String
                if (name.equals(DIGEST_HEADER, ignoreCase = true)) {
                    val value = param.args[1] as? String
                    val builder = param.thisObject
                    // The 'url' field in the builder is an HttpUrl object. toString() gives the full URL.
                    val url = XposedHelpers.getObjectField(builder, "url")
                    val method = XposedHelpers.getObjectField(builder, "method") as String

                    XposedBridge.log("XposedDigest: OkHttp: Intercepted '$DIGEST_HEADER' header addition")
                    XposedBridge.log("  URL: $url")
                    XposedBridge.log("  Method: $method")
                    XposedBridge.log("  Header: $name: $value")
                    XposedBridge.log("  Stack Trace:\n${Log.getStackTraceString(Throwable())}")
                }
            }
        }

        // Hook: public Builder addHeader(String name, String value)
        XposedHelpers.findAndHookMethod(requestBuilderClass, "addHeader", String::class.java, String::class.java, headerHook)

        // Hook: public Builder header(String name, String value)
        XposedHelpers.findAndHookMethod(requestBuilderClass, "header", String::class.java, String::class.java, headerHook)
    }

    private fun hookHttpURLConnection(classLoader: ClassLoader) {
        val urlConnectionClass = XposedHelpers.findClassIfExists("javax.net.ssl.HttpsURLConnection", classLoader)
            ?: XposedHelpers.findClassIfExists("java.net.HttpURLConnection", classLoader)
            ?: return

        // Hook: public void setRequestProperty(String key, String value)
        XposedHelpers.findAndHookMethod(urlConnectionClass, "setRequestProperty", String::class.java, String::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val key = param.args[0] as String
                if (key.equals(DIGEST_HEADER, ignoreCase = true)) {
                    val value = param.args[1] as String
                    val connection = param.thisObject as HttpURLConnection

                    XposedBridge.log("XposedDigest: HttpURLConnection: Found '$DIGEST_HEADER' header")
                    XposedBridge.log("  URL: ${connection.url}")
                    XposedBridge.log("  Method: ${connection.requestMethod}")
                    XposedBridge.log("  Header: $key: $value")
                    XposedBridge.log("  Stack Trace:\n${Log.getStackTraceString(Throwable())}")
                }
            }
        })
    }
}
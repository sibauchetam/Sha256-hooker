package com.example.xposeddigest

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // This is the preferences file that the hooks will read.
        // On modern Android, MODE_WORLD_READABLE is deprecated and we rely on LSPosed's mechanisms
        // to make this file accessible to the target process.
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val switchCrypto = findViewById<SwitchMaterial>(R.id.switch_crypto_hooks)
        val switchNetwork = findViewById<SwitchMaterial>(R.id.switch_network_hooks)
        val exportButton = findViewById<Button>(R.id.button_export_logs)

        // Load current settings
        switchCrypto.isChecked = prefs.getBoolean(KEY_CRYPTO_HOOKS_ENABLED, true)
        switchNetwork.isChecked = prefs.getBoolean(KEY_NETWORK_HOOKS_ENABLED, true)

        // Save settings on change
        switchCrypto.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_CRYPTO_HOOKS_ENABLED, isChecked).apply()
        }

        switchNetwork.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_NETWORK_HOOKS_ENABLED, isChecked).apply()
        }

        exportButton.setOnClickListener {
            Toast.makeText(this, "Log export not implemented yet.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val PREFS_NAME = "XposedDigest_Settings"
        const val KEY_CRYPTO_HOOKS_ENABLED = "crypto_hooks_enabled"
        const val KEY_NETWORK_HOOKS_ENABLED = "network_hooks_enabled"
    }
}
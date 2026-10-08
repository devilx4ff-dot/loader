package com.gameloader.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // no persistence — always show the license screen first
        startActivity(Intent(this, LicenseActivity::class.java))
        finish()
    }
}
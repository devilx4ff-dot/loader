package com.gameloader.app.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.gameloader.app.R
import com.gameloader.app.core.LicenseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LicenseActivity : AppCompatActivity() {

    private lateinit var etLicense: EditText
    private lateinit var btnActivate: AppCompatButton
    private lateinit var btnReset: TextView
    private lateinit var btnToggleVisibility: ImageView
    private lateinit var logo: ImageView
    private lateinit var licenseCard: LinearLayout
    private lateinit var activateSpinner: ProgressBar
    private var hidden = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)

        logo = findViewById(R.id.logo)
        etLicense = findViewById(R.id.etLicense)
        btnActivate = findViewById(R.id.btnActivate)
        btnReset = findViewById(R.id.btnReset)
        btnToggleVisibility = findViewById(R.id.btnToggleVisibility)
        licenseCard = findViewById(R.id.licenseCard)
        activateSpinner = findViewById(R.id.activateSpinner)

        // entrance animation
        logo.alpha = 0f
        logo.translationY = -30f
        logo.animate().alpha(1f).translationY(0f).setDuration(500)
            .setInterpolator(DecelerateInterpolator()).start()

        licenseCard.alpha = 0f
        licenseCard.translationY = 60f
        licenseCard.animate().alpha(1f).translationY(0f)
            .setStartDelay(150).setDuration(600)
            .setInterpolator(DecelerateInterpolator()).start()

        btnToggleVisibility.setOnClickListener {
            hidden = !hidden
            val pos = etLicense.selectionEnd
            etLicense.inputType = if (hidden)
                android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            else
                android.text.InputType.TYPE_CLASS_TEXT
            etLicense.setSelection(pos)
        }

        btnActivate.setOnClickListener { doActivate() }

        btnReset.setOnClickListener {
            // nothing to reset — the key was never saved. just clear the field.
            etLicense.setText("")
            Toast.makeText(this, "Field cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun doActivate() {
        val key = etLicense.text.toString().trim()
        if (key.isEmpty()) {
            shake(etLicense)
            Toast.makeText(this, "Enter a license key", Toast.LENGTH_SHORT).show()
            return
        }

        btnActivate.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90)
            .withEndAction {
                btnActivate.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }.start()

        setLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            val result = LicenseManager.activate(this@LicenseActivity, key)
            withContext(Dispatchers.Main) {
                setLoading(false)
                if (result.success) {
                    btnActivate.text = "ACTIVATED"
                    btnActivate.animate().alpha(0.8f).setDuration(200).start()
                    etLicense.postDelayed({
                        startActivity(Intent(this@LicenseActivity, HomeActivity::class.java))
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()
                    }, 500)
                } else {
                    shake(etLicense)
                    Toast.makeText(this@LicenseActivity,
                        result.message.ifEmpty { "Activation failed" },
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        btnActivate.isEnabled = !loading
        btnActivate.text = if (loading) "" else "ACTIVATE NOW"
        activateSpinner.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun shake(view: View) {
        val anim = ObjectAnimator.ofFloat(view, "translationX",
            0f, 18f, -18f, 14f, -14f, 8f, -8f, 0f)
        anim.duration = 400
        anim.start()
    }
}
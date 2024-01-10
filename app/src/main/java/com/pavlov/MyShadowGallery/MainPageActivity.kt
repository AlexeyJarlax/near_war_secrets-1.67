package com.pavlov.MyShadowGallery

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.widget.Button
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.pavlov.MyShadowGallery.security.ThreeStepsActivity
import com.pavlov.MyShadowGallery.util.AppPreferencesKeys

import com.pavlov.MyShadowGallery.util.ThemeManager

class MainPageActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var threeStepsActivity: ThreeStepsActivity
    private var simblPass = "🏳️"
    private var simblMimic = "🏳️"
    private var simblEncryption = "🏳️"
    private var text = "🏳️"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_page)
        sharedPreferences =
            getSharedPreferences(AppPreferencesKeys.PREFS_NAME, Context.MODE_PRIVATE)

//        ThemeManager.applyTheme(this) // Применяю ночную тему
        var backgroundView = findViewById<ImageView>(R.id.background_image)
        backgroundView.setImageResource(ThemeManager.applyUserSwitch(this))
        val buttonLogin = findViewById<Button>(R.id.button_login)
        val buttonSearch = findViewById<Button>(R.id.button_search)
        val buttonGallery = findViewById<Button>(R.id.button_gallery)
        val buttonMedialib = findViewById<Button>(R.id.button_item_loader)
        val buttonStorageLog = findViewById<Button>(R.id.button_storage_log)
        val buttonSettings = findViewById<Button>(R.id.button_settings)
        threeStepsActivity = ThreeStepsActivity()


        buttonLogin.setOnClickListener {
            goToThreeStepsActivity()
        }

        buttonSearch.setOnClickListener {
            val displayIntent = Intent(this, SearchActivity::class.java)
            startActivity(displayIntent)
        }

        buttonGallery.setOnClickListener { // ItemLoaderActivity в режиме Галереи
            val displayIntent = Intent(this, ItemLoaderActivity::class.java)
            // Добавляем флаг, чтобы ItemLoaderActivity запускалась в усеченной версии
            displayIntent.putExtra("hideConstraintLayout", true)
            // Запускаем ItemLoaderActivity
            startActivity(displayIntent)
        }

        buttonMedialib.setOnClickListener {
            val displayIntent = Intent(this, ItemLoaderActivity::class.java)
            startActivity(displayIntent)
        }

        buttonStorageLog.setOnClickListener {
            val displayIntent = Intent(this, StorageLogActivity::class.java)
            startActivity(displayIntent)
        }

        buttonSettings.setOnClickListener {
            val displayIntent = Intent(this, SettingsActivity::class.java)
            startActivity(displayIntent)
        }
        locker()
    } // конец OnCreate

    fun goToThreeStepsActivity() {
        val displayIntent = Intent(this, ThreeStepsActivity::class.java)
        startActivity(displayIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var backgroundView = findViewById<ImageView>(R.id.background_image)
        backgroundView.setImageResource(ThemeManager.applyUserSwitch(this))
    }

    private fun locker() {
        val passKey =
            sharedPreferences.getBoolean(AppPreferencesKeys.KEY_EXIST_OF_PASSWORD, false)
        val isExistsOfEncryptionKey =
            sharedPreferences.getBoolean(AppPreferencesKeys.KEY_EXIST_OF_ENCRYPTION_KLUCHIK, false)
        val mimikKey =
            sharedPreferences.getBoolean(AppPreferencesKeys.KEY_EXIST_OF_MIMICRY, false)

        var keySimbl = findViewById<Button>(R.id.button_login)

        simblPass = if (passKey) {
            "🔐"
        } else {
            ""
        }
        simblMimic = if (mimikKey) {
            "🕶️"
        } else {
            ""
        }
        simblEncryption = if (isExistsOfEncryptionKey) {
            "🔏"
        } else {
            ""
        }
        text = "${simblPass}${simblMimic}${simblEncryption}"
        keySimbl.text = text

        if (text.length < 2) {
            keySimbl.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.kototeka_thumb_color))
            keySimbl.text = "🏳️"
        } else if (text.length < 4) {
            keySimbl.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.kototeka_thumb_color2))
        } else if (text.length < 6) {
            keySimbl.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.yp_blue_light))
        } else if (text.length < 8) {
            keySimbl.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.yp_blue))
        }
    }
}
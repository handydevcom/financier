package com.handydev.financier

import android.content.Context
import android.preference.PreferenceManager
import androidx.multidex.MultiDexApplication
import com.handydev.financier.activity.PreferencesActivity
import com.handydev.financier.utils.MyPreferences
import org.acra.ACRA
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import java.lang.RuntimeException

open class FinancierApplication : MultiDexApplication() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        val acraPrefExists = PreferenceManager.getDefaultSharedPreferences(this).contains("acra.enable")
        if(!acraPrefExists) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("acra.enable", false).apply()
        }

        initAcra {
            //core configuration:
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            httpSender {
                uri = BuildConfig.ACRA_URI
                basicAuthLogin = BuildConfig.ACRA_LOGIN
                basicAuthPassword = BuildConfig.ACRA_PASSWORD
                httpMethod = HttpSender.Method.POST
            }
        }
        //ACRA.DEV_LOGGING = true
    }
}
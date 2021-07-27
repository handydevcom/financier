package com.handydev.financier

import android.content.Context
import androidx.multidex.MultiDexApplication
import org.acra.ACRA
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import java.lang.RuntimeException

open class FinancierApplication : MultiDexApplication() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

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
        ACRA.DEV_LOGGING = true
    }
}
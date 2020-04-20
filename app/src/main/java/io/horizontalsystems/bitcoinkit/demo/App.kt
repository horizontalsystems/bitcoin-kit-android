package io.horizontalsystems.bitcoinkit.demo

import android.app.Application
import com.facebook.stetho.Stetho

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Enable debug bridge
        Stetho.initializeWithDefaults(this)

        instance = this
    }

    companion object {
        lateinit var instance: App
            private set
    }

}

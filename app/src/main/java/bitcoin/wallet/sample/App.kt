package bitcoin.wallet.sample

import android.app.Application
import bitcoin.wallet.kit.WalletKit
import com.squareup.leakcanary.LeakCanary

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return
        }
        LeakCanary.install(this)

        WalletKit.init(this)
    }

}

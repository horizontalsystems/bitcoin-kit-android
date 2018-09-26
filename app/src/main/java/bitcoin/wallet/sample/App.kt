package bitcoin.wallet.sample

import android.app.Application
import bitcoin.wallet.kit.WalletKit

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        WalletKit.init(this)
    }

}

package bitcoin.wallet.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import bitcoin.wallet.kit.WalletKit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Run wallet kit
        WalletKit.init(this)

        val walletKit = WalletKit(listOf("used", "ugly", "meat", "glad", "balance", "divorce", "inner", "artwork", "hire", "invest", "already", "piano"))
        walletKit.start()

        setContentView(R.layout.activity_main)
    }
}

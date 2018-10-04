package bitcoin.wallet.sample

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import bitcoin.wallet.kit.WalletKit
import bitcoin.wallet.kit.models.TransactionInfo

class MainViewModel : ViewModel(), WalletKit.Listener {

    enum class State {
        STARTED, STOPPED
    }

    val transactions = MutableLiveData<List<TransactionInfo>>()
    val balance = MutableLiveData<Long>()
    val lastBlockHeight = MutableLiveData<Int>()
    val status = MutableLiveData<State>()

    private var started = false
        set(value) {
            field = value
            status.value = (if (value) State.STARTED else State.STOPPED)
        }

    private var walletKit: WalletKit

    init {
        val words = listOf("used", "ugly", "meat", "glad", "balance", "divorce", "inner", "artwork", "hire", "invest", "already", "piano")
        walletKit = WalletKit(words)

        walletKit.listener = this

        transactions.value = walletKit.transactions.asReversed()
        balance.value = walletKit.balance
        lastBlockHeight.value = walletKit.lastBlockHeight

        started = false
    }

    fun start() {
        if (started) return
        started = true

        walletKit.start()
    }

    fun receiveAddress(): String {
        return walletKit.receiveAddress()
    }

    override fun transactionsUpdated(walletKit: WalletKit, inserted: List<TransactionInfo>, updated: List<TransactionInfo>, deleted: List<Int>) {
        transactions.value = walletKit.transactions.asReversed()
    }

    override fun balanceUpdated(walletKit: WalletKit, balance: Long) {
        this.balance.value = balance
    }

    override fun lastBlockHeightUpdated(walletKit: WalletKit, lastBlockHeight: Int) {
        this.lastBlockHeight.value = lastBlockHeight
    }

    override fun progressUpdated(walletKit: WalletKit, progress: Double) {
        TODO("not implemented")
    }
}

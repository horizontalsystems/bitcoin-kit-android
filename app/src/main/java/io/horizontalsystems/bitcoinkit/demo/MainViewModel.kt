package io.horizontalsystems.bitcoinkit.demo

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.bitcoinkit.BitcoinKit.KitState
import io.horizontalsystems.bitcoinkit.models.BlockInfo
import io.horizontalsystems.bitcoinkit.models.TransactionInfo

class MainViewModel : ViewModel(), BitcoinKit.Listener {

    enum class State {
        STARTED, STOPPED
    }

    val transactions = MutableLiveData<List<TransactionInfo>>()
    val balance = MutableLiveData<Long>()
    val lastBlockHeight = MutableLiveData<Int>()
    val progress = MutableLiveData<Double>()
    val status = MutableLiveData<State>()
    val networkName: String

    private var started = false
        set(value) {
            field = value
            status.value = (if (value) State.STARTED else State.STOPPED)
        }

    private var bitcoinKit: BitcoinKit

    init {
        val words = listOf("used", "ugly", "meat", "glad", "balance", "divorce", "inner", "artwork", "hire", "invest", "already", "piano")
        val networkType = BitcoinKit.NetworkType.TestNet

        bitcoinKit = BitcoinKit(words, networkType)
        bitcoinKit.listener = this

        networkName = networkType.name
        balance.value = bitcoinKit.balance
        transactions.value = bitcoinKit.transactions.sortedBy { it.blockHeight?.times(-1) }
        lastBlockHeight.value = bitcoinKit.lastBlockHeight
        progress.value = 0.0

        started = false
    }

    fun start() {
        if (started) return
        started = true

        bitcoinKit.start()
    }

    fun receiveAddress(): String {
        return bitcoinKit.receiveAddress()
    }

    fun send(address: String, amount: Int) {
        bitcoinKit.send(address, amount)
    }

    fun fee(value: Int, address: String? = null): Int {
        return bitcoinKit.fee(value, address)
    }

    fun showDebugInfo() {
        bitcoinKit.showDebugInfo()
    }

    //
    // BitcoinKit Listener implementations
    //
    override fun onTransactionsUpdate(bitcoinKit: BitcoinKit, inserted: List<TransactionInfo>, updated: List<TransactionInfo>, deleted: List<Int>) {
        transactions.value = this.bitcoinKit.transactions.sortedBy { it.blockHeight?.times(-1) }
    }

    override fun onBalanceUpdate(bitcoinKit: BitcoinKit, balance: Long) {
        this.balance.value = balance
    }

    override fun onLastBlockInfoUpdate(bitcoinKit: BitcoinKit, blockInfo: BlockInfo) {
        this.lastBlockHeight.value = blockInfo.height
    }

    override fun onKitStateUpdate(bitcoinKit: BitcoinKit, state: KitState) {
        when (state) {
            is KitState.Synced -> {
                this.progress.postValue(1.0)
            }
            is KitState.Syncing -> {
                this.progress.postValue(state.progress)
            }
            is KitState.NotSynced -> {
                this.progress.postValue(0.0)
            }
        }
    }
}

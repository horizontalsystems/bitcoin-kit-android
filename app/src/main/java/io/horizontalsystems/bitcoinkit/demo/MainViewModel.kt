package io.horizontalsystems.bitcoinkit.demo

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.bitcoinkit.BitcoinKit.KitState
import io.horizontalsystems.bitcoinkit.BitcoinKitBuilder
import io.horizontalsystems.bitcoinkit.models.BlockInfo
import io.horizontalsystems.bitcoinkit.models.TransactionInfo
import io.reactivex.disposables.CompositeDisposable

class MainViewModel : ViewModel(), BitcoinKit.Listener {

    enum class State {
        STARTED, STOPPED
    }

    val transactions = MutableLiveData<List<TransactionInfo>>()
    val balance = MutableLiveData<Long>()
    val lastBlockHeight = MutableLiveData<Int>()
    val state = MutableLiveData<KitState>()
    val status = MutableLiveData<State>()
    val networkName: String
    private val disposables = CompositeDisposable()

    private var started = false
        set(value) {
            field = value
            status.value = (if (value) State.STARTED else State.STOPPED)
        }

    private var bitcoinKit: BitcoinKit

    init {
        val words = listOf("used", "ugly", "meat", "glad", "balance", "divorce", "inner", "artwork", "hire", "invest", "already", "piano")

        bitcoinKit = BitcoinKitBuilder()
                .setContext(App.instance)
                .setWords(words)
                .setNetworkType(BitcoinKit.NetworkType.TestNet)
                .setWalletId("wallet-id")
                .build()

        bitcoinKit.listener = this

        networkName = BitcoinKit.NetworkType.TestNet.name
        balance.value = bitcoinKit.balance

        bitcoinKit.transactions().subscribe { txList: List<TransactionInfo> ->
            transactions.value = txList.sortedByDescending { it.blockHeight }
        }.let {
            disposables.add(it)
        }

        lastBlockHeight.value = bitcoinKit.lastBlockInfo?.height ?: 0
        state.value = KitState.NotSynced

        started = false
    }

    fun start() {
        if (started) return
        started = true

        bitcoinKit.start()
    }

    fun clear() {
        bitcoinKit.clear()
    }

    fun receiveAddress(): String {
        return bitcoinKit.receiveAddress()
    }

    fun send(address: String, amount: Long) {
        bitcoinKit.send(address, amount)
    }

    fun fee(value: Long, address: String? = null): Long {
        return bitcoinKit.fee(value, address)
    }

    fun showDebugInfo() {
        bitcoinKit.showDebugInfo()
    }

    //
    // BitcoinKit Listener implementations
    //
    override fun onTransactionsUpdate(bitcoinKit: BitcoinKit, inserted: List<TransactionInfo>, updated: List<TransactionInfo>) {
        bitcoinKit.transactions().subscribe { txList: List<TransactionInfo> ->
            transactions.postValue(txList.sortedByDescending { it.blockHeight })
        }.let {
            disposables.add(it)
        }
    }

    override fun onTransactionsDelete(hashes: List<String>) {
    }

    override fun onBalanceUpdate(bitcoinKit: BitcoinKit, balance: Long) {
        this.balance.postValue(balance)
    }

    override fun onLastBlockInfoUpdate(bitcoinKit: BitcoinKit, blockInfo: BlockInfo) {
        this.lastBlockHeight.postValue(blockInfo.height)
    }

    override fun onKitStateUpdate(bitcoinKit: BitcoinKit, state: KitState) {
        this.state.postValue(state)
    }
}

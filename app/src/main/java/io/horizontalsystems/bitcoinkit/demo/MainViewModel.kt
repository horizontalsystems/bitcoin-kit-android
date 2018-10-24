package io.horizontalsystems.bitcoinkit.demo

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bitcoinkit.WalletKit
import io.horizontalsystems.bitcoinkit.models.BlockInfo
import io.horizontalsystems.bitcoinkit.models.TransactionInfo

class MainViewModel : ViewModel(), WalletKit.Listener {

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

    private var walletKit: WalletKit

    init {
        val words = listOf("used", "ugly", "meat", "glad", "balance", "divorce", "inner", "artwork", "hire", "invest", "already", "piano")
        val networkType = WalletKit.NetworkType.TestNet

        walletKit = WalletKit(words, networkType)
        walletKit.listener = this

        networkName = networkType.name
        balance.value = walletKit.balance
        transactions.value = walletKit.transactions.asReversed()
        lastBlockHeight.value = walletKit.lastBlockHeight
        progress.value = 0.0

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

    fun send(address: String, amount: Int) {
        walletKit.send(address, amount)
    }

    override fun transactionsUpdated(walletKit: WalletKit, inserted: List<TransactionInfo>, updated: List<TransactionInfo>, deleted: List<Int>) {
        transactions.value = walletKit.transactions.asReversed()
    }

    override fun balanceUpdated(walletKit: WalletKit, balance: Long) {
        this.balance.value = balance
    }

    override fun lastBlockInfoUpdated(walletKit: WalletKit, lastBlockInfo: BlockInfo) {
        this.lastBlockHeight.value = lastBlockInfo.height
    }

    override fun progressUpdated(walletKit: WalletKit, progress: Double) {
        this.progress.value = progress
    }
}

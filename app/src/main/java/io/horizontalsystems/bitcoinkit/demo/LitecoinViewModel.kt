package io.horizontalsystems.bitcoinkit.demo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.litecoinkit.LitecoinKit

class LitecoinViewModel : ViewModel(), LitecoinKit.Listener {

    enum class Status { STARTED, STOPPED }

    val syncState = MutableLiveData<BitcoinCore.KitState>()
    val balance = MutableLiveData<BalanceInfo>()
    val lastBlock = MutableLiveData<BlockInfo>()
    val status = MutableLiveData<Status>()
    val receiveAddress = MutableLiveData<String>()
    val mwebAddress = MutableLiveData<String>()
    val errorLiveData = MutableLiveData<String>()

    private lateinit var litecoinKit: LitecoinKit
    private val walletId = "LitecoinMwebDemo"

    fun init() {
        // Same test seed used in MainViewModel — swap for your own wallet in production.
        val words = "used ugly meat glad balance divorce inner artwork hire invest already piano".split(" ")
        litecoinKit = LitecoinKit(
            context = App.instance,
            words = words,
            passphrase = "",
            walletId = walletId,
            networkType = LitecoinKit.NetworkType.MainNet,
            syncMode = BitcoinCore.SyncMode.Api(),
            purpose = io.horizontalsystems.hdwalletkit.HDWallet.Purpose.BIP84
        )
        litecoinKit.listener = this

        balance.value = litecoinKit.balance
        lastBlock.value = litecoinKit.lastBlockInfo
        syncState.value = litecoinKit.syncState
        status.value = Status.STOPPED

        receiveAddress.value = litecoinKit.receiveAddress()
        mwebAddress.value = litecoinKit.mwebAddress() ?: "(not available)"
    }

    fun start() {
        if (status.value == Status.STARTED) return
        litecoinKit.start()
        status.value = Status.STARTED
    }

    fun stop() {
        litecoinKit.stop()
        status.value = Status.STOPPED
    }

    fun clear() {
        litecoinKit.stop()
        LitecoinKit.clear(App.instance, LitecoinKit.NetworkType.MainNet, walletId)
        init()
    }

    override fun onCleared() {
        super.onCleared()
        litecoinKit.stop()
    }

    // LitecoinKit.Listener

    override fun onBalanceUpdate(balance: BalanceInfo) {
        this.balance.postValue(balance)
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        lastBlock.postValue(blockInfo)
    }

    override fun onKitStateUpdate(state: BitcoinCore.KitState) {
        syncState.postValue(state)
    }

    override fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>) = Unit

    override fun onTransactionsDelete(hashes: List<String>) = Unit
}
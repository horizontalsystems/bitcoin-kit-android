package io.horizontalsystems.bitcoinkit.demo

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bitcoincore.BitcoinCore.KitState
import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelectorError
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.hodler.HodlerData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.hodler.LockTimeInterval
import io.reactivex.disposables.CompositeDisposable

class MainViewModel : ViewModel(), BitcoinKit.Listener {

    enum class State {
        STARTED, STOPPED
    }

    val transactions = MutableLiveData<List<TransactionInfo>>()
    val balance = MutableLiveData<BalanceInfo>()
    val lastBlock = MutableLiveData<BlockInfo>()
    val state = MutableLiveData<KitState>()
    val status = MutableLiveData<State>()
    lateinit var networkName: String
    private val disposables = CompositeDisposable()

    private var started = false
        set(value) {
            field = value
            status.value = (if (value) State.STARTED else State.STOPPED)
        }

    private lateinit var bitcoinKit: BitcoinKit

    private val walletId = "MyWallet"
    private val networkType = BitcoinKit.NetworkType.MainNet

    init {
        init()
    }

    private fun init() {
         val words = "used ugly meat glad balance divorce inner artwork hire invest already piano".split(" ")

        bitcoinKit = BitcoinKit(App.instance, words, walletId, networkType, bip = Bip.BIP44)

        bitcoinKit.listener = this

        networkName = bitcoinKit.networkName
        balance.value = bitcoinKit.balance

        bitcoinKit.transactions().subscribe { txList: List<TransactionInfo> ->
            transactions.value = txList
        }.let {
            disposables.add(it)
        }

        lastBlock.value = bitcoinKit.lastBlockInfo
        state.value = KitState.NotSynced

        started = false
    }

    fun start() {
        if (started) return
        started = true

        bitcoinKit.start()
    }

    fun clear() {
        bitcoinKit.stop()
        BitcoinKit.clear(App.instance, networkType, walletId)

        init()
    }


    fun showDebugInfo() {
        bitcoinKit.showDebugInfo()
    }

    //
    // BitcoinKit Listener implementations
    //
    override fun onTransactionsUpdate(inserted: List<TransactionInfo>, updated: List<TransactionInfo>) {
        bitcoinKit.transactions().subscribe { txList: List<TransactionInfo> ->
            transactions.postValue(txList)
        }.let {
            disposables.add(it)
        }
    }

    override fun onTransactionsDelete(hashes: List<String>) {
    }

    override fun onBalanceUpdate(balance: BalanceInfo) {
        this.balance.postValue(balance)
    }

    override fun onLastBlockInfoUpdate(blockInfo: BlockInfo) {
        this.lastBlock.postValue(blockInfo)
    }

    override fun onKitStateUpdate(state: KitState) {
        this.state.postValue(state)
    }

    val receiveAddressLiveData = MutableLiveData<String>()
    val feeLiveData = MutableLiveData<Long>()
    val errorLiveData = MutableLiveData<String>()
    val addressLiveData = MutableLiveData<String>()
    val amountLiveData = MutableLiveData<Long>()

    var amount: Long? = null
        set(value) {
            field = value
            updateFee()
        }

    var address: String? = null
        set(value) {
            field = value
            updateFee()
        }

    var feePriority: FeePriority = FeePriority.Medium
        set(value) {
            field = value
            updateFee()
        }

    var timeLockInterval: LockTimeInterval? = null
        set(value) {
            field = value
            updateFee()
        }

    fun onReceiveClick() {
        receiveAddressLiveData.value = bitcoinKit.receiveAddress()
    }

    fun onSendClick() {
        if (address.isNullOrBlank()) {
            errorLiveData.value = "Send address cannot be blank"
        } else if (amount == null) {
            errorLiveData.value = "Send amount cannot be blank"
        } else {
            try {
                bitcoinKit.send(address!!, amount!!, feeRate = feePriority.feeRate, pluginData = getPluginData())

                amountLiveData.value = null
                feeLiveData.value = null
                addressLiveData.value = null
                errorLiveData.value = "Transaction sent"
            } catch (e: Exception) {
                errorLiveData.value = when (e) {
                    is UnspentOutputSelectorError.InsufficientUnspentOutputs,
                    is UnspentOutputSelectorError.EmptyUnspentOutputs -> "Insufficient balance"
                    else -> e.message ?: "Failed to send transaction (${e.javaClass.name})"
                }
            }
        }
    }

    fun onMaxClick() {
        val spendableBalance = bitcoinKit.balance.spendable
        val fee = try {
            fee(spendableBalance, address)
        } catch (e: UnspentOutputSelectorError.InsufficientUnspentOutputs) {
            e.fee
        } catch (e: Exception) {
            0L
        }

        amountLiveData.value = spendableBalance - fee
    }

    private fun updateFee() {
        try {
            feeLiveData.value = amount?.let {
                fee(it, address)
            }
        } catch (e: Exception) {
            errorLiveData.value = e.message ?: e.javaClass.simpleName
        }
    }

    private fun fee(value: Long, address: String? = null): Long {
        return bitcoinKit.fee(value, address, feeRate = feePriority.feeRate, pluginData = getPluginData())
    }

    private fun getPluginData(): MutableMap<Byte, IPluginData> {
        val pluginData = mutableMapOf<Byte, IPluginData>()
        timeLockInterval?.let {
            pluginData[HodlerPlugin.id] = HodlerData(it)
        }
        return pluginData
    }
}

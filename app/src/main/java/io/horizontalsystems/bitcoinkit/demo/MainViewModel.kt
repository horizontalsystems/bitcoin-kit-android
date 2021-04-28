package io.horizontalsystems.bitcoinkit.demo

import android.text.SpannableStringBuilder
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCore.KitState
import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException
import io.horizontalsystems.bitcoincore.managers.SendValueErrors
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.hodler.HodlerData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.hodler.LockTimeInterval
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_send_receive.*

class MainViewModel : ViewModel(), BitcoinKit.Listener {

    enum class State {
        STARTED, STOPPED
    }

    val transactions = MutableLiveData<List<TransactionInfo>>()
    val balance = MutableLiveData<BalanceInfo>()
    val lastBlock = MutableLiveData<BlockInfo>()
    val state = MutableLiveData<KitState>()
    val status = MutableLiveData<State>()
    val transactionRaw = MutableLiveData<String>()
    val statusInfo = MutableLiveData<Map<String, Any>>()
    lateinit var networkName: String
    private val disposables = CompositeDisposable()

    private var started = false
        set(value) {
            field = value
            status.value = (if (value) State.STARTED else State.STOPPED)
        }

    private lateinit var bitcoinKit: BitcoinKit

    private val walletId = "MyWallet"
    private val networkType = BitcoinKit.NetworkType.TestNet
    private val syncMode = BitcoinCore.SyncMode.Api()
    private val bip = Bip.BIP44

    fun init() {
        //TODO create unique seed phrase,perhaps using shared preferences?
        val words = "used ugly meat glad balance divorce inner artwork hire invest already piano".split(" ")
        val passphrase = ""

        bitcoinKit = BitcoinKit(App.instance, words, passphrase, walletId, networkType, syncMode = syncMode, bip = bip)

        bitcoinKit.listener = this

        networkName = bitcoinKit.networkName
        balance.value = bitcoinKit.balance

        bitcoinKit.transactions().subscribe { txList: List<TransactionInfo> ->
            transactions.value = txList
        }.let {
            disposables.add(it)
        }

        lastBlock.value = bitcoinKit.lastBlockInfo
        state.value = bitcoinKit.syncState

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

    fun showStatusInfo() {
        statusInfo.postValue(bitcoinKit.statusInfo())
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
        when {
            address.isNullOrBlank() -> {
                errorLiveData.value = "Send address cannot be blank"
            }
            amount == null -> {
                errorLiveData.value = "Send amount cannot be blank"
            }
            else -> {
                try {
                  val transaction =  bitcoinKit.send(address!!, amount!!, feeRate = feePriority.feeRate, sortType = TransactionDataSortType.Shuffle, pluginData = getPluginData())

                    amountLiveData.value = null
                    feeLiveData.value = null
                    addressLiveData.value = null
                    errorLiveData.value = "Transaction sent ${transaction.header.serializedTxInfo}"
                } catch (e: Exception) {
                    errorLiveData.value = when (e) {
                        is SendValueErrors.InsufficientUnspentOutputs,
                        is SendValueErrors.EmptyOutputs -> "Insufficient balance"
                        is AddressFormatException -> "Could not Format Address"
                        else -> e.message ?: "Failed to send transaction (${e.javaClass.name})"
                    }

                }
            }
        }
    }

    fun onMaxClick() {
        try {
            amountLiveData.value = bitcoinKit.maximumSpendableValue(address, feePriority.feeRate, getPluginData())
        } catch (e: Exception) {
            amountLiveData.value = 0
            errorLiveData.value = when (e) {

                is SendValueErrors.Dust,
                is SendValueErrors.EmptyOutputs -> "You need at least ${e.message} satoshis to make an transaction"
                is AddressFormatException -> "Could not Format Address"
                else -> e.message ?: "Maximum could not be calculated"
            }
        }
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

    fun onRawTransactionClick(transactionHash: String) {
        transactionRaw.postValue(bitcoinKit.getRawTransaction(transactionHash))
    }
}

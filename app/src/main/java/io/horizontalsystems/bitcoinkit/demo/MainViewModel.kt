package io.horizontalsystems.bitcoinkit.demo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCore.KitState
import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException
import io.horizontalsystems.bitcoincore.extensions.toReversedByteArray
import io.horizontalsystems.bitcoincore.managers.SendValueErrors
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoinkit.BitcoinKit
import io.horizontalsystems.hodler.HodlerData
import io.horizontalsystems.hodler.HodlerPlugin
import io.horizontalsystems.hodler.LockTimeInterval
import io.reactivex.disposables.CompositeDisposable

class MainViewModel : ViewModel(), BitcoinKit.Listener {

    enum class State {
        STARTED, STOPPED
    }

    private var transactionFilterType: TransactionFilterType? = null
    val types = listOf(null) + TransactionFilterType.values()

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

    private val walletId = "SyncDemoWallet42"
    private val networkType = BitcoinKit.NetworkType.MainNet
    private val syncMode = BitcoinCore.SyncMode.Api()
    private val bip = Bip.BIP84

    fun init() {
        //TODO create unique seed phrase,perhaps using shared preferences?
        val words = "supply decline tomato achieve skull sell rent fold mystery cricket rhythm buddy".split(" ")
        val words2 = "bid hotel witness assume call limb wink buzz soup picture scatter such".split(" ")
        val passphrase = ""

        val testNetBlock : Block = Block(
            BlockHeader(777150464, "00000000000000391d536135debbe17550888f5ed921ffd4e3243452d86362b6".toReversedByteArray(),
                "386ff8d145c1b2bce660e4b7be6461e722e1b1ebf807867d0e877f2a704cf60f".toReversedByteArray(), 1661281593,
                423228096, 3304394013,
                "0000000000000014766b22567d0f359c10836d8fd2a38d0d0d316bdbc2ab150f".toReversedByteArray()), 2343559)

        val mainnetBlock : Block = Block(
            BlockHeader(545259520, "00000000000000000004f6be1c3bd65be9fcba704030db756ac9d48ec5d616c2".toReversedByteArray(),
                "3ed8d4d65c18c0907679b9219ccaeb3f639182258a588abc1f72c7d22a8d25d1".toReversedByteArray(), 1662125853,
                386471456, 1474792728,
                "000000000000000000080cbc2c01be5b174b3b4fbc333498614a9a24a6d0e6ea".toReversedByteArray()), 752290)


        bitcoinKit = BitcoinKit(App.instance, words2, passphrase, walletId, networkType, syncMode = syncMode, bip = bip, block = mainnetBlock)
        bitcoinKit.listener = this
        networkName = bitcoinKit.networkName
        balance.value = bitcoinKit.balance

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
        setTransactionFilterType(transactionFilterType)
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

    fun setTransactionFilterType(transactionFilterType: TransactionFilterType?) {
        this.transactionFilterType = transactionFilterType

        bitcoinKit.transactions(type = transactionFilterType).subscribe { txList: List<TransactionInfo> ->
            transactions.postValue(txList)
        }.let {
            disposables.add(it)
        }
    }
}

package io.horizontalsystems.bitcoinkit.demo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.litecoinkit.LitecoinKit
import io.horizontalsystems.litecoinmwebkit.LitecoinMwebKit
import io.horizontalsystems.litecoinmwebkit.MwebSyncState
import io.horizontalsystems.litecoinmwebkit.MwebTransactionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Drives a Litecoin MWEB wallet next to a plain Litecoin wallet on the same
 * seed, so peg-ins (Litecoin to MWEB) can be exercised from one screen.
 */
class MwebViewModel : ViewModel(), LitecoinKit.Listener {

    val networkType = LitecoinKit.NetworkType.TestNet
    private val words = "used ugly meat glad balance divorce inner artwork hire invest already piano".split(" ")
    private val walletId = "MwebDemo"
    private val feeRate = 10

    val syncState = MutableLiveData<String>()
    val balance = MutableLiveData<String>()
    val litecoinBalance = MutableLiveData<String>()
    val litecoinState = MutableLiveData<String>()
    val receiveAddress = MutableLiveData<String>()
    val transactions = MutableLiveData<String>()
    val message = MutableLiveData<String>()
    val started = MutableLiveData(false)

    private lateinit var mwebKit: LitecoinMwebKit
    private lateinit var litecoinKit: LitecoinKit

    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.US)

    init {
        createKits()
    }

    private fun createKits() {
        mwebKit = LitecoinMwebKit(App.instance, words, "", walletId, networkType)
        litecoinKit = LitecoinKit(
            App.instance, words, "", walletId, networkType,
            syncMode = BitcoinCore.SyncMode.Full(), purpose = HDWallet.Purpose.BIP84
        )
        litecoinKit.listener = this
        litecoinBalance.postValue(format(litecoinKit.balance.spendable) + " LTC")
        litecoinState.postValue(litecoinKit.syncState.toString())

        viewModelScope.launch(Dispatchers.IO) {
            mwebKit.syncStateFlow.collect { state ->
                syncState.postValue(
                    when (state) {
                        is MwebSyncState.Synced -> "synced"
                        is MwebSyncState.Syncing -> "syncing ${"%.3f".format(state.progress)} (${state.stage})"
                        is MwebSyncState.NotSynced -> "not synced: ${state.exception.message}"
                    }
                )
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            mwebKit.balanceFlow.collect {
                balance.postValue("${format(it.spendable)} LTC spendable, ${format(it.unconfirmed)} unconfirmed")
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            mwebKit.transactionsFlow.collect { list ->
                transactions.postValue(list.joinToString("\n\n") { describe(it) }.ifEmpty { "no transactions" })
            }
        }
    }

    fun start() {
        if (started.value == true) return
        started.value = true
        mwebKit.start()
        litecoinKit.start()
    }

    fun refresh() {
        mwebKit.refresh()
        litecoinKit.refresh()
    }

    fun clear() {
        mwebKit.stop()
        litecoinKit.stop()
        LitecoinMwebKit.clear(App.instance, networkType, walletId)
        LitecoinKit.clear(App.instance, networkType, walletId)
        started.value = false
        createKits()
    }

    fun onReceiveClick() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                receiveAddress.postValue(mwebKit.receiveAddress())
            } catch (e: Exception) {
                message.postValue("receive address failed: ${e.message}")
            }
        }
    }

    fun onLitecoinReceiveClick() {
        receiveAddress.postValue(litecoinKit.receiveAddress())
    }

    /** Sends from the MWEB wallet: to an MWEB address, or a peg-out to a Litecoin address. */
    fun onSendClick(address: String, amount: String) {
        val value = parseAmount(amount) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fee = mwebKit.estimateFee(address, value, feeRate)
                val tx = mwebKit.send(address, value, feeRate)
                message.postValue("sent ${tx.type} ${format(tx.amount)} LTC, fee ${fee} litoshi, uid ${tx.uid}")
            } catch (e: Exception) {
                message.postValue("send failed: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    /** Moves Litecoin from the plain wallet into the MWEB wallet. */
    fun onPegInClick(amount: String) {
        val value = parseAmount(amount) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fee = mwebKit.estimatePegInFee(litecoinKit, value, feeRate)
                val tx = mwebKit.pegIn(litecoinKit, value, feeRate)
                message.postValue("peg-in ${format(tx.amount)} LTC to ${tx.toAddress}, fees ${fee.litecoinFee} + ${fee.mwebFee} litoshi, txid ${tx.txHash}")
            } catch (e: Exception) {
                message.postValue("peg-in failed: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    private fun parseAmount(amount: String): Long? {
        val value = amount.trim().toDoubleOrNull()
        if (value == null || value <= 0) {
            message.value = "enter an amount in LTC"
            return null
        }
        return (value * 100_000_000).toLong()
    }

    private fun describe(tx: MwebTransactionInfo): String {
        val when_ = dateFormat.format(Date(tx.timestamp * 1000))
        val where = tx.toAddress ?: tx.ownAddress ?: ""
        return "$when_ ${tx.type} ${tx.status} ${format(tx.amount)} LTC" +
                (tx.fee?.let { " fee $it" } ?: "") +
                (tx.blockHeight?.let { " @$it" } ?: "") +
                (tx.txHash?.let { "\n  tx $it" } ?: "") +
                (if (where.isNotEmpty()) "\n  ${where.take(24)}…" else "")
    }

    private fun format(litoshi: Long) = NumberFormatHelper.cryptoAmountFormat.format(litoshi / 100_000_000.0)

    override fun onBalanceUpdate(balance: BalanceInfo) {
        litecoinBalance.postValue("${format(balance.spendable)} LTC spendable, ${format(balance.unspendableTimeLocked)} maturing")
    }

    override fun onKitStateUpdate(state: BitcoinCore.KitState) {
        litecoinState.postValue(state.toString())
    }

    override fun onCleared() {
        mwebKit.stop()
        litecoinKit.stop()
    }
}

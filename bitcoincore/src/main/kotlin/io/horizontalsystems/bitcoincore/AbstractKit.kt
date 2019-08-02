package io.horizontalsystems.bitcoincore

import io.horizontalsystems.bitcoincore.models.BitcoinPaymentData
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

abstract class AbstractKit {

    protected abstract var bitcoinCore: BitcoinCore
    protected abstract var network: Network

    val balance
        get() = bitcoinCore.balance

    val lastBlockInfo
        get() = bitcoinCore.lastBlockInfo

    val networkName: String
        get() = network.javaClass.simpleName

    fun start() {
        bitcoinCore.start()
    }

    fun stop() {
        bitcoinCore.stop()
    }

    fun refresh() {
        bitcoinCore.refresh()
    }

    fun fee(value: Long, address: String? = null, senderPay: Boolean = true, feeRate: Int, changeScriptType: Int = ScriptType.P2PKH): Long {
        return bitcoinCore.fee(value, address, senderPay, feeRate, changeScriptType)
    }

    fun send(address: String, value: Long, senderPay: Boolean = true, feeRate: Int, changeScriptType: Int = ScriptType.P2PKH): FullTransaction {
        return bitcoinCore.send(address, value, senderPay, feeRate, changeScriptType)
    }

    fun send(hash: ByteArray, scriptType: Int, value: Long, senderPay: Boolean = true, feeRate: Int, changeScriptType: Int = ScriptType.P2PKH): FullTransaction {
        return bitcoinCore.send(hash, scriptType, value, senderPay, feeRate, changeScriptType)
    }

    fun redeem(unspentOutput: UnspentOutput, address: String, feeRate: Int, signatureScriptFunction: (ByteArray, ByteArray) -> ByteArray): FullTransaction {
        return bitcoinCore.redeem(unspentOutput, address, feeRate, signatureScriptFunction)
    }

    fun receiveAddress(type: Int = ScriptType.P2PKH): String {
        return bitcoinCore.receiveAddress(type)
    }

    fun receivePublicKey(): PublicKey {
        return bitcoinCore.receivePublicKey()
    }

    fun changePublicKey(): PublicKey {
        return bitcoinCore.changePublicKey()
    }

    fun validateAddress(address: String) {
        bitcoinCore.validateAddress(address)
    }

    fun parsePaymentAddress(paymentAddress: String): BitcoinPaymentData {
        return bitcoinCore.parsePaymentAddress(paymentAddress)
    }

    fun showDebugInfo() {
        bitcoinCore.showDebugInfo()
    }

    fun getPublicKeyByPath(path: String) : PublicKey {
        return bitcoinCore.getPublicKeyByPath(path)
    }

    fun watchTransaction(filter: TransactionFilter, listener: WatchedTransactionManager.Listener) {
        bitcoinCore.watchTransaction(filter, listener)
    }
}

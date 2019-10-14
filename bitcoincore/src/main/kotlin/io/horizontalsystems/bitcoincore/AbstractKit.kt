package io.horizontalsystems.bitcoincore

import io.horizontalsystems.bitcoincore.models.BitcoinPaymentData
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutput

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

    fun fee(value: Long, address: String? = null, senderPay: Boolean = true, feeRate: Int, pluginData: Map<String, Map<String, Any>> = mapOf()): Long {
        return bitcoinCore.fee(value, address, senderPay, feeRate, pluginData)
    }

    fun send(address: String, value: Long, senderPay: Boolean = true, feeRate: Int, extraData: Map<String, Map<String, Any>> = mapOf()): FullTransaction {
        return bitcoinCore.send(address, value, senderPay, feeRate, extraData)
    }

    fun send(hash: ByteArray, scriptType: Int, value: Long, senderPay: Boolean = true, feeRate: Int): FullTransaction {
        return bitcoinCore.send(hash, scriptType, value, senderPay, feeRate)
    }

    fun redeem(unspentOutput: UnspentOutput, address: String, feeRate: Int, signatureScriptFunction: (ByteArray, ByteArray) -> ByteArray): FullTransaction {
        return bitcoinCore.redeem(unspentOutput, address, feeRate, signatureScriptFunction)
    }

    fun receiveAddress(): String {
        return bitcoinCore.receiveAddress()
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

    fun statusInfo(): Map<String, Any> {
        return bitcoinCore.statusInfo()
    }

    fun getPublicKeyByPath(path: String): PublicKey {
        return bitcoinCore.getPublicKeyByPath(path)
    }

    fun watchTransaction(filter: TransactionFilter, listener: WatchedTransactionManager.Listener) {
        bitcoinCore.watchTransaction(filter, listener)
    }
}

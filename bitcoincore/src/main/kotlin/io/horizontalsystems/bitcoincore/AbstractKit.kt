package io.horizontalsystems.bitcoincore

import io.horizontalsystems.bitcoincore.models.BitcoinPaymentData
import io.horizontalsystems.bitcoincore.network.Network

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

    fun fee(value: Long, address: String? = null, senderPay: Boolean = true, feeRate: Int): Long {
        return bitcoinCore.fee(value, address, senderPay, feeRate)
    }

    fun send(address: String, value: Long, senderPay: Boolean = true, feeRate: Int) {
        bitcoinCore.send(address, value, senderPay, feeRate)
    }

    fun receiveAddress(): String {
        return bitcoinCore.receiveAddress()
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
}

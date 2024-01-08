package io.horizontalsystems.bitcoincore

import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.bitcoincore.models.BitcoinPaymentData
import io.horizontalsystems.bitcoincore.models.BitcoinSendInfo
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.bitcoincore.models.TransactionFilterType
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.models.UsedAddress
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.reactivex.Single

abstract class AbstractKit {

    protected abstract var bitcoinCore: BitcoinCore
    protected abstract var network: Network

    val unspentOutputs
        get() = bitcoinCore.unspentOutputs

    val balance
        get() = bitcoinCore.balance

    val lastBlockInfo
        get() = bitcoinCore.lastBlockInfo

    val networkName: String
        get() = network.javaClass.simpleName

    val syncState get() = bitcoinCore.syncState

    val watchAccount: Boolean
        get() = bitcoinCore.watchAccount

    fun start() {
        bitcoinCore.start()
    }

    fun stop() {
        bitcoinCore.stop()
    }

    fun refresh() {
        bitcoinCore.refresh()
    }

    fun onEnterForeground() {
        bitcoinCore.onEnterForeground()
    }

    fun onEnterBackground() {
        bitcoinCore.onEnterBackground()
    }

    fun transactions(fromUid: String? = null, type: TransactionFilterType? = null, limit: Int? = null): Single<List<TransactionInfo>> {
        return bitcoinCore.transactions(fromUid, type, limit)
    }

    fun getTransaction(hash: String): TransactionInfo? {
        return bitcoinCore.getTransaction(hash)
    }

    fun sendInfo(
        value: Long,
        address: String? = null,
        senderPay: Boolean = true,
        feeRate: Int,
        unspentOutputs: List<UnspentOutputInfo>?,
        pluginData: Map<Byte, IPluginData> = mapOf()
    ): BitcoinSendInfo {
        return bitcoinCore.sendInfo(
            value = value,
            address = address,
            senderPay = senderPay,
            feeRate = feeRate,
            unspentOutputs = unspentOutputs,
            pluginData = pluginData
        )
    }

    fun send(
        address: String,
        value: Long,
        senderPay: Boolean = true,
        feeRate: Int,
        sortType: TransactionDataSortType,
        unspentOutputs: List<UnspentOutputInfo>? = null,
        pluginData: Map<Byte, IPluginData> = mapOf()
    ): FullTransaction {
        return bitcoinCore.send(address, value, senderPay, feeRate, sortType, unspentOutputs, pluginData)
    }

    fun send(
        address: String,
        value: Long,
        senderPay: Boolean = true,
        feeRate: Int,
        sortType: TransactionDataSortType,
        pluginData: Map<Byte, IPluginData> = mapOf()
    ): FullTransaction {
        return bitcoinCore.send(address, value, senderPay, feeRate, sortType, null, pluginData)
    }

    fun send(
        hash: ByteArray,
        scriptType: ScriptType,
        value: Long,
        senderPay: Boolean = true,
        feeRate: Int,
        sortType: TransactionDataSortType,
        unspentOutputs: List<UnspentOutputInfo>? = null,
    ): FullTransaction {
        return bitcoinCore.send(hash, scriptType, value, senderPay, feeRate, sortType, unspentOutputs)
    }

    fun send(
        hash: ByteArray,
        scriptType: ScriptType,
        value: Long,
        senderPay: Boolean = true,
        feeRate: Int,
        sortType: TransactionDataSortType,
    ): FullTransaction {
        return bitcoinCore.send(hash, scriptType, value, senderPay, feeRate, sortType, null)
    }

    fun redeem(unspentOutput: UnspentOutput, address: String, feeRate: Int, sortType: TransactionDataSortType): FullTransaction {
        return bitcoinCore.redeem(unspentOutput, address, feeRate, sortType)
    }

    fun receiveAddress(): String {
        return bitcoinCore.receiveAddress()
    }

    fun usedAddresses(change: Boolean): List<UsedAddress> {
        return bitcoinCore.usedAddresses(change)
    }

    fun receivePublicKey(): PublicKey {
        return bitcoinCore.receivePublicKey()
    }

    fun changePublicKey(): PublicKey {
        return bitcoinCore.changePublicKey()
    }

    fun validateAddress(address: String, pluginData: Map<Byte, IPluginData>) {
        bitcoinCore.validateAddress(address, pluginData)
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

    fun maximumSpendableValue(address: String?, feeRate: Int, unspentOutputs: List<UnspentOutputInfo>?, pluginData: Map<Byte, IPluginData>): Long {
        return bitcoinCore.maximumSpendableValue(address, feeRate, unspentOutputs, pluginData)
    }

    fun minimumSpendableValue(address: String?): Int {
        return bitcoinCore.minimumSpendableValue(address)
    }

    fun getRawTransaction(transactionHash: String): String? {
        return bitcoinCore.getRawTransaction(transactionHash)
    }
}

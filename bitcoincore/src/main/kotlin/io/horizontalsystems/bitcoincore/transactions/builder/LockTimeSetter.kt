package io.horizontalsystems.bitcoincore.transactions.builder

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.core.PluginManager

class LockTimeSetter(private val storage: IStorage, val pluginManager: PluginManager) {

    fun setLockTime(transaction: MutableTransaction) {
        transaction.transaction.lockTime = pluginManager.getTransactionLockTime(transaction) ?: storage.lastBlock()?.height?.toLong() ?: 0
    }

}

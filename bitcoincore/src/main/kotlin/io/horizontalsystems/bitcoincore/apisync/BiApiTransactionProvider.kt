package io.horizontalsystems.bitcoincore.apisync

import io.horizontalsystems.bitcoincore.apisync.model.TransactionItem
import io.horizontalsystems.bitcoincore.core.IApiTransactionProvider
import io.horizontalsystems.bitcoincore.managers.ApiSyncStateManager

class BiApiTransactionProvider(
    private val restoreProvider: IApiTransactionProvider,
    private val syncProvider: IApiTransactionProvider,
    private val syncStateManager: ApiSyncStateManager
) : IApiTransactionProvider {

    override fun transactions(addresses: List<String>, stopHeight: Int?): List<TransactionItem> =
        if (syncStateManager.restored) {
            syncProvider.transactions(addresses, stopHeight)
        } else {
            restoreProvider.transactions(addresses, stopHeight)
        }
}

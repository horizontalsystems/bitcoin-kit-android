package io.horizontalsystems.bitcoinkit.storage

import android.content.Context
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.models.BlockchainState
import io.horizontalsystems.bitcoinkit.models.FeeRate
import io.horizontalsystems.bitcoinkit.models.PeerAddress

class Storage(context: Context, dbName: String) : IStorage {
    private val store = KitDatabase.getInstance(context, dbName)

    // FeeRate
    override val feeRate: FeeRate?
        get() = store.feeRate.getRate()

    override fun setFeeRate(feeRate: FeeRate) {
        return store.feeRate.insert(feeRate)
    }

    // RestoreState
    override val initialRestored: Boolean?
        get() = store.blockchainState.getState()?.initialRestored

    override fun setInitialRestored(isRestored: Boolean) {
        store.blockchainState.insert(BlockchainState(initialRestored = isRestored))
    }

    // PeerAddress
    override fun getLeastScorePeerAddressExcludingIps(ips: List<String>): PeerAddress? {
        return store.peerAddress.getLeastScore(ips)
    }

    override fun getExistingPeerAddress(ips: List<String>): List<PeerAddress> {
        return store.peerAddress.getExisting(ips)
    }

    override fun increasePeerAddressScore(ip: String) {
        store.peerAddress.increaseScore(ip)
    }

    override fun deletePeerAddress(ip: String) {
        store.peerAddress.delete(ip)
    }

    override fun setPeerAddresses(list: List<PeerAddress>) {
        store.peerAddress.insertAll(list)
    }

    override fun clear() {
        store.clearAllTables()
    }
}

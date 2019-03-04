package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.models.FeeRate
import io.horizontalsystems.bitcoinkit.models.PeerAddress

interface IStorage {
    val feeRate: FeeRate?
    fun setFeeRate(feeRate: FeeRate)

    val initialRestored: Boolean?
    fun setInitialRestored(isRestored: Boolean)

    fun getLeastScorePeerAddressExcludingIps(ips: List<String>): PeerAddress?
    fun getExistingPeerAddress(ips: List<String>): List<PeerAddress>
    fun increasePeerAddressScore(ip: String)
    fun deletePeerAddress(ip: String)
    fun setPeerAddresses(list: List<PeerAddress>)

    fun clear()
}

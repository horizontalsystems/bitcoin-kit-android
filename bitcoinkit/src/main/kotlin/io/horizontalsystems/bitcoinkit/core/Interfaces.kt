package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.BlockHash
import io.horizontalsystems.bitcoinkit.models.FeeRate
import io.horizontalsystems.bitcoinkit.models.PeerAddress
import io.realm.Realm
import io.realm.RealmResults

interface IStorage {
    //  realm

    fun inTransaction(callback: (Realm) -> Unit)
    fun realmInstance(callback: (Realm) -> Unit)

    //  FeeRate

    val feeRate: FeeRate?
    fun setFeeRate(feeRate: FeeRate)

    //  BlockchainState

    val initialRestored: Boolean?
    fun setInitialRestored(isRestored: Boolean)

    //  PeerAddress

    fun getLeastScorePeerAddressExcludingIps(ips: List<String>): PeerAddress?
    fun getExistingPeerAddress(ips: List<String>): List<PeerAddress>
    fun increasePeerAddressScore(ip: String)
    fun deletePeerAddress(ip: String)
    fun setPeerAddresses(list: List<PeerAddress>)

    //  BlockHash

    fun getBlockHashesSortedBySequenceAndHeight(limit: Int): List<BlockHash>
    fun getBlockHashHeaderHashes(): List<ByteArray>
    fun getBlockHashHeaderHashHexes(except: String): List<String>
    fun getLastBlockHash(): BlockHash?
    fun getBlockchainBlockHashes(): List<BlockHash>
    fun getLastBlockchainBlockHash(): BlockHash?
    fun deleteBlockchainBlockHashes()
    fun deleteBlockHash(hashHex: String)
    fun addBlockHashes(hashes: List<BlockHash>)

    //  Block

    fun getBlock(height: Int): Block?
    fun getBlock(headerHash: ByteArray): Block?
    fun getBlocks(heightGreaterThan: Int, sortedBy: String, limit: Int): List<Block>
    fun getBlocks(realm: Realm, hashHexes: List<String>): RealmResults<Block>
    fun blocksCount(headerHexes: List<String>? = null): Int
    fun saveBlock(block: Block)
    fun lastBlock(): Block?

    fun clear()
}

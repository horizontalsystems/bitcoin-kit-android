package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.models.*
import io.realm.Realm
import io.realm.RealmObject

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

    fun <E : RealmObject> copyToRealm(obj: E, realm: Realm): E

    fun getBlock(height: Int): Block?
    fun getBlock(hashHex: String, realm: Realm? = null): Block?
    fun getBlock(stale: Boolean, sortedHeight: String, realm: Realm? = null): Block?

    fun getBlocks(stale: Boolean, realm: Realm? = null): List<Block>
    fun getBlocks(heightGreaterThan: Int, sortedBy: String, limit: Int): List<Block>
    fun getBlocks(heightGreaterOrEqualTo: Int, stale: Boolean, realm: Realm? = null): List<Block>
    fun getBlocks(realm: Realm, hashHexes: List<String>): List<Block>

    fun blocksCount(headerHexes: List<String>? = null): Int
    fun saveBlock(block: Block)
    fun lastBlock(): Block?
    fun updateBlock(staleBlock: Block, realm: Realm)
    fun deleteBlocks(blocks: List<Block>, realm: Realm)

    //  Transaction

    fun getBlockTransactions(block: Block, realm: Realm): List<Transaction>
    fun getNewTransactions(): List<Transaction>
    fun getNewTransaction(hashHex: String): Transaction?
    fun isTransactionExists(hash: ByteArray): Boolean

    //  SentTransaction

    fun getSentTransaction(hashHex: String): SentTransaction?
    fun addSentTransaction(transaction: SentTransaction)
    fun updateSentTransaction(transaction: SentTransaction)

    fun clear()
}

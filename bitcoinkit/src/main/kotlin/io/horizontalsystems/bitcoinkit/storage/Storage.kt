package io.horizontalsystems.bitcoinkit.storage

import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.models.*
import io.realm.Realm
import io.realm.RealmObject
import io.realm.Sort

class Storage(private val store: KitDatabase, val realmFactory: RealmFactory) : IStorage {

    override fun inTransaction(callback: (Realm) -> Unit) {
        realmFactory.realm.use { realm ->
            realm.executeTransaction {
                callback.invoke(it)
            }
        }
    }

    override fun realmInstance(callback: (Realm) -> Unit) {
        realmFactory.realm.use {
            callback.invoke(it)
        }
    }

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
        store.peerAddress.delete(PeerAddress(ip))
    }

    override fun setPeerAddresses(list: List<PeerAddress>) {
        store.peerAddress.insertAll(list)
    }

    // BlockHash

    override fun getBlockHashesSortedBySequenceAndHeight(limit: Int): List<BlockHash> {
        return store.blockHash.getBlockHashesSortedSequenceHeight(limit)
    }

    override fun getBlockHashHeaderHashes(): List<ByteArray> {
        return store.blockHash.allBlockHashes()
    }

    override fun getBlockHashHeaderHashHexes(except: String): List<String> {
        return store.blockHash.allBlockHashes(except)
    }

    override fun getLastBlockHash(): BlockHash? {
        return store.blockHash.getLastBlockHash()
    }

    override fun getBlockchainBlockHashes(): List<BlockHash> {
        return store.blockHash.getBlockchainBlockHashes()
    }

    override fun addBlockHashes(hashes: List<BlockHash>) {
        store.blockHash.insertAll(hashes)
    }

    override fun getLastBlockchainBlockHash(): BlockHash? {
        return store.blockHash.getLastBlockchainBlockHash()
    }

    override fun deleteBlockchainBlockHashes() {
        store.blockHash.delete(height = 0)
    }

    override fun deleteBlockHash(hashHex: String) {
        store.blockHash.delete(hashHex)
    }

    // Block

    override fun <E : RealmObject> copyToRealm(obj: E, realm: Realm): E {
        return realm.copyToRealmOrUpdate(obj)
    }

    override fun getBlock(height: Int): Block? {
        realmFactory.realm.use {
            val block = it.where(Block::class.java)
                    .equalTo("height", height)
                    .findFirst() ?: return null

            return it.copyFromRealm(block)
        }
    }

    override fun getBlock(hashHex: String, realm: Realm?): Block? {
        realm?.let {
            return it.where(Block::class.java).equalTo("reversedHeaderHashHex", hashHex).findFirst()
        }

        realmFactory.realm.use {
            val block = it.where(Block::class.java).equalTo("reversedHeaderHashHex", hashHex)
                    .findFirst() ?: return null

            return it.copyFromRealm(block)
        }
    }

    override fun getBlock(stale: Boolean, sortedHeight: String, realm: Realm?): Block? {
        val sortOrder = if (sortedHeight == "ASC") {
            Sort.ASCENDING
        } else {
            Sort.DESCENDING
        }

        realm?.let {
            return realm.where(Block::class.java)
                    .equalTo("stale", stale)
                    .sort("height", sortOrder)
                    .findFirst()
        }

        realmFactory.realm.use {
            val result = it.where(Block::class.java)
                    .equalTo("stale", stale)
                    .sort("height", sortOrder)
                    .findFirst() ?: return null

            return it.copyFromRealm(result)
        }
    }

    override fun getBlocks(stale: Boolean, realm: Realm?): List<Block> {
        realm?.let {
            return it.where(Block::class.java)
                    .equalTo("stale", stale)
                    .findAll()
        }

        realmFactory.realm.use {
            val results = it.where(Block::class.java)
                    .equalTo("stale", stale)
                    .findAll()

            return it.copyFromRealm(results)
        }
    }

    override fun getBlocks(heightGreaterThan: Int, sortedBy: String, limit: Int): List<Block> {
        realmFactory.realm.use {
            val blocks = it.where(Block::class.java)
                    .greaterThan("height", heightGreaterThan)
                    .sort(sortedBy, Sort.DESCENDING)
                    .findAll()
                    .take(limit)

            return it.copyFromRealm(blocks)
        }
    }

    override fun getBlocks(heightGreaterOrEqualTo: Int, stale: Boolean, realm: Realm?): List<Block> {
        realm?.let {
            return it.where(Block::class.java)
                    .equalTo("stale", false)
                    .greaterThanOrEqualTo("height", heightGreaterOrEqualTo)
                    .findAll()
        }

        realmFactory.realm.use {
            val results = it.where(Block::class.java)
                    .equalTo("stale", false)
                    .greaterThanOrEqualTo("height", heightGreaterOrEqualTo)
                    .findAll()

            return it.copyFromRealm(results)
        }
    }

    override fun getBlocks(realm: Realm, hashHexes: List<String>): List<Block> {
        return realm.where(Block::class.java).`in`("reversedHeaderHashHex", hashHexes.toTypedArray()).findAll()
    }

    override fun blocksCount(headerHexes: List<String>?): Int {
        realmFactory.realm.use {
            val realmQuery = it.where(Block::class.java)
            if (headerHexes != null) {
                realmQuery.`in`("reversedHeaderHashHex", headerHexes.toTypedArray())
            }

            return realmQuery.count().toInt()
        }
    }

    override fun saveBlock(block: Block) {
        realmFactory.realm.use { realm ->
            realm.executeTransaction { it.insertOrUpdate(block) }
        }
    }

    override fun lastBlock(): Block? {
        realmFactory.realm.use {
            val block = it.where(Block::class.java)
                    .sort("height", Sort.DESCENDING)
                    .findFirst() ?: return null

            return it.copyFromRealm(block)
        }
    }

    override fun updateBlock(staleBlock: Block, realm: Realm) {
        realm.copyToRealmOrUpdate(staleBlock)
    }

    override fun deleteBlocks(blocks: List<Block>, realm: Realm) {
        val managedBlocks = realm.where(Block::class.java)
                .`in`("reversedHeaderHashHex", blocks.map { it.reversedHeaderHashHex }.toTypedArray())
                .findAll()

        managedBlocks.forEach { block ->
            block.transactions?.forEach { transaction ->
                transaction.inputs.deleteAllFromRealm()
                transaction.outputs.deleteAllFromRealm()
            }

            block.transactions?.deleteAllFromRealm()
        }

        managedBlocks.deleteAllFromRealm()
    }

    // Transaction

    override fun getBlockTransactions(block: Block, realm: Realm): List<Transaction> {
        val result = realm.where(Block::class.java).equalTo("reversedHeaderHashHex", block.reversedHeaderHashHex).findFirst()
        if (result?.transactions == null) {
            return emptyList()
        }

        return result.transactions
    }

    override fun getNewTransaction(hashHex: String): Transaction? {
        realmFactory.realm.use {
            return it.where(Transaction::class.java)
                    .equalTo("hashHexReversed", hashHex)
                    .equalTo("status", Transaction.Status.NEW)
                    .findFirst()
        }
    }

    override fun getNewTransactions(): List<Transaction> {
        realmFactory.realm.use {
            val results = it.where(Transaction::class.java).equalTo("status", Transaction.Status.NEW).findAll()
            return it.copyFromRealm(results)
        }
    }

    override fun isTransactionExists(hash: ByteArray): Boolean {
        realmFactory.realm.use {
            return it.where(Transaction::class.java).equalTo("hash", hash).count() > 0
        }
    }

    // SentTransaction

    override fun getSentTransaction(hashHex: String): SentTransaction? {
        return store.sentTransaction.getTransaction(hashHex)
    }

    override fun addSentTransaction(transaction: SentTransaction) {
        store.sentTransaction.insert(transaction)
    }

    override fun updateSentTransaction(transaction: SentTransaction) {
        store.sentTransaction.insert(transaction)
    }

    // Rest

    override fun clear() {
        store.clearAllTables()
    }

}

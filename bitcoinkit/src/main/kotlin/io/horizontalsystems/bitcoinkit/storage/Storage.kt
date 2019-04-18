package io.horizontalsystems.bitcoinkit.storage

import android.arch.persistence.db.SimpleSQLiteQuery
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.models.*

open class Storage(protected open val store: KitDatabase) : IStorage {

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

    override fun getBlock(height: Int): Block? {
        return store.block.getBlockByHeight(height)
    }

    override fun getBlock(hashHex: String): Block? {
        return store.block.getBlockByHex(hashHex)
    }

    override fun getBlock(stale: Boolean, sortedHeight: String): Block? {
        return if (sortedHeight == "DESC") {
            store.block.getLast(stale)
        } else {
            store.block.getFirst(stale)
        }
    }

    override fun getBlocks(stale: Boolean): List<Block> {
        return store.block.getBlocksByStale(stale)
    }

    override fun getBlocks(heightGreaterThan: Int, sortedBy: String, limit: Int): List<Block> {
        return store.block.getBlocks(heightGreaterThan, limit)
    }

    override fun getBlocks(heightGreaterOrEqualTo: Int, stale: Boolean): List<Block> {
        return store.block.getBlocks(heightGreaterOrEqualTo, stale)
    }

    override fun getBlocks(hashHexes: List<String>): List<Block> {
        return store.block.getBlocks(hashHexes)
    }

    override fun getBlocksChunk(fromHeight: Int, toHeight: Int): List<Block> {
        return store.block.getBlocksChunk(fromHeight, toHeight)
    }

    override fun blocksCount(headerHexes: List<String>?): Int {
        return if (headerHexes == null) {
            store.block.count()
        } else {
            store.block.getBlocksCount(headerHexes)
        }
    }

    override fun addBlock(block: Block) {
        store.block.insert(block)
    }

    override fun saveBlock(block: Block) {
        store.block.insert(block)
    }

    override fun lastBlock(): Block? {
        return store.block.getLastBlock()
    }

    override fun updateBlock(staleBlock: Block) {
        store.block.update(staleBlock)
    }

    override fun deleteBlocks(blocks: List<Block>) {
        blocks.forEach { block ->
            val transactions = store.transaction.getBlockTransactions(block.headerHashReversedHex)

            transactions.forEach {
                store.input.deleteAll(getTransactionInputs(it))
                store.output.deleteAll(getTransactionOutputs(it))
            }

            store.transaction.deleteAll(transactions)
        }

        store.block.deleteAll(blocks)
    }

    // Transaction

    override fun getTransactionsSortedTimestampAndOrdered(): List<Transaction> {
        return store.transaction.getSortedTimestampAndOrdered()
    }

    override fun getFullTransactionInfo(transactions: List<TransactionWithBlock>): List<FullTransactionInfo> {
        val txHashes = transactions.map { it.transaction.hashHexReversed }
        val inputs = store.input.getInputsWithPrevouts(txHashes)
        val outputs = store.output.getTransactionsOutputs(txHashes)

        return transactions.map { tx ->
            FullTransactionInfo(
                    tx.block,
                    tx.transaction,
                    inputs.filter { it.input.transactionHashReversedHex == tx.transaction.hashHexReversed },
                    outputs.filter { it.transactionHashReversedHex == tx.transaction.hashHexReversed }
            )
        }
    }

    override fun getFullTransactionInfo(fromTransaction: Transaction?, limit: Int?): List<FullTransactionInfo> {
        var query = "SELECT transactions.*, Block.*" +
                " FROM `Transaction` as transactions" +
                " LEFT JOIN Block ON transactions.blockHashReversedHex = Block.headerHashReversedHex"

        if (fromTransaction != null) {
            query = " WHERE transactions.timestamp < ${fromTransaction.timestamp} OR (transactions.timestamp = ${fromTransaction.timestamp} AND transactions.`order` < ${fromTransaction.order})"
        }

        query += " ORDER BY timestamp DESC, `order` DESC"

        if (limit != null) {
            query += ", LIMIT $limit"
        }

        return getFullTransactionInfo(store.transaction.getTransactionWithBlockBySql(SimpleSQLiteQuery(query)))
    }

    override fun getTransaction(hashHex: String): Transaction? {
        return store.transaction.getByHashHex(hashHex)
    }

    override fun getTransactionOfOutput(output: TransactionOutput): Transaction? {
        return store.transaction.getByHashHex(output.transactionHashReversedHex)
    }

    override fun addTransaction(transaction: FullTransaction) {
        store.runInTransaction {
            store.transaction.insert(transaction.header)

            transaction.inputs.forEach {
                store.input.insert(it)
            }

            transaction.outputs.forEach {
                store.output.insert(it)
            }
        }
    }

    override fun updateTransaction(transaction: Transaction) {
        store.transaction.update(transaction)
    }

    override fun getBlockTransactions(block: Block): List<Transaction> {
        return store.transaction.getBlockTransactions(block.headerHashReversedHex)
    }

    override fun getNewTransaction(hashHex: String): Transaction? {
        return store.transaction.getNewTransaction(hashHex)
    }

    override fun getNewTransactions(): List<FullTransaction> {
        return store.transaction.getNewTransactions().map {
            FullTransaction(header = it, inputs = getTransactionInputs(it), outputs = getTransactionOutputs(it))
        }
    }

    override fun isTransactionExists(hash: ByteArray): Boolean {
        return store.transaction.getByHash(hash) != null
    }

    // TransactionOutput

    override fun getUnspentOutputs(): List<UnspentOutput> {
        return store.output.getUnspents()
    }

    override fun getPreviousOutput(input: TransactionInput): TransactionOutput? {
        return store.output.getPreviousOutput(input.previousOutputTxReversedHex, input.previousOutputIndex.toInt())
    }

    override fun getTransactionOutputs(transaction: Transaction): List<TransactionOutput> {
        return store.output.getByHashHex(transaction.hashHexReversed)
    }

    override fun getOutputsOfPublicKey(publicKey: PublicKey): List<TransactionOutput> {
        return store.output.getListByPath(publicKey.path)
    }

    override fun getMyOutputs(): List<FullOutputInfo> {
        return store.output.getMyOutputs()
    }

    // TransactionInput

    override fun getTransactionInputs(transaction: Transaction): List<TransactionInput> {
        return store.input.getTransactionInputs(transaction.hashHexReversed)
    }

    // PublicKey

    override fun hasInputs(ofOutput: TransactionOutput): Boolean {
        return store.input.getInputsOfOutput(ofOutput.transactionHashReversedHex, ofOutput.index).isNotEmpty()
    }

    override fun getPublicKey(byPath: String): PublicKey? {
        return store.publicKey.getByPath(path = byPath)
    }

    override fun getPublicKeyByHash(keyHash: ByteArray, isWPKH: Boolean): PublicKey? {
        return if (isWPKH) {
            store.publicKey.getByScriptHashWPKH(keyHash)
        } else {
            store.publicKey.getByKeyOrKeyHash(keyHash)
        }
    }

    override fun getPublicKeys(): List<PublicKey> {
        return store.publicKey.getAll()
    }

    override fun savePublicKeys(keys: List<PublicKey>) {
        store.publicKey.insertOrIgnore(keys)
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

package io.horizontalsystems.bitcoincore.storage

import androidx.sqlite.db.SimpleSQLiteQuery
import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType

open class Storage(protected open val store: CoreDatabase) : IStorage {

    // RestoreState

    override val initialRestored: Boolean?
        get() = store.blockchainState.getState()?.initialRestored

    override fun setInitialRestored(isRestored: Boolean) {
        store.blockchainState.insert(BlockchainState(initialRestored = isRestored))
    }

    // PeerAddress

    override fun getLeastScoreFastestPeerAddressExcludingIps(ips: List<String>): PeerAddress? {
        return store.peerAddress.getLeastScoreFastest(ips)
    }

    override fun deletePeerAddress(ip: String) {
        store.peerAddress.delete(PeerAddress(ip))
    }

    override fun setPeerAddresses(list: List<PeerAddress>) {
        store.peerAddress.insertAll(list)
    }

    override fun markConnected(ip: String, time: Long) {
        store.peerAddress.setSuccessConnectionTime(time, ip)
    }

    // BlockHash

    override fun getBlockHashesSortedBySequenceAndHeight(limit: Int): List<BlockHash> {
        return store.blockHash.getBlockHashesSortedSequenceHeight(limit)
    }

    override fun getBlockHashHeaderHashes(): List<ByteArray> {
        return store.blockHash.allBlockHashes()
    }

    override fun getBlockHashHeaderHashes(except: List<ByteArray>): List<ByteArray> {
        return store.blockHash.allBlockHashes(except)
    }

    override fun getLastBlockHash(): BlockHash? {
        return store.blockHash.getLastBlockHash()
    }

    override fun getBlockchainBlockHashes(): List<BlockHash> {
        return store.blockHash.getBlockchainBlockHashes()
    }

    override fun deleteBlockHash(hash: ByteArray) {
        store.blockHash.delete(hash)
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

    // Block

    override fun getBlockByHeightStalePrioritized(height: Int): Block? {
        return store.block.getBlockByHeightStalePrioritized(height)
    }

    override fun getBlock(height: Int): Block? {
        return store.block.getBlockByHeight(height)
    }

    override fun getBlock(hashHash: ByteArray): Block? {
        return store.block.getBlockByHash(hashHash)
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

    override fun getBlocks(hashes: List<ByteArray>): List<Block> {
        return store.block.getBlocks(hashes)
    }

    override fun getBlocksChunk(fromHeight: Int, toHeight: Int): List<Block> {
        return store.block.getBlocksChunk(fromHeight, toHeight)
    }

    override fun blocksCount(headerHashes: List<ByteArray>?): Int {
        return if (headerHashes == null) {
            store.block.count()
        } else {
            store.block.getBlocksCount(headerHashes)
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
            val transactions = store.transaction.getBlockTransactions(block.headerHash)

            transactions.forEach {
                store.input.deleteAll(getTransactionInputs(it))
                store.output.deleteAll(getTransactionOutputs(it))
            }

            store.transaction.deleteAll(transactions)
        }

        store.block.deleteAll(blocks)
    }

    override fun deleteBlocksWithoutTransactions(toHeight: Int) {
        store.block.deleteBlocksWithoutTransactions(toHeight)
    }

    override fun unstaleAllBlocks() {
        store.block.unstaleAllBlocks()
    }

    override fun timestamps(from: Int, to: Int): List<Long> {
        return store.block.getTimestamps(from, to)
    }

    // Transaction

    override fun getFullTransactionInfo(transactions: List<TransactionWithBlock>): List<FullTransactionInfo> {
        val txHashes = transactions.map { it.transaction.hash }
        val inputs = store.input.getInputsWithPrevouts(txHashes)
        val outputs = store.output.getTransactionsOutputs(txHashes)

        return transactions.map { tx ->
            FullTransactionInfo(
                    tx.block,
                    if (tx.transaction.status == Transaction.Status.INVALID) InvalidTransaction(tx.transaction, tx.transaction.serializedTxInfo, tx.transaction.rawTransaction) else tx.transaction,
                    inputs.filter { it.input.transactionHash.contentEquals(tx.transaction.hash) },
                    outputs.filter { it.transactionHash.contentEquals(tx.transaction.hash) }
            )
        }
    }

    override fun getFullTransactionInfo(fromTransaction: Transaction?, limit: Int?): List<FullTransactionInfo> {
        var query = "SELECT transactions.*, Block.*" +
                " FROM (SELECT * FROM `Transaction` UNION ALL SELECT * FROM InvalidTransaction) as transactions" +
                " LEFT JOIN Block ON transactions.blockHash = Block.headerHash"

        if (fromTransaction != null) {
            query += " WHERE transactions.timestamp < ${fromTransaction.timestamp} OR (transactions.timestamp = ${fromTransaction.timestamp} AND transactions.`order` < ${fromTransaction.order})"
        }

        query += " ORDER BY timestamp DESC, `order` DESC"

        if (limit != null) {
            query += " LIMIT $limit"
        }

        return getFullTransactionInfo(store.transaction.getTransactionWithBlockBySql(SimpleSQLiteQuery(query)))
    }

    override fun getFullTransactionInfo(txHash: ByteArray): FullTransactionInfo? {
        return store.transaction.getTransactionWithBlock(txHash)?.let { tx ->
            val inputs = store.input.getInputsWithPrevouts(listOf(txHash))
            val outputs = store.output.getTransactionsOutputs(listOf(txHash))

            FullTransactionInfo(
                    tx.block,
                    tx.transaction,
                    inputs.filter { it.input.transactionHash.contentEquals(tx.transaction.hash) },
                    outputs.filter { it.transactionHash.contentEquals(tx.transaction.hash) }
            )
        }
    }

    override fun getTransaction(hash: ByteArray): Transaction? {
        return store.transaction.getByHash(hash)
    }

    override fun getFullTransaction(hash: ByteArray): FullTransaction? {
        return store.transaction.getByHash(hash)?.let { convertToFullTransaction(it) }
    }

    override fun getValidOrInvalidTransaction(uid: String): Transaction? {
        return store.transaction.getValidOrInvalidByUid(uid)
    }

    override fun getTransactionOfOutput(output: TransactionOutput): Transaction? {
        return store.transaction.getByHash(output.transactionHash)
    }

    override fun addTransaction(transaction: FullTransaction) {
        store.runInTransaction {
            addWithoutTransaction(transaction)
        }
    }

    override fun updateTransaction(transaction: Transaction) {
        store.transaction.update(transaction)
    }

    override fun updateTransaction(transaction: FullTransaction) {
        store.runInTransaction {
            store.transaction.update(transaction.header)
            transaction.inputs.forEach {
                store.input.update(it)
            }

            transaction.outputs.forEach {
                store.output.update(it)
            }
        }

    }

    override fun getBlockTransactions(block: Block): List<Transaction> {
        return store.transaction.getBlockTransactions(block.headerHash)
    }

    override fun getNewTransaction(hash: ByteArray): Transaction? {
        return store.transaction.getNewTransaction(hash)
    }

    override fun getNewTransactions(): List<FullTransaction> {
        return store.transaction.getNewTransactions().map { convertToFullTransaction(it) }
    }

    override fun isRelayedTransactionExists(hash: ByteArray): Boolean {
        return store.transaction.getByHashAndStatus(hash, Transaction.Status.RELAYED) != null
    }

    override fun isTransactionExists(hash: ByteArray): Boolean {
        return store.transaction.getByHash(hash) != null
    }

    override fun getConflictingTransactions(transaction: FullTransaction): List<Transaction> {
        val txHashes = HashSet<String>()
        transaction.inputs.forEach { input ->
            store.input.getInput(input.previousOutputTxHash, input.previousOutputIndex)?.transactionHash?.let { txHash ->
                txHashes.add(txHash.toHexString())
            }
        }

        txHashes.remove(transaction.header.hash.toHexString())

        return if (txHashes.isNotEmpty()) {
            txHashes.mapNotNull {
                store.transaction.getByHash(it.hexToByteArray())
            }
        } else {
            listOf()
        }
    }

    override fun getIncomingPendingTxHashes(): List<ByteArray> {
        return store.transaction.getIncomingPendingTxHashes()
    }

    override fun incomingPendingTransactionsExist(): Boolean {
        return store.transaction.getIncomingPendingTxCount() > 0
    }

    private fun convertToFullTransaction(transaction: Transaction): FullTransaction {
        return FullTransaction(header = transaction, inputs = getTransactionInputs(transaction), outputs = getTransactionOutputs(transaction))
    }

    private fun addWithoutTransaction(transaction: FullTransaction) {
        store.transaction.insert(transaction.header)

        transaction.inputs.forEach {
            store.input.insert(it)
        }

        transaction.outputs.forEach {
            store.output.insert(it)
        }
    }

    // InvalidTransaction

    override fun getInvalidTransaction(hash: ByteArray): InvalidTransaction? {
        return store.transaction.getInvalidTransaction(hash)
    }

    override fun moveTransactionToInvalidTransactions(invalidTransactions: List<InvalidTransaction>) {
        store.runInTransaction {
            invalidTransactions.forEach { invalidTransaction ->
                store.invalidTransaction.insert(invalidTransaction)

                val inputs = store.input.getInputsWithPrevouts(listOf(invalidTransaction.hash))
                inputs.forEach { input ->
                    input.previousOutput?.let {
                        store.output.markFailedToSpend(it.outputTransactionHash, it.index)
                    }
                }

                store.input.deleteByTxHash(invalidTransaction.hash)

                store.output.deleteByTxHash(invalidTransaction.hash)

                store.transaction.deleteByHash(invalidTransaction.hash)
            }
        }
    }

    override fun moveInvalidTransactionToTransactions(invalidTransaction: InvalidTransaction, toTransactions: FullTransaction) {
        store.runInTransaction {
            addWithoutTransaction(toTransactions)
            store.invalidTransaction.delete(invalidTransaction.uid)
        }
    }

    override fun deleteAllInvalidTransactions() {
        store.invalidTransaction.deleteAll()
    }

    // TransactionOutput

    override fun getUnspentOutputs(): List<UnspentOutput> {
        return store.output.getUnspents()
    }

    override fun getPreviousOutput(input: TransactionInput): TransactionOutput? {
        return store.output.getPreviousOutput(input.previousOutputTxHash, input.previousOutputIndex.toInt())
    }

    override fun getTransactionOutputs(transaction: Transaction): List<TransactionOutput> {
        return store.output.getByHash(transaction.hash)
    }

    override fun getOutputsOfPublicKey(publicKey: PublicKey): List<TransactionOutput> {
        return store.output.getListByPath(publicKey.path)
    }

    override fun getMyOutputs(): List<TransactionOutput> {
        return store.output.getMyOutputs()
    }

    override fun getOutputsForBloomFilter(blockHeight: Int, irregularScriptTypes: List<ScriptType>): List<TransactionOutput> {
        return store.output.getOutputsForBloomFilter(blockHeight, irregularScriptTypes.map { it.value })
    }

    // TransactionInput

    override fun getTransactionInputs(transaction: Transaction): List<TransactionInput> {
        return store.input.getTransactionInputs(transaction.hash)
    }

    override fun getTransactionInputs(txHash: ByteArray): List<TransactionInput> {
        return store.input.getTransactionInputs(txHash)
    }

    override fun getTransactionInputs(txHashes: List<ByteArray>): List<TransactionInput> {
        return store.input.getTransactionInputs(txHashes)
    }

    override fun getTransactionInput(previousOutputTxHash: ByteArray, previousOutputIndex: Long): TransactionInput? {
        return store.input.getInput(previousOutputTxHash, previousOutputIndex)
    }

    override fun getTransactionInputsByPrevOutputTxHash(txHash: ByteArray): List<TransactionInput> {
        return store.input.getInputsByPrevOutputTxHash(txHash)
    }

    // PublicKey

    override fun getPublicKeyByScriptHashForP2PWKH(keyHash: ByteArray): PublicKey? {
        return store.publicKey.getByScriptHashWPKH(keyHash)
    }

    override fun getPublicKeyByKeyOrKeyHash(keyHash: ByteArray): PublicKey? {
        return store.publicKey.getByKeyOrKeyHash(keyHash)
    }

    override fun getPublicKeys(): List<PublicKey> {
        return store.publicKey.getAll()
    }

    override fun getPublicKeysUsed(): List<PublicKey> {
        return store.publicKey.getAllUsed()
    }

    override fun getPublicKeysUnused(): List<PublicKey> {
        return store.publicKey.getAllUnused()
    }

    override fun getPublicKeysWithUsedState(): List<PublicKeyWithUsedState> {
        return store.publicKey.getAllWithUsedState()
    }

    override fun savePublicKeys(keys: List<PublicKey>) {
        store.publicKey.insertOrIgnore(keys)
    }

    // SentTransaction

    override fun getSentTransaction(hash: ByteArray): SentTransaction? {
        return store.sentTransaction.getTransaction(hash)
    }

    override fun addSentTransaction(transaction: SentTransaction) {
        store.sentTransaction.insert(transaction)
    }

    override fun updateSentTransaction(transaction: SentTransaction) {
        store.sentTransaction.insert(transaction)
    }

    override fun deleteSentTransaction(transaction: SentTransaction) {
        store.sentTransaction.delete(transaction)
    }

}

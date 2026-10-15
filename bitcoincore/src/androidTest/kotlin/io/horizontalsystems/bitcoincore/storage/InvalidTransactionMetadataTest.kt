package io.horizontalsystems.bitcoincore.storage

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.horizontalsystems.bitcoincore.blocks.IBlockchainDataListener
import io.horizontalsystems.bitcoincore.core.BaseTransactionInfoConverter
import io.horizontalsystems.bitcoincore.core.PluginManager
import io.horizontalsystems.bitcoincore.core.TransactionInfoConverter
import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.models.InvalidTransaction
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.serializers.TransactionSerializer
import io.horizontalsystems.bitcoincore.transactions.TransactionInvalidator
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InvalidTransactionMetadataTest {

    private lateinit var database: CoreDatabase
    private lateinit var storage: Storage
    private val converter = TransactionInfoConverter().apply {
        baseConverter = BaseTransactionInfoConverter(PluginManager())
    }
    private val listener = object : IBlockchainDataListener {
        override fun onBlockInsert(block: Block) = Unit
        override fun onTransactionsUpdate(inserted: List<Transaction>, updated: List<Transaction>, block: Block?) = Unit
        override fun onTransactionsDelete(hashes: List<String>) = Unit
    }

    private val sequence = 0xfffffffdL
    private val outpointP = ByteArray(32) { 1 }
    private val outpointQ = ByteArray(32) { 2 }

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, CoreDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        storage = Storage(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // A spends P:0 and Q:1. X spends P:0 with the same sequence, the way a fee bump from
    // another wallet does, so its input row replaces A's. A is then invalidated.
    @Test
    fun invalidatingTransactionWithReplacedInput_keepsTransactionListReadable() {
        val a = transaction(listOf(outpointP to 0L, outpointQ to 1L), outputValue = 90_000)
        val x = transaction(listOf(outpointP to 0L), outputValue = 40_000)
        storage.addTransaction(a)
        storage.addTransaction(x)

        TransactionInvalidator(storage, converter, listener)
            .invalidate(storage.getTransaction(a.header.hash)!!)

        // What DataProvider.transactions() does for the "All" filter
        val list = storage.getFullTransactionInfo(null, null, null).map { converter.transactionInfo(it) }

        assertNull(storage.getTransaction(a.header.hash))
        assertNotNull(storage.getInvalidTransaction(a.header.hash))
        assertEquals(1, list.count { it.uid == a.header.uid })
        assertEquals(2, list.size)
    }

    @Test
    fun migration_repairsInvalidTransactionSavedUnderRehashedHash() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "migration-19-20-test"
        context.deleteDatabase(dbName)

        val a = transaction(listOf(outpointP to 0L, outpointQ to 1L), outputValue = 90_000)
        val x = transaction(listOf(outpointP to 0L), outputValue = 40_000)
        val prevQ = Transaction(version = 2, lockTime = 0).apply { hash = outpointQ }

        CoreDatabase.getInstance(context, dbName).let { db ->
            val storage = Storage(db)
            db.transaction.insert(prevQ)
            db.output.insert(TransactionOutput(50_000, 1, byteArrayOf(0x51), ScriptType.P2PKH).apply { transactionHash = outpointQ })
            storage.addTransaction(a)
            storage.addTransaction(x)
            invalidateLikeVersion19(storage, a.header.hash)

            assertNotNull(storage.getTransaction(a.header.hash))
            db.close()
        }

        SQLiteDatabase.openDatabase(context.getDatabasePath(dbName).path, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.version = 19
        }

        CoreDatabase.getInstance(context, dbName).let { db ->
            val storage = Storage(db)
            val list = storage.getFullTransactionInfo(null, null, null).map { converter.transactionInfo(it) }

            assertNull(storage.getTransaction(a.header.hash))
            assertNotNull(storage.getInvalidTransaction(a.header.hash))
            assertEquals(1, list.count { it.uid == a.header.uid })
            assertEquals(90_000, list.first { it.uid == a.header.uid }.amount)
            assertTrue(db.input.getTransactionInputs(a.header.hash).isEmpty())
            assertTrue(db.output.getTransactionsOutputs(listOf(outpointQ)).single().failedToSpend)
            db.close()
        }
        context.deleteDatabase(dbName)
    }

    @Test
    fun invalidTransactionWithoutMetadata_isListedFromItsSavedInfo() {
        val a = transaction(listOf(outpointP to 0L), outputValue = 90_000)
        storage.addTransaction(a)
        val info = storage.getFullTransactionInfo(a.header.hash)!!
        val serializedTxInfo = converter.transactionInfo(info).serialize()
        val orphan = InvalidTransaction(info.header, serializedTxInfo, null).apply {
            uid = "orphan"
            hash = ByteArray(32) { 9 }
        }
        database.invalidTransaction.insert(orphan)

        val list = storage.getFullTransactionInfo(null, null, null).map { converter.transactionInfo(it) }

        assertEquals(2, list.size)
        assertEquals(90_000, list.first { it.transactionHash == info.header.hash.reversedArray().toHex() }.amount)
    }

    // What TransactionInvalidator did before the fix: the raw transaction rehashed the stored
    // header, and the move used that hash.
    private fun invalidateLikeVersion19(storage: Storage, hash: ByteArray) {
        val info = storage.getFullTransactionInfo(hash)!!
        info.header.status = Transaction.Status.INVALID
        val serializedTxInfo = converter.transactionInfo(info).serialize()
        val rawTransaction = TransactionSerializer.serialize(FullTransaction(info.header, info.inputs.map { it.input }, info.outputs)).toHex()
        assertNotEquals(hash.toHex(), info.header.hash.toHex())
        storage.moveTransactionToInvalidTransactions(listOf(InvalidTransaction(info.header, serializedTxInfo, rawTransaction)))
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private fun transaction(outpoints: List<Pair<ByteArray, Long>>, outputValue: Long): FullTransaction {
        val header = Transaction(version = 2, lockTime = 0).apply {
            isMine = true
            isOutgoing = true
        }
        val inputs = outpoints.map { (hash, index) ->
            TransactionInput(hash, index, sigScript = byteArrayOf(0x51), sequence = sequence)
        }
        val outputs = listOf(TransactionOutput(outputValue, 0, byteArrayOf(0x51), ScriptType.P2PKH))
        return FullTransaction(header, inputs, outputs).apply {
            metadata.amount = outputValue
        }
    }
}

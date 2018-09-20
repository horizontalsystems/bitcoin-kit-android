package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.Factories
import bitcoin.wallet.kit.core.hexStringToByteArray
import bitcoin.wallet.kit.headers.BlockValidator
import bitcoin.wallet.kit.managers.ProgressSyncer
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.Header
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.network.TestNet
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import junit.framework.Assert.fail
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class TransactionHandlerTest {

    private val factory = Factories()
    private val realmFactory = factory.realmFactory
    private var realm = realmFactory.realm
    private var transactionProcessor = mock(TransactionProcessor::class.java)
    private var progressSyncer = mock(ProgressSyncer::class.java)
    private var blockValidator = mock(BlockValidator::class.java)

    private lateinit var transactionHandler: TransactionHandler

    private val testHeader = Header().apply {
        version = 536870912
        prevHash = "00000000000000000000943de85f4495f053ff55f27d135edc61c27990c2eec5".hexStringToByteArray().reversedArray()
        merkleHash = "167bf70981d49388d07881b1a448ff9b79cf2a32716e45c535345823d8cdd541".hexStringToByteArray().reversedArray()
        timestamp = 1533980459
        bits = 388763047
        nonce = 1545867530

    }

    private val testHeader2 = Header().apply {
        version = 536870912
        prevHash = "00000000000000000011206e641083b68ffc41b7fe6ee1af4a5d69995d1b2d0e".hexStringToByteArray().reversedArray()
        merkleHash = "5510c0c3d1fd9d2b56a34aab98c29860015caf248fa62a1907b197ddec17c788".hexStringToByteArray().reversedArray()
        timestamp = 1535128609
        bits = 388763047
        nonce = 2295801359
    }

    @Before
    fun setup() {
        transactionHandler = TransactionHandler(realmFactory, transactionProcessor, progressSyncer, blockValidator)
    }

    @After
    fun tearDown() {
        realm.executeTransaction {
            it.deleteAll()
        }
    }

    @Test
    fun handle_blockNewValid_transactionNew() {
        val transactionNew = Transaction()

        realm.beginTransaction()
        realm.insert(Block(testHeader2, TestNet().checkpointBlock))
        realm.commitTransaction()

        transactionHandler.handle(arrayOf(transactionNew), testHeader)

        val transactionNewManaged = realm.where(Transaction::class.java).findFirst()

        Assert.assertEquals(Transaction.Status.RELAYED, transactionNewManaged?.status)
        Assert.assertArrayEquals(testHeader.hash, transactionNewManaged?.block?.headerHash)

        verify(transactionProcessor).enqueueRun()
    }

    @Test
    fun handle_blockNewValid_transactionExist() {
        val transactionExisting = Transaction()
        transactionExisting.status = Transaction.Status.NEW

        realm.beginTransaction()
        realm.insert(Block(testHeader2, TestNet().checkpointBlock))
        val transactionExistingManaged = realm.copyToRealm(transactionExisting)
        realm.commitTransaction()

        transactionHandler.handle(arrayOf(transactionExisting), testHeader)

        Assert.assertEquals(Transaction.Status.RELAYED, transactionExistingManaged.status)
        Assert.assertArrayEquals(testHeader.hash, transactionExistingManaged.block?.headerHash)

        verifyNoMoreInteractions(transactionProcessor)
    }

    @Test
    fun handle_blockNewValid() {
        realm.beginTransaction()
        realm.insert(Block(testHeader2, TestNet().checkpointBlock))
        realm.commitTransaction()

        transactionHandler.handle(arrayOf(), testHeader)

        val newBlock = realm.where(Block::class.java).equalTo("headerHash", testHeader.hash).findFirst()

        Assert.assertTrue(newBlock?.synced ?: false)
        Assert.assertArrayEquals(testHeader.hash, newBlock?.headerHash)

        verifyNoMoreInteractions(transactionProcessor)
        verify(progressSyncer).enqueueRun()
    }

    @Test
    fun handle_blockNewNotValid() {
        realm.beginTransaction()
        realm.insert(Block(testHeader2, TestNet().checkpointBlock))
        realm.commitTransaction()

        whenever(blockValidator.validate(any())).thenThrow(BlockValidator.InvalidBlock(BlockValidator.ValidatorError.WrongPreviousHeaderHash))

        try {
            transactionHandler.handle(arrayOf(), testHeader)
            fail("Expected an BlockValidator.InvalidBlock to be thrown")
        } catch (e: BlockValidator.InvalidBlock) { }

        Assert.assertEquals(0, realm.where(Block::class.java).equalTo("headerHash", testHeader.hash).count())

        verifyNoMoreInteractions(transactionProcessor)
        verifyNoMoreInteractions(progressSyncer)
    }

    @Test
    fun handle_blockNew_noPrevBlock() {

        transactionHandler.handle(arrayOf(), testHeader)

        verifyNoMoreInteractions(blockValidator)
        verifyNoMoreInteractions(transactionProcessor)
        verifyNoMoreInteractions(progressSyncer)
    }

    @Test
    fun handle_blockExist_transactionNew() {
        val transactionNew = Transaction()

        realm.beginTransaction()
        val block = realm.copyToRealm(Block(testHeader, 1000))
        realm.commitTransaction()

        transactionHandler.handle(arrayOf(transactionNew), testHeader)

        verify(transactionProcessor).enqueueRun()

        val transactionNewManaged = realm.where(Transaction::class.java).findFirst()

        Assert.assertEquals(block, transactionNewManaged?.block)
    }

    @Test
    fun handle_blockExist_transactionExist() {
        val transactionExisting = Transaction()
        transactionExisting.status = Transaction.Status.NEW

        realm.beginTransaction()
        val block = realm.copyToRealm(Block(testHeader, 1000))
        val transactionExistingManaged = realm.copyToRealm(transactionExisting)
        realm.commitTransaction()

        transactionHandler.handle(arrayOf(transactionExisting), testHeader)

        verifyNoMoreInteractions(transactionProcessor)
        Assert.assertEquals(Transaction.Status.RELAYED, transactionExistingManaged.status)
        Assert.assertEquals(block, transactionExistingManaged.block)
    }

    @Test
    fun handle_blockExistNotSyncedNoHeader() {
        realm.beginTransaction()
        val block = realm.copyToRealm(Block(testHeader.hash, 1000))
        realm.commitTransaction()

        transactionHandler.handle(arrayOf(), testHeader)

        verifyNoMoreInteractions(transactionProcessor)
        verify(progressSyncer).enqueueRun()

        Assert.assertTrue(block.synced)
        Assert.assertArrayEquals(testHeader.hash, block.header?.hash)
    }

    @Test
    fun handle_blockExistNotSynced() {
        realm.beginTransaction()
        val block = realm.copyToRealm(Block(testHeader, 1000))
        realm.commitTransaction()

        transactionHandler.handle(arrayOf(), testHeader)

        verifyNoMoreInteractions(transactionProcessor)
        verify(progressSyncer).enqueueRun()

        Assert.assertTrue(block.synced)
    }

    @Test
    fun handle_blockExistSynced() {
        realm.executeTransaction {
            it.insert(Block(testHeader, 1000).apply {
                synced = true
            })
        }

        transactionHandler.handle(arrayOf(), testHeader)

        verifyNoMoreInteractions(transactionProcessor)
        verifyNoMoreInteractions(progressSyncer)
    }
}
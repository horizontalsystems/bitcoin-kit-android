package bitcoin.wallet.kit.blocks

import bitcoin.wallet.kit.RealmFactoryMock
import bitcoin.wallet.kit.managers.AddressManager
import bitcoin.wallet.kit.models.*
import bitcoin.wallet.kit.network.MainNet
import bitcoin.wallet.kit.transactions.TransactionProcessor
import com.nhaarman.mockito_kotlin.verify
import io.realm.Realm
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class BlockSyncerDownloadCompletedTest {
    private val factories = RealmFactoryMock()
    private val realm = factories.realmFactory.realm
    private val network = MainNet()
    private val blockchainBuilder = mock(BlockchainBuilder::class.java)
    private val transactionProcessor = mock(TransactionProcessor::class.java)
    private val addressManager = mock(AddressManager::class.java)

    private lateinit var blockSyncer: BlockSyncer

    @Before
    fun setup() {
        realm.executeTransaction {
            realm.deleteAll()
        }

        blockSyncer = BlockSyncer(factories.realmFactory, blockchainBuilder, transactionProcessor, addressManager, network)
    }

    @Test
    fun merkleBlocksDownloadCompleted() {
        val block1 = Block(Header().apply {
            this.prevHash = network.checkpointBlock.headerHash
        }, realm.where(Block::class.java).findFirst()!!)

        val block2 = Block(Header().apply {
            this.prevHash = block1.headerHash
        }, block1)

        val block3 = Block(Header().apply {
            this.prevHash = block2.headerHash
        }, block2)

        val transactionInput = TransactionInput()
        val transactionOutput = TransactionOutput()
        val transaction = Transaction().apply {
            hashHexReversed = "1"
            inputs.add(transactionInput)
            outputs.add(transactionOutput)
        }

        realm.executeTransaction {
            realm.insert(listOf(
                    BlockHash(block2.headerHash, 0, 2),
                    BlockHash(block3.headerHash, 0, 3)
            ))

            realm.insert(listOf(block1, block2, block3))

            val managedBlock3 = realm.where(Block::class.java).equalTo("reversedHeaderHashHex", block3.reversedHeaderHashHex).findFirst()
            transaction.block = managedBlock3

            realm.insert(transaction)
        }

        blockSyncer.clearNotFullBlocks()

        verify(addressManager).fillGap()

        val realm1 = factories.realmFactory.realm

        assertBlockPresent(block1, realm1)
        assertBlockNotPresent(block2, realm1)
        assertBlockNotPresent(block3, realm1)
        assertTransactionNotPresent(transaction, realm1)
        assertNoTransactionOutput(realm1)
        assertNoTransactionInput(realm1)
    }

    private fun assertBlockPresent(block: Block, realm: Realm) {
        val blockInDB = realm.where(Block::class.java).equalTo("reversedHeaderHashHex", block.reversedHeaderHashHex).findFirst()
        Assert.assertNotNull(blockInDB)
        Assert.assertNotNull(blockInDB?.previousBlock)
    }

    private fun assertBlockNotPresent(block: Block, realm: Realm) {
        val blockInDB = realm.where(Block::class.java).equalTo("reversedHeaderHashHex", block.reversedHeaderHashHex).findFirst()
        Assert.assertNull(blockInDB)
    }

    private fun assertTransactionNotPresent(transaction: Transaction, realm: Realm) {
        val transactionInDB = realm.where(Transaction::class.java).equalTo("hashHexReversed", transaction.hashHexReversed).findFirst()
        Assert.assertNull(transactionInDB)
    }

    private fun assertNoTransactionOutput(realm: Realm) {
        Assert.assertEquals(0, realm.where(TransactionOutput::class.java).count())
    }

    private fun assertNoTransactionInput(realm: Realm) {
        Assert.assertEquals(0, realm.where(TransactionInput::class.java).count())
    }

}
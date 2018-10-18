package bitcoin.wallet.kit.blocks

import bitcoin.wallet.kit.RealmFactoryMock
import bitcoin.wallet.kit.managers.AddressManager
import bitcoin.wallet.kit.models.*
import bitcoin.wallet.kit.network.MainNet
import bitcoin.wallet.kit.scripts.ScriptType
import bitcoin.wallet.kit.transactions.TransactionProcessor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.realm.Realm
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class BlockSyncerHandleMerkleBlockTest {
    private val factories = RealmFactoryMock()
    private val realm = factories.realmFactory.realm
    private val network = MainNet()
    private val blockchainBuilder = mock(BlockchainBuilder::class.java)
    private val transactionProcessor = mock(TransactionProcessor::class.java)
    private val addressManager = mock(AddressManager::class.java)

    private lateinit var blockSyncer: BlockSyncer

    lateinit var block: Block
    lateinit var transactionMine: Transaction
    lateinit var transactionNotMine: Transaction
    lateinit var merkleBlock: MerkleBlock

    @Before
    fun setup() {
        realm.executeTransaction {
            realm.deleteAll()
        }

        blockSyncer = BlockSyncer(factories.realmFactory, blockchainBuilder, transactionProcessor, addressManager, network)

        block = Block(Header().apply {
            this.prevHash = network.checkpointBlock.headerHash
        }, realm.where(Block::class.java).findFirst()!!)

        transactionMine = Transaction().apply {
            hashHexReversed = "transaction1_1"
            isMine = true
        }

        transactionNotMine = Transaction().apply {
            hashHexReversed = "transaction1_2"
            isMine = false
        }

        merkleBlock = MerkleBlock().apply {
            header = block.header!!
            associatedTransactions.addAll(listOf(transactionMine, transactionNotMine))
        }

        realm.executeTransaction {
            realm.insert(BlockHash(block.headerHash, 0, 1))
        }
    }

    @Test
    fun handleMerkleBlock_transactionAlreadyInDB() {
        realm.executeTransaction {
            transactionMine.status = Transaction.Status.NEW

            realm.insert(transactionMine)
        }

        whenever(blockchainBuilder.connect(merkleBlock, realm)).thenReturn(block)

        blockSyncer.handleMerkleBlock(merkleBlock, true)

        verify(transactionProcessor).process(transactionMine, realm)

        val realm1 = factories.realmFactory.realm

        val transactionInDB = realm1.where(Transaction::class.java).equalTo("hashHexReversed", transactionMine.hashHexReversed).findFirst()
        Assert.assertEquals(Transaction.Status.RELAYED, transactionInDB?.status)
        Assert.assertEquals(block.reversedHeaderHashHex, transactionInDB?.block?.reversedHeaderHashHex)
    }

    @Test
    fun handleMerkleBlock_onlyMyTransactionsSaved() {
        whenever(blockchainBuilder.connect(merkleBlock, realm)).thenReturn(block)

        blockSyncer.handleMerkleBlock(merkleBlock, true)

        verify(transactionProcessor).process(transactionMine, realm)
        verify(transactionProcessor).process(transactionNotMine, realm)

        val realm1 = factories.realmFactory.realm

        assertTransactionSaved(transactionMine, block, realm1)
        assertTransactionNotSaved(transactionNotMine, realm1)
    }

    @Test
    fun handleMerkleBlock_hasNewOutput_exceptionThrown() {
        transactionMine.outputs.add(TransactionOutput().apply {
            scriptType = ScriptType.P2WPKH
        })

        whenever(blockchainBuilder.connect(merkleBlock, realm)).thenReturn(block)

        try {
            blockSyncer.handleMerkleBlock(merkleBlock, true)
            Assert.fail("Expected exception")
        } catch (e: BlockSyncer.Error.NextBlockNotFull) {
        }
    }

    @Test
    fun handleMerkleBlock_blockSaved() {
        whenever(blockchainBuilder.connect(merkleBlock, realm)).thenReturn(block)

        blockSyncer.handleMerkleBlock(merkleBlock, true)

        val realm1 = factories.realmFactory.realm

        assertBlockSaved(block, realm1)
    }

    @Test
    fun handleMerkleBlock_gapShiftsTrue_exceptionThrown() {
        whenever(blockchainBuilder.connect(merkleBlock, realm)).thenReturn(block)
        whenever(addressManager.gapShifts(realm)).thenReturn(true)

        try {
            blockSyncer.handleMerkleBlock(merkleBlock, true)
            Assert.fail("Expected exception")
        } catch (e: BlockSyncer.Error.NextBlockNotFull) {
        }
    }

    @Test
    fun handleMerkleBlock_FullBlock_blockHashDeleted() {
        whenever(blockchainBuilder.connect(merkleBlock, realm)).thenReturn(block)

        blockSyncer.handleMerkleBlock(merkleBlock, true)

        val realm1 = factories.realmFactory.realm
        assertBlockHashNotPresent(block.reversedHeaderHashHex, realm1)
    }

    @Test
    fun handleMerkleBlock_NotFullBlock_blockHashNotDeleted() {
        whenever(blockchainBuilder.connect(merkleBlock, realm)).thenReturn(block)

        blockSyncer.handleMerkleBlock(merkleBlock, false)

        val realm1 = factories.realmFactory.realm
        assertBlockHashPresent(block.reversedHeaderHashHex, realm1)
    }

    private fun assertBlockHashPresent(reversedHeaderHashHex: String, realm: Realm) {
        val blockHash = realm.where(BlockHash::class.java).equalTo("reversedHeaderHashHex", reversedHeaderHashHex).findFirst()
        Assert.assertNotNull(blockHash)
    }

    private fun assertBlockHashNotPresent(reversedHeaderHashHex: String, realm: Realm) {
        val blockHash = realm.where(BlockHash::class.java).equalTo("reversedHeaderHashHex", reversedHeaderHashHex).findFirst()
        Assert.assertNull(blockHash)
    }

    private fun assertTransactionSaved(transaction: Transaction, block: Block, realm: Realm) {
        val transactionInDB = realm.where(Transaction::class.java).equalTo("hashHexReversed", transaction.hashHexReversed).findFirst()
        Assert.assertNotNull(transactionInDB)
        Assert.assertEquals(block.reversedHeaderHashHex, transactionInDB?.block?.reversedHeaderHashHex)
    }

    private fun assertTransactionNotSaved(transaction: Transaction, realm: Realm) {
        val transactionInDB = realm.where(Transaction::class.java).equalTo("hashHexReversed", transaction.hashHexReversed).findFirst()
        Assert.assertNull(transactionInDB)
    }

    private fun assertBlockSaved(block: Block, realm: Realm) {
        val blockInDB = realm.where(Block::class.java).equalTo("reversedHeaderHashHex", block.reversedHeaderHashHex).findFirst()
        Assert.assertNotNull(blockInDB)
        Assert.assertNotNull(blockInDB?.previousBlock)
    }

    private fun assertBlockNotSaved(block: Block, realm: Realm) {
        val blockInDB = realm.where(Block::class.java).equalTo("reversedHeaderHashHex", block.reversedHeaderHashHex).findFirst()
        Assert.assertNull(blockInDB)
    }

}
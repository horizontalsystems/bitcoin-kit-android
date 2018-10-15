package bitcoin.wallet.kit.blocks

import bitcoin.wallet.kit.RealmFactoryMock
import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.wallet.kit.managers.AddressManager
import bitcoin.wallet.kit.models.*
import bitcoin.wallet.kit.network.MainNet
import bitcoin.wallet.kit.transactions.GapData
import bitcoin.wallet.kit.transactions.TransactionProcessor
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.realm.Realm
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class BlockSyncerTest {
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
            it.deleteAll()
        }
        blockSyncer = BlockSyncer(factories.realmFactory, blockchainBuilder, transactionProcessor, addressManager, network)
    }

    @Test
    fun handleMerkleBlocks() {
        val block1 = Block(Header().apply {
            this.prevHash = network.checkpointBlock.headerHash
        }, realm.where(Block::class.java).findFirst()!!)

        val block2 = Block(Header().apply {
            this.prevHash = block1.headerHash
        }, block1)

        val block3 = Block(Header().apply {
            this.prevHash = block2.headerHash
        }, block2)

        realm.executeTransaction {
            realm.insert(listOf(
                    BlockHash(block1.headerHash, 0, 1),
                    BlockHash(block2.headerHash, 0, 2),
                    BlockHash(block3.headerHash, 0, 3)
            ))
        }

        val blockchain = mapOf(block1.reversedHeaderHashHex to block1, block2.reversedHeaderHashHex to block2, block3.reversedHeaderHashHex to block3)

        val transactionMine = Transaction().apply {
            hashHexReversed = "transaction1_1"
            isMine = true
        }

        val transactionNotMine = Transaction().apply {
            hashHexReversed = "transaction1_2"
            isMine = false
        }

        val merkleBlock1 = MerkleBlock().apply {
            header = block1.header!!
            associatedTransactions.addAll(listOf(transactionMine, transactionNotMine))
        }

        val merkleBlock2 = MerkleBlock().apply {
            header = block2.header!!
        }

        val transactionMineInOuterBlock = Transaction().apply {
            hashHexReversed = "transaction3_1"
            isMine = true
        }

        val merkleBlock3 = MerkleBlock().apply {
            header = block3.header!!
            associatedTransactions.addAll(listOf(transactionMineInOuterBlock))
        }

        val merkleBlocks = listOf(merkleBlock1, merkleBlock2, merkleBlock3)
        val lastUsedExternalKey = mock(PublicKey::class.java)
        val lastUsedInternalKey = mock(PublicKey::class.java)
        val gapData = GapData(merkleBlock2, lastUsedExternalKey, lastUsedInternalKey)

        whenever(blockchainBuilder.buildChain(merkleBlocks, realm)).thenReturn(blockchain)
        whenever(transactionProcessor.getGapData(merkleBlocks, realm)).thenReturn(gapData)

        blockSyncer.handleMerkleBlocks(merkleBlocks)


        val realm1 = factories.realmFactory.realm

        verify(transactionProcessor).link(transactionMine, realm)
        verify(transactionProcessor).link(transactionNotMine, realm)
        verify(transactionProcessor, never()).link(transactionMineInOuterBlock, realm)

        verify(addressManager).fillGap(lastUsedExternalKey, lastUsedInternalKey)

        assertBlockSaved(block1, realm1)
        assertBlockSaved(block2, realm1)
        assertBlockNotSaved(block3, realm1)

        assertTransactionSaved(transactionMine, block1, realm1)
        assertTransactionNotSaved(transactionNotMine, realm1)
        assertTransactionNotSaved(transactionMineInOuterBlock, realm1)

        assertBlockHashNotPresent(block1.reversedHeaderHashHex, realm1)
        assertBlockHashNotPresent(block2.reversedHeaderHashHex, realm1)
        assertBlockHashPresent(block3.reversedHeaderHashHex, realm1)
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
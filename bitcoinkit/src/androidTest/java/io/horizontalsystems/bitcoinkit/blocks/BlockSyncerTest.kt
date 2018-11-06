package io.horizontalsystems.bitcoinkit.blocks

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.RealmFactoryMock
import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoinkit.managers.AddressManager
import io.horizontalsystems.bitcoinkit.managers.BloomFilterManager
import io.horizontalsystems.bitcoinkit.models.*
import io.horizontalsystems.bitcoinkit.network.MainNet
import io.horizontalsystems.bitcoinkit.transactions.TransactionProcessor
import io.realm.Realm
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class BlockSyncerTest {
    private val factories = RealmFactoryMock()
    private val realm = factories.realmFactory.realm
    private val network = MainNet()
    private val blockchain = mock(Blockchain::class.java)
    private val transactionProcessor = mock(TransactionProcessor::class.java)
    private val addressManager = mock(AddressManager::class.java)
    private val bloomFilterManager = mock(BloomFilterManager::class.java)

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

        blockSyncer = BlockSyncer(factories.realmFactory, blockchain, transactionProcessor, addressManager, bloomFilterManager, network)

        realm.executeTransaction {
            block = realm.copyToRealm(Block(Header().apply {
                this.prevHash = network.checkpointBlock.headerHash
            }, realm.where(Block::class.java).findFirst()!!))

            realm.insert(BlockHash(block.headerHash, 0, 1))
        }

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

    }

    @Test
    fun handleMerkleBlock_forceAddValidOrphanBlock() {
        val height = 100

        merkleBlock.height = height

        whenever(blockchain.forceAdd(merkleBlock, height, realm)).thenReturn(block)
        whenever(blockchain.connect(merkleBlock, realm)).thenThrow(BlockValidatorException.NoPreviousBlock())

        blockSyncer.handleMerkleBlock(merkleBlock)

        verify(blockchain).forceAdd(merkleBlock, height, realm)
    }

    @Test
    fun handleMerkleBlock_NoPreviousBlock() {
        realm.executeTransaction {
            realm.where(BlockHash::class.java)
                    .equalTo("headerHash", merkleBlock.blockHash)
                    .findFirst()
                    ?.height = 0
        }

        whenever(blockchain.connect(merkleBlock, realm)).thenThrow(BlockValidatorException.NoPreviousBlock())

        try {
            blockSyncer.handleMerkleBlock(merkleBlock)
            Assert.fail("Expected exception")
        } catch (e: BlockValidatorException.NoPreviousBlock) {
        }
    }

    @Test
    fun handleMerkleBlock_BloomFilterNotExpired_blockHashDeleted() {
        whenever(blockchain.connect(merkleBlock, realm)).thenReturn(block)

        blockSyncer.handleMerkleBlock(merkleBlock)

        verify(transactionProcessor).process(merkleBlock.associatedTransactions, block, false, realm)

        val realm1 = factories.realmFactory.realm
        assertBlockHashNotPresent(block.reversedHeaderHashHex, realm1)
    }

    @Test
    fun handleMerkleBlock_BloomFilterExpired_blockHashNotDeleted() {
        whenever(blockchain.connect(merkleBlock, realm)).thenReturn(block)
        whenever(transactionProcessor.process(merkleBlock.associatedTransactions, block, false, realm)).thenThrow(BloomFilterManager.BloomFilterExpired)

        blockSyncer.handleMerkleBlock(merkleBlock)

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

}
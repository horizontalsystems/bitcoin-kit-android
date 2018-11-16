package io.horizontalsystems.bitcoinkit.blocks

import io.horizontalsystems.bitcoinkit.RealmFactoryMock
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.network.Network
import io.realm.Realm
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class BlockchainTest {
    private val factories = RealmFactoryMock()
    private val realm = factories.realmFactory.realm
    private val network = mock(Network::class.java)
    private val blockchain = Blockchain(network)

    @Before
    fun setup() {
        realm.executeTransaction {
            realm.deleteAll()
        }
    }

    @Test
    fun handleFork_noFork() {
        val blocksInChain = mapOf(1 to "InChain1", 2 to "InChain2", 3 to "InChain3")
        val newBlocks = mapOf(4 to "NewBlock4", 5 to "NewBlock5", 6 to "NewBlock6")

        insertBlocks(blocksInChain, newBlocks)

        blockchain.handleFork(realm)

        assertBlocksPresent(blocksInChain, realm)
        assertNotStaleBlocksPresent(newBlocks, realm)
    }

    @Test
    fun handleFork_forkExist_newBlocksLonger_transactions() {
        val blocksInChain = mapOf(1 to "InChain1", 2 to "InChain2", 3 to "InChain3")
        val newBlocks = mapOf(2 to "NewBlock2", 3 to "NewBlock3", 4 to "NewBlock4")

        insertBlocks(blocksInChain, newBlocks)

        blockchain.handleFork(realm)

        assertBlocksPresent(mapOf(1 to "InChain1"), realm)
        assertBlocksNotPresent(mapOf(2 to "InChain2", 3 to "InChain3"), realm)
        assertNotStaleBlocksPresent(newBlocks, realm)
    }

    @Test
    fun handleFork_forkExist_newBlocksShorter() {
        val blocksInChain = mapOf(1 to "InChain1", 2 to "InChain2", 3 to "InChain3", 4 to "InChain4")
        val newBlocks = mapOf(2 to "NewBlock2", 3 to "NewBlock3")

        insertBlocks(blocksInChain, newBlocks)

        blockchain.handleFork(realm)

        assertBlocksPresent(blocksInChain, realm)
        assertBlocksNotPresent(newBlocks, realm)
    }

    @Test
    fun handleFork_forkExist_newBlocksEqual() {
        val blocksInChain = mapOf(1 to "InChain1", 2 to "InChain2", 3 to "InChain3")
        val newBlocks = mapOf(2 to "NewBlock2", 3 to "NewBlock3")

        insertBlocks(blocksInChain, newBlocks)

        blockchain.handleFork(realm)

        assertBlocksPresent(blocksInChain, realm)
        assertBlocksNotPresent(newBlocks, realm)
    }

    @Test
    fun handleFork_noNewBlocks() {
        val blocksInChain = mapOf(1 to "InChain1", 2 to "InChain2", 3 to "InChain3")
        val newBlocks = mapOf<Int, String>()

        insertBlocks(blocksInChain, newBlocks)

        blockchain.handleFork(realm)

        assertBlocksPresent(blocksInChain, realm)
    }

    @Test
    fun handleFork_forkExist_noBlocksInChain() {
        val blocksInChain = mapOf<Int, String>()
        val newBlocks = mapOf(2 to "NewBlock2", 3 to "NewBlock3", 4 to "NewBlock4")

        insertBlocks(blocksInChain, newBlocks)

        blockchain.handleFork(realm)

        assertNotStaleBlocksPresent(newBlocks, realm)
    }

    private fun assertBlocksPresent(blocksInChain: Map<Int, String>, realm: Realm) {
        blocksInChain.forEach { (height, id) ->
            Assert.assertEquals("Block $id($height) not found", 1, realm.where(Block::class.java).equalTo("height", height).equalTo("reversedHeaderHashHex", id).count())
            Assert.assertEquals("Transaction $id($height) not found", 1, realm.where(Transaction::class.java).equalTo("hashHexReversed", id).count())
        }
    }

    private fun assertBlocksNotPresent(blocksInChain: Map<Int, String>, realm: Realm) {
        blocksInChain.forEach { (height, id) ->
            Assert.assertEquals("Block $id($height) should not present", 0, realm.where(Block::class.java).equalTo("height", height).equalTo("reversedHeaderHashHex", id).count())
            Assert.assertEquals("Transaction $id($height) should not present", 0, realm.where(Transaction::class.java).equalTo("hashHexReversed", id).count())
        }
    }

    private fun assertNotStaleBlocksPresent(blocksInChain: Map<Int, String>, realm: Realm) {
        blocksInChain.forEach { (height, id) ->
            Assert.assertEquals("Not stale block $id($height) not found", 1, realm.where(Block::class.java).equalTo("stale", false).equalTo("height", height).equalTo("reversedHeaderHashHex", id).count())
            Assert.assertEquals("Transaction $id($height) not found", 1, realm.where(Transaction::class.java).equalTo("hashHexReversed", id).count())
        }
    }

    private fun insertBlocks(blocksInChain: Map<Int, String>, newBlocks: Map<Int, String>) {
        realm.executeTransaction {
            blocksInChain.forEach { (height, id) ->
                val block = realm.copyToRealm(Block().apply {
                    reversedHeaderHashHex = id
                    this.height = height
                    stale = false
                })

                realm.insert(Transaction().apply {
                    hashHexReversed = id
                    this.block = block
                })
            }

            newBlocks.forEach { (height, id) ->
                val block = realm.copyToRealm(Block().apply {
                    reversedHeaderHashHex = id
                    this.height = height
                    stale = true
                })

                realm.insert(Transaction().apply {
                    hashHexReversed = id
                    this.block = block
                })
            }
        }
    }

}

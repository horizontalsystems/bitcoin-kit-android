package io.horizontalsystems.dogecoinkit

import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.utils.HashUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MainNetDogecoinTest {

    private val network = MainNetDogecoin()

    /**
     * Checkpoint resources are looked up by the network class's simple name, so a rename or a
     * misspelled file only shows up when a wallet is first created. Loading them here instead.
     */
    @Test
    fun loadsCheckpoints() {
        assertEquals(6_370_000, network.lastCheckpoint.block.height)
        assertEquals(AuxPoW.AUXPOW_START_HEIGHT, network.bip44Checkpoint.block.height)
    }

    /**
     * Every checkpoint header must hash to the hash stored alongside it, which is what makes a
     * hand-assembled checkpoint file safe to trust.
     */
    @Test
    fun checkpointHeadersHashToTheirStoredHash() {
        listOf(network.lastCheckpoint, network.bip44Checkpoint).forEach { checkpoint ->
            (listOf(checkpoint.block) + checkpoint.additionalBlocks).forEach { block ->
                assertArrayEquals(
                    "checkpoint at height ${block.height} does not hash to its stored hash",
                    block.headerHash,
                    HashUtils.doubleSha256(block.header().toByteArray())
                )
            }
        }
    }

    /**
     * The bip44 checkpoint sits on the first merge-mined block, so it doubles as a fixture: its
     * version must carry the AuxPoW flag and Dogecoin's chain id.
     */
    @Test
    fun bip44CheckpointIsTheFirstMergeMinedBlock() {
        val block = network.bip44Checkpoint.block

        assertEquals(
            "60323982f9c5ff1b5a954eac9dc1269352835f47c2c5222691d80f0d50dcf053",
            block.headerHash.toReversedHex()
        )
        assertEquals(true, AuxPoW.hasAuxPoW(block.version))
        assertEquals(0x0062, block.version ushr 16)
    }

    /**
     * The kit installs no block validators, so Api and Full sync would accept a peer's header
     * chain unverified. They have to fail loudly rather than sync against an unchecked chain.
     */
    @Test
    fun acceptsOnlyBlockchairSyncMode() {
        DogecoinKit.requireSupportedSyncMode(BitcoinCore.SyncMode.Blockchair())

        listOf(BitcoinCore.SyncMode.Api(), BitcoinCore.SyncMode.Full()).forEach { syncMode ->
            try {
                DogecoinKit.requireSupportedSyncMode(syncMode)
                fail("expected ${syncMode.javaClass.simpleName} to be rejected")
            } catch (e: DogecoinKit.UnsupportedSyncMode) {
                assertTrue(e.message!!.contains(syncMode.javaClass.simpleName))
            }
        }
    }

    @Test
    fun hasDogecoinNetworkParameters() {
        assertEquals(0xc0c0c0c0L, network.magic)
        assertEquals(22556, network.port)
        assertEquals(30, network.addressVersion)
        assertEquals(22, network.addressScriptVersion)
        assertEquals(3, network.coinType)
        assertEquals("dogecoin", network.blockchairChainId)
    }

    private fun io.horizontalsystems.bitcoincore.models.Block.header() =
        io.horizontalsystems.bitcoincore.io.BitcoinOutput().also {
            it.writeInt(version)
            it.write(previousBlockHash)
            it.write(merkleRoot)
            it.writeUnsignedInt(timestamp)
            it.writeUnsignedInt(bits)
            it.writeUnsignedInt(nonce)
        }
}

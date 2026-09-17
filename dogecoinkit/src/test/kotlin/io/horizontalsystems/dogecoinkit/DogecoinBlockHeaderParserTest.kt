package io.horizontalsystems.dogecoinkit

import io.horizontalsystems.bitcoincore.core.DoubleSha256Hasher
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.serializers.TransactionSerializer
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.EOFException

/**
 * Block fixtures are the serialized mainnet blocks from libdohj (Apache-2.0), which uses the same
 * five blocks to cover the pre-AuxPoW era, the merge-mining fork itself, and blocks after it.
 *
 * The load-bearing assertion in every case is [parseWholeBlock] reaching exactly the end of the
 * file. The AuxPoW structure has no length prefix, so if the skip were off by even one byte the
 * transaction count would be read from the wrong place and either the parse would throw or bytes
 * would be left over. Matching the known block hash on its own would not catch that, because the
 * hash only covers the first 80 bytes.
 */
class DogecoinBlockHeaderParserTest {

    private val parser = DogecoinBlockHeaderParser(DoubleSha256Hasher())

    @Test
    fun parsesBlock1_beforeMergeMining() {
        val (header, txCount) = parseWholeBlock("dogecoin_block1")

        assertEquals("82bc68038f6034c0596b6e313729793a887fded6e92a31fbdf70863f89d9bea2", header.hash.toReversedHex())
        assertEquals(1, txCount)
        assertEquals(0x1e0ffff0L, header.bits)
        assertFalse(AuxPoW.hasAuxPoW(header.version))
    }

    @Test
    fun parsesBlock250000_beforeMergeMining() {
        val (header, txCount) = parseWholeBlock("dogecoin_block250000")

        assertEquals("0e4bcfe8d970979f7e30e2809ab51908d435677998cf759169407824d4f36460", header.hash.toReversedHex())
        assertEquals(6, txCount)
        assertEquals(2469341065L, header.nonce)
        assertFalse(AuxPoW.hasAuxPoW(header.version))
    }

    @Test
    fun parsesBlock371337_theFirstMergeMinedBlock() {
        val (header, txCount) = parseWholeBlock("dogecoin_block371337")

        assertEquals("60323982f9c5ff1b5a954eac9dc1269352835f47c2c5222691d80f0d50dcf053", header.hash.toReversedHex())
        assertEquals(6, txCount)
        assertTrue(AuxPoW.hasAuxPoW(header.version))
        assertEquals(DOGECOIN_CHAIN_ID, header.version ushr 16)
        // The nonce lives in the parent header once a block is merge-mined.
        assertEquals(0L, header.nonce)
    }

    @Test
    fun parsesBlock748634_mergeMined() {
        val (header, _) = parseWholeBlock("dogecoin_block748634")

        assertEquals("bd98a06391115285265c04984e8505229739f6ffa5d498929a91fbe7c281ea7b", header.hash.toReversedHex())
        assertTrue(AuxPoW.hasAuxPoW(header.version))
        assertEquals(DOGECOIN_CHAIN_ID, header.version ushr 16)
    }

    @Test
    fun parsesBlock894863_mergeMined() {
        val (header, _) = parseWholeBlock("dogecoin_block894863")

        assertEquals("93a207e6d227f4d60ee64fad584b47255f654b0b6378d78e774123dd66f4fef9", header.hash.toReversedHex())
        assertTrue(AuxPoW.hasAuxPoW(header.version))
        assertEquals(DOGECOIN_CHAIN_ID, header.version ushr 16)
    }

    /**
     * Regression guard. The Dogecoin wallet that shipped on Android tested the version for equality
     * against 0x00620102, which matches no block mined today — every current block is 0x00620104,
     * so that check silently stopped finding AuxPoW at all. The flag is a bit, not a value.
     */
    @Test
    fun detectsAuxPoWByBitmaskNotEquality() {
        assertTrue(AuxPoW.hasAuxPoW(0x00620102))   // the fork-era version
        assertTrue(AuxPoW.hasAuxPoW(0x00620104))   // what mainnet produces today
        assertTrue(AuxPoW.hasAuxPoW(0x00620004 or 0x100))

        assertFalse(AuxPoW.hasAuxPoW(1))
        assertFalse(AuxPoW.hasAuxPoW(2))
        assertFalse(AuxPoW.hasAuxPoW(0x00620004))  // chain id set, flag clear
    }

    /**
     * The merkleblock parser is the one the app actually exercises, since Blockchair sync fetches
     * merkle blocks and never sends getheaders. Its body is appended to a real merge-mined header
     * so that reading it back proves the AuxPoW skip landed on the right byte.
     */
    @Test
    fun merkleBlockParserReadsBodyAfterAuxPoW() {
        val prefix = headerAndAuxPoWBytes("dogecoin_block371337")

        val hashes = listOf(ByteArray(32) { 0x11 }, ByteArray(32) { 0x22 })
        val flags = byteArrayOf(0x1d)
        val body = BitcoinOutput().apply {
            writeInt(6)                     // total transactions in the block
            writeVarInt(hashes.size.toLong())
            hashes.forEach { write(it) }
            writeVarInt(flags.size.toLong())
            write(flags)
        }.toByteArray()

        val message = DogecoinMerkleBlockMessageParser(parser)
            .parseMessage(BitcoinInputMarkable(prefix + body)) as io.horizontalsystems.bitcoincore.network.messages.MerkleBlockMessage

        assertEquals(
            "60323982f9c5ff1b5a954eac9dc1269352835f47c2c5222691d80f0d50dcf053",
            message.header.hash.toReversedHex()
        )
        assertEquals(6, message.txCount)
        assertEquals(2, message.hashCount)
        assertArrayEquals(hashes[0], message.hashes[0])
        assertArrayEquals(hashes[1], message.hashes[1])
        assertArrayEquals(flags, message.flags)
    }

    @Test
    fun headersMessageParserReadsConsecutiveMergeMinedHeaders() {
        val first = headerAndAuxPoWBytes("dogecoin_block748634")
        val second = headerAndAuxPoWBytes("dogecoin_block894863")

        // A headers message is each header followed by a zero transaction count.
        val payload = BitcoinOutput().apply {
            writeVarInt(2)
            write(first); writeVarInt(0)
            write(second); writeVarInt(0)
        }.toByteArray()

        val message = DogecoinHeadersMessageParser(parser)
            .parseMessage(BitcoinInputMarkable(payload)) as io.horizontalsystems.bitcoincore.network.messages.HeadersMessage

        assertEquals(2, message.headers.size)
        assertEquals("bd98a06391115285265c04984e8505229739f6ffa5d498929a91fbe7c281ea7b", message.headers[0].hash.toReversedHex())
        assertEquals("93a207e6d227f4d60ee64fad584b47255f654b0b6378d78e774123dd66f4fef9", message.headers[1].hash.toReversedHex())
    }

    @Test(expected = AuxPoW.InvalidAuxPoW::class)
    fun rejectsAbsurdMerkleBranchLength() {
        // A hostile peer claiming a 10,000-entry merkle branch must be refused before we start
        // allocating for it, rather than after the reads run off the end of the message.
        val payload = BitcoinOutput().apply {
            write(minimalCoinbase())
            write(ByteArray(32))    // parent block hash
            writeVarInt(10_000)     // absurd coinbase branch length
        }.toByteArray()

        AuxPoW.skip(BitcoinInputMarkable(payload))
    }

    /** The smallest transaction TransactionSerializer will round-trip: one input, one output. */
    private fun minimalCoinbase(): ByteArray = BitcoinOutput().apply {
        writeInt(1)                     // version
        writeVarInt(1)                  // input count
        write(ByteArray(32))            // prevout hash
        writeUnsignedInt(0xffffffffL)   // prevout index
        writeVarInt(2); write(byteArrayOf(0x51, 0x52))  // scriptSig
        writeUnsignedInt(0xffffffffL)   // sequence
        writeVarInt(1)                  // output count
        writeLong(0)                    // value
        writeVarInt(1); write(byteArrayOf(0x51))        // scriptPubKey
        writeUnsignedInt(0)             // locktime
    }.toByteArray()

    // --- helpers -------------------------------------------------------------------------------

    /**
     * Parses the header, then every transaction, and asserts the block ends exactly there.
     */
    private fun parseWholeBlock(name: String): Pair<BlockHeader, Int> {
        val input = BitcoinInputMarkable(fixture(name))

        val header = parser.parse(input)
        val txCount = input.readVarInt().toInt()
        repeat(txCount) {
            TransactionSerializer.deserialize(input)
        }

        val leftover = remainingBytes(input)
        if (leftover > 0) {
            fail("$name: $leftover bytes left over after the last transaction — the AuxPoW skip is misaligned")
        }

        return header to txCount
    }

    /**
     * The 80 byte header plus the AuxPoW that follows it, i.e. everything the parser consumes.
     */
    private fun headerAndAuxPoWBytes(name: String): ByteArray {
        val bytes = fixture(name)
        val input = BitcoinInputMarkable(bytes)
        parser.parse(input)
        return bytes.copyOfRange(0, bytes.size - remainingBytes(input))
    }

    private fun remainingBytes(input: BitcoinInputMarkable): Int {
        var count = 0
        try {
            while (true) {
                input.readBytes(1)
                count++
            }
        } catch (e: EOFException) {
            // reached the end
        }
        return count
    }

    private fun fixture(name: String): ByteArray =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("$name.bin")) { "missing fixture $name.bin" }
            .use { it.readBytes() }

    companion object {
        private const val DOGECOIN_CHAIN_ID = 0x0062
    }
}

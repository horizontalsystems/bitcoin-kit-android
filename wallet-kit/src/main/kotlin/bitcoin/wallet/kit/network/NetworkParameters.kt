package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.models.Block
import bitcoin.walllet.kit.utils.HashUtils

/**
 * <p>NetworkParameters contains the data needed for working with an instantiation of a Bitcoin chain.</p>
 */

abstract class NetworkParameters {

    /** The string returned by getId() for the main, production network where people trade things.  */
    val ID_MAINNET = "org.bitcoin.production"
    /** The string returned by getId() for the testnet.  */
    val ID_TESTNET = "org.bitcoin.test"
    /** The string returned by getId() for regtest mode.  */
    val ID_REGTEST = "org.bitcoin.regtest"

    /** The string used by the payment protocol to represent the main net.  */
    val PAYMENT_PROTOCOL_ID_MAINNET = "main"
    /** The string used by the payment protocol to represent the test net.  */
    val PAYMENT_PROTOCOL_ID_TESTNET = "test"
    /** The string used by the payment protocol to represent the regtest net.  */
    val PAYMENT_PROTOCOL_ID_REGTEST = "regtest"

    abstract var id: String

    abstract var port: Int

    // Indicates message origin network and is used to seek to the next message when stream state is unknown.
    abstract var packetMagic: Long

    abstract var bip32HeaderPub: Int
    abstract var bip32HeaderPriv: Int
    abstract var addressHeader: Int
    abstract var scriptAddressHeader: Int
    abstract var coinType: Int

    abstract var dnsSeeds: Array<String>

    abstract var paymentProtocolId: String

    fun isMainNet(): Boolean {
        return id == ID_MAINNET
    }

    /**
     * Bitcoin protocol version.
     */
    val protocolVersion = 70014

    /**
     * Bitcoin protocol version since which Bloom filtering is available
     */
    val bloomFilter = 70000

    /**
     * Network services.
     */
    val networkServices = 1L

    /**
     * A service bit that denotes whether the peer has a copy of the block chain or not.
     */
    val serviceFullNode = 1L

    /**
     * Hash bytes as "00000000...0000"
     */
    val zeroHashBytes = HashUtils
            .toBytesAsLE("0000000000000000000000000000000000000000000000000000000000000000")

    abstract val checkpointBlock: Block

    abstract fun validate(block: Block)

    fun magicAsUInt32ByteArray(): ByteArray {
        val magic = packetMagic
        val bytes = ByteArray(4)
        bytes[3] = (magic and 0xFFFF).toByte()
        bytes[2] = ((magic ushr 8) and 0xFFFF).toByte()
        bytes[1] = ((magic ushr 16) and 0xFFFF).toByte()
        bytes[0] = ((magic ushr 24) and 0xFFFF).toByte()
        return bytes
    }
}

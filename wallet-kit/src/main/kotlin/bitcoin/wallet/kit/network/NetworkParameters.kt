package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.models.Block
import bitcoin.walllet.kit.utils.HashUtils

/** Network-specific parameters */
abstract class NetworkParameters {

    // The strings returned by getId() for networks
    val ID_MAINNET = "org.bitcoin.production"
    val ID_TESTNET = "org.bitcoin.test"
    val ID_REGTEST = "org.bitcoin.regtest"

    val protocolVersion = 70014
    val bloomFilter = 70000
    val networkServices = 1L
    val serviceFullNode = 1L
    val zeroHashBytes = HashUtils.toBytesAsLE("0000000000000000000000000000000000000000000000000000000000000000")

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

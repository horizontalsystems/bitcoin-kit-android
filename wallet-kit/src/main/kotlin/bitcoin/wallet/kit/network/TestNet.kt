package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.Header
import bitcoin.walllet.kit.utils.HashUtils

open class TestNet : NetworkParameters() {

    override var id: String = ID_TESTNET

    override var port: Int = 18333

    override var packetMagic: Long = 0x0b110907

    override var bip32HeaderPub: Int = 0x043587CF

    override var bip32HeaderPriv: Int = 0x04358394

    override var addressHeader: Int = 111

    override var scriptAddressHeader: Int = 196

    override var coinType: Int = 1

    override var dnsSeeds: Array<String> = arrayOf(
            "testnet-seed.bitcoin.petertodd.org",    // Peter Todd
            "testnet-seed.bitcoin.jonasschnelli.ch", // Jonas Schnelli
            "testnet-seed.bluematt.me",              // Matt Corallo
            "testnet-seed.bitcoin.schildbach.de",    // Andreas Schildbach
            "bitcoin-testnet.bloqseeds.net"         // Bloq
    )

    override var paymentProtocolId: String = PAYMENT_PROTOCOL_ID_TESTNET

    override val checkpointBlock = Block().apply {
        height = 1380960
        header = Header().apply {
            version = 536870912
            prevHash = HashUtils.toBytesAsLittleEndian("000000000000032d74ad8eb0a0be6b39b8e095bd9ca8537da93aae15087aafaf")
            merkleHash = HashUtils.toBytesAsLittleEndian("dec6a6b395b29be37f4b074ed443c3625fac3ae835b1f1080155f01843a64268")
            timestamp = 1533498326
            bits = 436270990
            nonce = 205753354
        }
    }

}

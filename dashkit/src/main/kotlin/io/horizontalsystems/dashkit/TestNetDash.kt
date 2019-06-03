package io.horizontalsystems.dashkit

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils

class TestNetDash : Network() {

    override val protocolVersion = 70214

    override var port: Int = 19999

    override var magic: Long = 0xffcae2ce
    override var bip32HeaderPub: Int = 0x0488B21E   // The 4 byte header that serializes in base58 to "xpub".
    override var bip32HeaderPriv: Int = 0x0488ADE4  // The 4 byte header that serializes in base58 to "xprv"
    override var addressVersion: Int = 140
    override var addressSegwitHrp: String = "bc"
    override var addressScriptVersion: Int = 19
    override var coinType: Int = 1

    override val maxBlockSize = 1_000_000

    override var dnsSeeds = listOf(
            "testnet-seed.dashdot.io",
            "test.dnsseed.masternode.io"
    )

    override val checkpointBlock = Block(BlockHeader(
            version = 536870912,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("0000000006616ee85366fabce00a28af650519eb1c6106d613ce3678947a42fb"),
            merkleRoot = HashUtils.toBytesAsLE("fe39bdb86999ba1eaca10e56bf12528c9cce278c8dde66f399605d8e79e12fe6"),
            timestamp = 1554724358,
            bits = 0x1c09e0a2,
            nonce = 3017212751,
            hash = HashUtils.toBytesAsLE("00000000064ca5bc01e45950d863fb7e938bdb9ecdec698e8d7acfb79a57d15e")
    ), 75900)

}

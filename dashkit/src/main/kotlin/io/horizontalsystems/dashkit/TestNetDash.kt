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

    override val bip44CheckpointBlock = Block(BlockHeader(
            version = 536870912,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("0000000006616ee85366fabce00a28af650519eb1c6106d613ce3678947a42fb"),
            merkleRoot = HashUtils.toBytesAsLE("fe39bdb86999ba1eaca10e56bf12528c9cce278c8dde66f399605d8e79e12fe6"),
            timestamp = 1554724358,
            bits = 0x1c09e0a2,
            nonce = 3017212751,
            hash = HashUtils.toBytesAsLE("00000000064ca5bc01e45950d863fb7e938bdb9ecdec698e8d7acfb79a57d15e")
    ), 75900)

    override val lastCheckpointBlock = Block(BlockHeader(
            version = 536870912,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("000000001099bd5d3c903f2ab865b2c49c8bd29bddc9c990db43acd99617362c"),
            merkleRoot = HashUtils.toBytesAsLE("e58aeda83f17834baedb488c5276a37376c61c375848761f9a02c1981fe0d507"),
            timestamp = 1559651035,
            bits = 0x1c0f8fa9,
            nonce = 1118140024,
            hash = HashUtils.toBytesAsLE("000000000cf1ebc27139b55559f2a0e312e566e1fd7dcac7ccf4e58d973794f5")
    ), 111324)

}

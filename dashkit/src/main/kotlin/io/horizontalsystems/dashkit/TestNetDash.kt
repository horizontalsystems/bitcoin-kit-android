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
            version = 1,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("0000000000000000000000000000000000000000000000000000000000000000"),
            merkleRoot = HashUtils.toBytesAsLE("4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b"),
            timestamp = 1231006505,
            bits = 486604799,
            nonce = 2083236893,
            hash = HashUtils.toBytesAsLE("00000bafbc94add76cb75e2ec92894837288a481e5c005f6563d91623bf8bc2c")
    ), 0)

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

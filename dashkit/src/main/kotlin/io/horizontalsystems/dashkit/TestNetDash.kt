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
            previousBlockHeaderHash = HashUtils.toBytesAsLE("000000000e34afe7600a439c89dbbb90908a6bf2bc117dcf30e82c89a83ec280"),
            merkleRoot = HashUtils.toBytesAsLE("334130f690b9e58bfc61c767c89251913e87dd7a44a1eb30bdd0668d85313527"),
            timestamp = 1566527727,
            bits = 0x1c0f2298,
            nonce = 958408694,
            hash = HashUtils.toBytesAsLE("000000000d5cbf42cd0da22e4a2dc4aab275a5e8d5a8ab39025b1bd2d588ebfb")
    ), 160834)

}

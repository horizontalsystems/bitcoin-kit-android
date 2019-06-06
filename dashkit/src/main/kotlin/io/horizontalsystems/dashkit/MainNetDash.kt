package io.horizontalsystems.dashkit

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils

class MainNetDash : Network() {

    override val protocolVersion = 70214

    override var port: Int = 9999

    override var magic: Long = 0xbd6b0cbf
    override var bip32HeaderPub: Int = 0x0488B21E   // The 4 byte header that serializes in base58 to "xpub".
    override var bip32HeaderPriv: Int = 0x0488ADE4  // The 4 byte header that serializes in base58 to "xprv"
    override var addressVersion: Int = 76
    override var addressSegwitHrp: String = "bc"
    override var addressScriptVersion: Int = 16
    override var coinType: Int = 5

    override val maxBlockSize = 1_000_000

    override var dnsSeeds = listOf(
            "dnsseed.dash.org",
            "dnsseed.dashdot.io",
            "dnsseed.masternode.io"
    )

    override val bip44CheckpointBlock = Block(BlockHeader(
            version = 1,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("0000000000000000000000000000000000000000000000000000000000000000"),
            merkleRoot = HashUtils.toBytesAsLE("4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b"),
            timestamp = 1231006505,
            bits = 486604799,
            nonce = 2083236893,
            hash = HashUtils.toBytesAsLE("00000ffd590b1485b3caadc19b22e6379c733355108f107a430458cdf3407ab6")
    ), 0)

    override val lastCheckpointBlock = Block(BlockHeader(
            version = 536870928,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("000000000000000fcbac491b68a0774d1b9f82edeae8742eb492815e8fa76ca5"),
            merkleRoot = HashUtils.toBytesAsLE("91e15e6045c20d06abc41eb5feb17813ccc723f6f018ca8fd01485e8837bc761"),
            timestamp = 1559624664,
            bits = 0x191a414a,
            nonce = 838341360,
            hash = HashUtils.toBytesAsLE("00000000000000011ce58a8bb55333277640b015e97689f9277d582a4c1f9999")
    ), 1081358)
}

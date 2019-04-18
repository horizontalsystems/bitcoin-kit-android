package io.horizontalsystems.bitcoinkit.dash

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils

class MainNetDash : Network() {

    override val protocolVersion = 70213

    override var port: Int = 9999

    override var magic: Long = 0xbd6b0cbf
    override var bip32HeaderPub: Int = 0x0488B21E   // The 4 byte header that serializes in base58 to "xpub".
    override var bip32HeaderPriv: Int = 0x0488ADE4  // The 4 byte header that serializes in base58 to "xprv"
    override var addressVersion: Int = 76
    override var addressSegwitHrp: String = "bc"
    override var addressScriptVersion: Int = 16
    override var coinType: Int = 5

    override val maxBlockSize = 1_000_000

    override var dnsSeeds: Array<String> = arrayOf(
            "dnsseed.dash.org",
            "dnsseed.dashdot.io",
            "dnsseed.masternode.io"
    )

    override val checkpointBlock = Block(BlockHeader(
            536870912,
            HashUtils.toBytesAsLE("000000000000000992e45d7b6d5204e40b24474db7c107e7b1e4884f3e76462c"),
            HashUtils.toBytesAsLE("61694834cfd431c70975645849caff2e1bfb4c487706cf217129fd4371cd7a79"),
            1551689319L,
            0x193f7bf8,
            2813674015,
            HashUtils.toBytesAsLE("00000000000000243001bbc7deafb49dc28738204d8a237852aacb19cb262474")
    ), 1030968)

}

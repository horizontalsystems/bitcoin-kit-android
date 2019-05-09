package io.horizontalsystems.dashkit

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
            version = 536870912,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("0000000000000008e256ece9efbd7ce5b23391dba1129a55143b9ae90841bfc1"),
            merkleRoot = HashUtils.toBytesAsLE("c24e8f73fa72fcbf510f48ee198243fb8d08895d95814e8f8da00dc8d463608a"),
            timestamp = 1551689319L,
            bits = 0x193f7bf8,
            nonce = 2813674015,
            hash = HashUtils.toBytesAsLE("000000000000000d1f01d32f7d3b2813d8543377668f82811cfbbe89cb7f50fe")
    ), 1066693)

}

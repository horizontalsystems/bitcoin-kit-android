package io.horizontalsystems.bitcoinkit.dash

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils

class TestNetDash : Network() {

    override val protocolVersion = 70213

    override var port: Int = 19999

    override var magic: Long = 0xffcae2ce
    override var bip32HeaderPub: Int = 0x0488B21E   // The 4 byte header that serializes in base58 to "xpub".
    override var bip32HeaderPriv: Int = 0x0488ADE4  // The 4 byte header that serializes in base58 to "xprv"
    override var addressVersion: Int = 140
    override var addressSegwitHrp: String = "bc"
    override var addressScriptVersion: Int = 19
    override var coinType: Int = 1

    override val maxBlockSize = 1_000_000

    override var dnsSeeds: Array<String> = arrayOf(
            "testnet-seed.dashdot.io",
            "test.dnsseed.masternode.io"
    )

    override val checkpointBlock = Block(BlockHeader(
            536870912,
            HashUtils.toBytesAsLE("00000025a533a276a43aaacc27d44f1e599f07fde18b8348c1355a9bcf0ea339"),
            HashUtils.toBytesAsLE("fe39bdb86999ba1eaca10e56bf12528c9cce278c8dde66f399605d8e79e12fe6"),
            1551699279,
            0x1d312d59,
            4281733120,
            HashUtils.toBytesAsLE("0000000f10a125d1d97784028be7c3b737e21a3ab76d59a60f8d244ab548de14")
    ), 55032)

}

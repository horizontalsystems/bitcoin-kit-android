package io.horizontalsystems.bitcoincash

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.transactions.scripts.Sighash
import io.horizontalsystems.bitcoincore.utils.HashUtils
import kotlin.experimental.or

class MainNetBitcoinCash : Network() {

    override var port: Int = 8333

    override var magic: Long = 0xe8f3e1e3L
    override var bip32HeaderPub: Int = 0x0488b21e
    override var bip32HeaderPriv: Int = 0x0488ade4
    override var addressVersion: Int = 0
    override var addressSegwitHrp: String = "bitcoincash"
    override var addressScriptVersion: Int = 5
    override var coinType: Int = 0

    override val maxBlockSize = 32 * 1024 * 1024
    override val sigHashForked = true
    override val sigHashValue = Sighash.FORKID or Sighash.ALL

    override var dnsSeeds = listOf(
            "seed.bitcoinabc.org",                  // Bitcoin ABC seeder
            "seed-abc.bitcoinforks.org",            // bitcoinforks seeders
            "btccash-seeder.bitcoinunlimited.info", // BU backed seeder
            "seed.bitprim.org",                     // Bitprim
            "seed.deadalnix.me",                    // Amaury SÉCHET
            "seeder.criptolayer.net"                // criptolayer.net
    )

    private val blockHeader = BlockHeader(
            version = 805289984,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("000000000000000000e1f8ea917f17c378fdfd8d13f23160c6cb522d406c37ab"),
            merkleRoot = HashUtils.toBytesAsLE("923fb5f581b3dfe4bc6103891c63e5789abbaac7d8fcd2ba4b25ac2abccdba9c"),
            timestamp = 1557394860,
            bits = 402883015,
            nonce = 3963128149,
            hash = HashUtils.toBytesAsLE("0000000000000000030e41502adfbcb20fdca66b15cc9e157449585c6c85da6e")
    )

    override val checkpointBlock = Block(blockHeader, 581790)
}

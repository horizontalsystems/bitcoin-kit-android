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
    override val dustRelayTxFee = 1000 // https://github.com/Bitcoin-ABC/bitcoin-abc/blob/master/src/policy/policy.h#L78
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

    override val bip44CheckpointBlock = Block(BlockHeader(
            version = 2,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("00000000000000006bcf448b771c8f4db4e2ca653474e3b29504ec08422b3fba"),
            merkleRoot = HashUtils.toBytesAsLE("4ea18e999a57fc55fb390558dbb88a7b9c55c71c7de4cec160c045802ee587d2"),
            timestamp = 1397755646,
            bits = 419470732,
            nonce = 2160181286,
            hash = HashUtils.toBytesAsLE("00000000000000003decdbb5f3811eab3148fbc29d3610528eb3b50d9ee5723f")
    ), 296352)

    override val lastCheckpointBlock = Block(BlockHeader(
            version = 536870912,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("00000000000000000264b391cca605b0dcebcb22e4c7b243240db68586ec72ca"),
            merkleRoot = HashUtils.toBytesAsLE("efdce6583f7b16f3dcb585625ec0c2eb1fdb81aef0e3e3bd5cdd0ae2154e645f"),
            timestamp = 1573034827,
            bits = 402835824,
            nonce = 1099862827,
            hash = HashUtils.toBytesAsLE("00000000000000000252f670239ac6c123c321a54ca8fb0f853b86a48bf41b58")
    ), 607845)
}

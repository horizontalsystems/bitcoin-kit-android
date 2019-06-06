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
            version = 0x2000e000,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("000000000000000002a1f5acfab47e5e1afcac9f50eb9b7c875e6c736d099763"),
            merkleRoot = HashUtils.toBytesAsLE("e6a8e517f708d294f426895c255cfd0a443d7f55a768b04398eadde0c516027c"),
            timestamp = 1559650598,
            bits = 0x1803769a,
            nonce = 0xed7bb8ff,
            hash = HashUtils.toBytesAsLE("00000000000000000040f26002e04126dc84700d6f82c0785efab2293080fe68")
    ), 585504)
}

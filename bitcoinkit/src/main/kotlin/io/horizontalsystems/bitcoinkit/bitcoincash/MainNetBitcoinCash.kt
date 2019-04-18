package io.horizontalsystems.bitcoinkit.bitcoincash

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.utils.HashUtils

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

    override var dnsSeeds: Array<String> = arrayOf(
            "seed.bitcoinabc.org",                  // Bitcoin ABC seeder
            "seed-abc.bitcoinforks.org",            // bitcoinforks seeders
            "btccash-seeder.bitcoinunlimited.info", // BU backed seeder
            "seed.bitprim.org",                     // Bitprim
            "seed.deadalnix.me",                    // Amaury SÉCHET
            "seeder.criptolayer.net"                // criptolayer.net
    )

    private val blockHeader = BlockHeader(
            version = 549453824,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("000000000000000002e4009667ba236d52f605cd44c12ebd79208c14b520968e"),
            merkleRoot = HashUtils.toBytesAsLE("65822e3caa2a4709abd37df7de6464a58830da0f8e308af44586114e1c73914f"),
            timestamp = 1551084121,
            bits = 403013590,
            nonce = 2244691553,
            hash = HashUtils.toBytesAsLE("00000000000000000356c9c3f92f22b88bd8d0544f40a09260235d6cf95d491b")
    )

    override val checkpointBlock = Block(blockHeader, 571268)
}

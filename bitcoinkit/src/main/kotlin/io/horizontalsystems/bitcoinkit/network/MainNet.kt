package io.horizontalsystems.bitcoinkit.network

import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.storage.BlockHeader
import io.horizontalsystems.bitcoinkit.utils.HashUtils

class MainNet : Network() {

    override var port: Int = 8333

    override var magic: Long = 0xd9b4bef9L
    override var bip32HeaderPub: Int = 0x0488B21E   // The 4 byte header that serializes in base58 to "xpub".
    override var bip32HeaderPriv: Int = 0x0488ADE4  // The 4 byte header that serializes in base58 to "xprv"
    override var addressVersion: Int = 0
    override var addressSegwitHrp: String = "bc"
    override var addressScriptVersion: Int = 5
    override var coinType: Int = 0

    override val maxBlockSize = 1_000_000

    override var dnsSeeds: Array<String> = arrayOf(
            "seed.bitcoin.sipa.be",             // Pieter Wuille
            "dnsseed.bluematt.me",              // Matt Corallo
            "dnsseed.bitcoin.dashjr.org",       // Luke Dashjr
            "seed.bitcoinstats.com",            // Chris Decker
            "seed.bitcoin.jonasschnelli.ch",    // Jonas Schnelli
            "seed.btc.petertodd.org",           // Peter Todd
            "seed.bitcoin.sprovoost.nl"         // Sjors Provoost
    )

    private val blockHeader = BlockHeader(
            version = 536870912,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("00000000000000000017e5c36734296b27065045f181e028c0d91cebb336d50c"),
            merkleRoot = HashUtils.toBytesAsLE("2f9963d6eb332a0dd03ad806f504981e6180226dbca4385dc801db8974b2c17b"),
            timestamp = 1551026038,
            bits = 388914000,
            nonce = 1427093839,
            hash = HashUtils.toBytesAsLE("0000000000000000002567dc317da20ddb0d7ef922fe1f9c2375671654f9006c")
    )

    override val checkpointBlock = Block(blockHeader, 564480)
}

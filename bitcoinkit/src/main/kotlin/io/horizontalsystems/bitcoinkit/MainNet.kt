package io.horizontalsystems.bitcoinkit

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils

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

    override var dnsSeeds = listOf(
            "seed.bitcoin.sipa.be",             // Pieter Wuille
            "dnsseed.bluematt.me",              // Matt Corallo
            "dnsseed.bitcoin.dashjr.org",       // Luke Dashjr
            "seed.bitcoinstats.com",            // Chris Decker
            "seed.bitcoin.jonasschnelli.ch",    // Jonas Schnelli
            "seed.btc.petertodd.org",           // Peter Todd
            "seed.bitcoin.sprovoost.nl"         // Sjors Provoost
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
            version = 0x20000000,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("0000000000000000000485ab94f5ea60203aacfc9740b3e42700d7e7012f76d7"),
            merkleRoot = HashUtils.toBytesAsLE("2e76c50d3dcecc46264b7ff8e653d5c9f06680f4d88f5b239d58a531a3c12279"),
            timestamp = 1559256184,
            bits = 0x1725bb76,
            nonce = 0x423310ae,
            hash = HashUtils.toBytesAsLE("00000000000000000001791f463d849ce5363d751c91f7d3cd2ff18981ae221d")
    ), 578592)
}

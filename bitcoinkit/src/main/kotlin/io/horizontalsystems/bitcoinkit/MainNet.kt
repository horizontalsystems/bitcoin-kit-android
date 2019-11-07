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
    override val dustRelayTxFee = 3000 // https://github.com/bitcoin/bitcoin/blob/c536dfbcb00fb15963bf5d507b7017c241718bf6/src/policy/policy.h#L50

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
            version = 536879104,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("0000000000000000000676463abf3771ea01e0f8c948d1c93658a1d82d95df5a"),
            merkleRoot = HashUtils.toBytesAsLE("24415cca3979b6f1b6a19c63160bf732030b1031b5615d98efe8c2cee083175e"),
            timestamp = 1571866973,
            bits = 387223263,
            nonce = 1093878582,
            hash = HashUtils.toBytesAsLE("0000000000000000000983a15a2735b4f37861c4019d97200820cd190f28dba5")
    ), 600768)
}

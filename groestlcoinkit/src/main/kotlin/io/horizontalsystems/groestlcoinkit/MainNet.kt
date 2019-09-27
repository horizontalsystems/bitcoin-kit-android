package io.horizontalsystems.groestlcoinkit

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils

class MainNet : Network() {

    override var port: Int = 1331

    override var magic: Long = 0xd4b4bef9L
    override var bip32HeaderPub: Int = 0x0488B21E   // The 4 byte header that serializes in base58 to "xpub".
    override var bip32HeaderPriv: Int = 0x0488ADE4  // The 4 byte header that serializes in base58 to "xprv"
    override var addressVersion: Int = 36
    override var addressSegwitHrp: String = "grs"
    override var addressScriptVersion: Int = 5
    override var coinType: Int = 17

    override val maxBlockSize = 1_000_000

    override var dnsSeeds = listOf(
            "dnsseed1.groestlcoin.org",
            "dnsseed2.groestlcoin.org",
            "dnsseed3.groestlcoin.org",
            "dnsseed4.groestlcoin.org"
    )

    override val bip44CheckpointBlock = Block(BlockHeader(
            version = 112,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("00000ac5927c594d49cc0bdb81759d0da8297eb614683d3acb62f0703b639023"),
            merkleRoot = HashUtils.toBytesAsLE("cf72b5842b3528fd7f3065ba9e93c50a62e84f42b3b7b7a351d910b5e353b662"),
            timestamp = 1395495671,
            bits = 0x1e0fffff,
            nonce = 2623799296,
            hash = HashUtils.toBytesAsLE("00000973d52019a3fdda1b1f346e1d76cbf12f8fdd9fbf0ade33bc1da89cb2e9")
    ), 1)

    override val lastCheckpointBlock = Block(BlockHeader(
            version = 536870912,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("0000000000001c933b03ef592afb0069e5989cd55ad58ec43e7c95576832b358"),
            merkleRoot = HashUtils.toBytesAsLE("294ba7824542147ab1f228cb4a1102feabc3ad9bbacef28e2fa929f923698fa8"),
            timestamp = 1567468782,
            bits = 0x1a2836de,
            nonce = 529959857,
            hash = HashUtils.toBytesAsLE("000000000000032ad22aabbaf6350d5a50174b905e61e3dfec8bba41d5f755cc")
    ), 2738651)
}

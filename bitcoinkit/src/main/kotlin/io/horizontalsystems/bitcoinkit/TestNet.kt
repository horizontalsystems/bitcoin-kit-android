package io.horizontalsystems.bitcoinkit

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils

class TestNet : Network() {

    override var port: Int = 18333

    override var magic: Long = 0x0709110B
    override var bip32HeaderPub: Int = 0x043587CF
    override var bip32HeaderPriv: Int = 0x04358394
    override var addressVersion: Int = 111
    override var addressSegwitHrp: String = "tb"
    override var addressScriptVersion: Int = 196
    override var coinType: Int = 1

    override val maxBlockSize = 1_000_000

    override var dnsSeeds = listOf(
            "testnet-seed.bitcoin.petertodd.org",    // Peter Todd
            "testnet-seed.bitcoin.jonasschnelli.ch", // Jonas Schnelli
            "testnet-seed.bluematt.me",              // Matt Corallo
            "testnet-seed.bitcoin.schildbach.de",    // Andreas Schildbach
            "bitcoin-testnet.bloqseeds.net"          // Bloq
    )

    override val bip44CheckpointBlock = Block(BlockHeader(
            version = 2,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("0000000003dc49f7472f960eedb4fb2d1ccc8b0530ca6c75ed2bba9718b6f297"),
            merkleRoot = HashUtils.toBytesAsLE("a60fdbc889976c573450e9f78f1c330e374968a54f294e427180da1e9a07806b"),
            timestamp = 1393645018,
            bits = 0x1c0180ab,
            nonce = 634051227,
            hash = HashUtils.toBytesAsLE("000000000000bbde3a83bd29bc5cacd73f039f345318e7a4088914342c9d259a")
    ), 199584)

    override val lastCheckpointBlock = Block(BlockHeader(
            version = 0x20400000,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("000000000001386a3904f0ba4f25dc7ace09b67a6fe8977e7aecc55813fa9ac5"),
            merkleRoot = HashUtils.toBytesAsLE("c75984136598aaec589cbe0f61b1caf6c1eb69614c412861b474aedce5d71b7a"),
            timestamp = 1560904860,
            bits = 0x1b008690,
            nonce = 0xcf731739,
            hash = HashUtils.toBytesAsLE("000000000000608edb398b7969795bc0681c1fa280234f44cb2c506f853ae41e")
    ), 1560384)

}

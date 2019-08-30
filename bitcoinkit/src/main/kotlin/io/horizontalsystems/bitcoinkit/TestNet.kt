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
            version = 1073676288,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("00000000000000525de83fba2439549ef0ed78d6d08516a0513abb972b0fca95"),
            merkleRoot = HashUtils.toBytesAsLE("1c2f9a7885b2334096fbc1cd1373a1ac6d42b3eacee91d32758bb6f9f5018670"),
            timestamp = 1565789713,
            bits = 436336433,
            nonce = 1552477045,
            hash = HashUtils.toBytesAsLE("00000000000001d6d3fcee88c80ac371d82b370f0158cc2737bebad6d13ff47b")
    ), 1574496)

}

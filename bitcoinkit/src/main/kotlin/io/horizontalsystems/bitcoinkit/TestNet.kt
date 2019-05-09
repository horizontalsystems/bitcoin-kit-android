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

    override var dnsSeeds: Array<String> = arrayOf(
            "testnet-seed.bitcoin.petertodd.org",    // Peter Todd
            "testnet-seed.bitcoin.jonasschnelli.ch", // Jonas Schnelli
            "testnet-seed.bluematt.me",              // Matt Corallo
            "testnet-seed.bitcoin.schildbach.de",    // Andreas Schildbach
            "bitcoin-testnet.bloqseeds.net"          // Bloq
    )

    private val blockHeader = BlockHeader(
            version = 1073676288,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("00000000000002845d416fbfa05a5d40ba5ba5418a64f06443042a53cf1fd608"),
            merkleRoot = HashUtils.toBytesAsLE("5cf68623e65eed4af3d669fd3680bbc5f7781a9ff9f8bd8d44e40ad06416fba4"),
            timestamp = 1556877853,
            bits = 436373240,
            nonce = 388744679,
            hash = HashUtils.toBytesAsLE("000000000000013d3dd95fb84b56616dd29409dc9750e200b2c19f435e561d5e")
    )

    override val checkpointBlock = Block(blockHeader, 1514016)
}

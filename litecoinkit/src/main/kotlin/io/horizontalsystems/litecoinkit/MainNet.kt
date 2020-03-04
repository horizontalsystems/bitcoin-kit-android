package io.horizontalsystems.litecoinkit

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils

class MainNet : Network() {
    override val protocolVersion: Int = 70015
    override var port: Int = 9333

    override var magic: Long = 0xdbb6c0fb
    override var bip32HeaderPub: Int = 0x0488B21E   // The 4 byte header that serializes in base58 to "xpub".
    override var bip32HeaderPriv: Int = 0x0488ADE4  // The 4 byte header that serializes in base58 to "xprv"
    override var addressVersion: Int = 0
    override var addressSegwitHrp: String = "ltc"
    override var addressScriptVersion: Int = 0x32
    override var coinType: Int = 2

    override val maxBlockSize = 1_000_000
    override val dustRelayTxFee = 3000 // https://github.com/bitcoin/bitcoin/blob/c536dfbcb00fb15963bf5d507b7017c241718bf6/src/policy/policy.h#L50

    override val syncableFromApi = false

    override var dnsSeeds = listOf(
            "dnsseed.litecoinpool.org",
            "seed-a.litecoin.loshan.co.uk",
            "dnsseed.thrasher.io",
            "dnsseed.koin-project.com",
            "dnsseed.litecointools.com"
    )


    override val bip44CheckpointBlock = Block(BlockHeader(
            version = 2,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("636a10f30811889ed382cefd3643903e0cb432b3c52e47f328a8f19bc89b2f98"),
            merkleRoot = HashUtils.toBytesAsLE("9986047ecd48b4c149b22500f059640d07ed3b27af112be4acf38e376343ec9f"),
            timestamp = 1398284155,
            bits = 0x1b0c7a03,
            nonce = 3717250361,
            hash = HashUtils.toBytesAsLE("256a05e5154fad3e86b1bd0066ba7314a28ed83ff7b7bbb6dec6a5e3262a749f")
    ), 554400)

/*
    override val bip44CheckpointBlock = Block(BlockHeader(
            version = 2,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("a5bb20d85385a38069808aaa48021a28f767bc0fdc35390d32a548d00584c4a6"),
            merkleRoot = HashUtils.toBytesAsLE("29c5896f8e6a3c1c6e84db8804cbf471c427b0ad084a1679a3092ed7d2693198"),
            timestamp = 1583038687,
            bits = 0x1a029e9d,
            nonce = 51179438,
            hash = HashUtils.toBytesAsLE("3ea393edec1c0672a8e5514f7fd7f82ea0db792139144ec8a8941978321751fe")
    ), 1798272)
*/
}

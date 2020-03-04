package io.horizontalsystems.litecoinkit

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils

class TestNet : Network() {
    override val protocolVersion: Int = 70015
    override var port: Int = 19335

    override var magic: Long = 0xf1c8d2fd
    override var bip32HeaderPub: Int = 0x043587CF
    override var bip32HeaderPriv: Int = 0x04358394
    override var addressVersion: Int = 111
    override var addressSegwitHrp: String = "tltc"
    override var addressScriptVersion: Int = 0x32
    override var coinType: Int = 1

    override val maxBlockSize = 1_000_000
    override val dustRelayTxFee = 3000 // https://github.com/bitcoin/bitcoin/blob/c536dfbcb00fb15963bf5d507b7017c241718bf6/src/policy/policy.h#L50

    override val syncableFromApi = false

    override var dnsSeeds = listOf(
            "testnet-seed.ltc.xurious.com",
            "seed-b.litecoin.loshan.co.uk",
            "dnsseed-testnet.thrasher.io"
    )

    override val bip44CheckpointBlock = Block(BlockHeader(
            version = 2,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("4966625a4b2851d9fdee139e56211a0d88575f59ed816ff5e6a63deb4e3e29a0"),
            merkleRoot = HashUtils.toBytesAsLE("f7a718f20ea4529351892e70a563f1c58af5e720798e475cc677302ebef92513"),
            timestamp = 1486961886,
            bits = 0x1e0fffff,
            nonce = 0xf68b0300,
            hash = HashUtils.toBytesAsLE("dc19bf491bf601e2a05fd37372f6dc1a51feba5f0f35cf944d39334e79790f5b")
    ), 1)

/*
    override val bip44CheckpointBlock = Block(BlockHeader(
            version = 2,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("a1e894c32a86e1cb161f27053c77b6471113fb5a57a707a75c9a60e6c894c973"),
            merkleRoot = HashUtils.toBytesAsLE("1d45cf33e04dfbe33cff8f55c914bf167c658b89e1c9a1e6567303da87a056a7"),
            timestamp = 1582983826,
            bits = 0x1d0ffff0,
            nonce = 3728684288,
            hash = HashUtils.toBytesAsLE("1bd8874feb033b27adfd40677c0dd3afbf80415b049d7c198e72921623e2dd45")
    ), 1393056)
*/
}

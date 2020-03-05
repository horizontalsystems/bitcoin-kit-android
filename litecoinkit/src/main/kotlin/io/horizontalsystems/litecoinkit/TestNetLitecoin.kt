package io.horizontalsystems.litecoinkit

import io.horizontalsystems.bitcoincore.network.Network

class TestNetLitecoin : Network() {
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
}

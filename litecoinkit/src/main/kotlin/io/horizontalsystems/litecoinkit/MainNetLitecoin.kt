package io.horizontalsystems.litecoinkit

import io.horizontalsystems.bitcoincore.network.Network

class MainNetLitecoin : Network() {
    override val protocolVersion: Int = 70015
    override var port: Int = 9333

    override var magic: Long = 0xdbb6c0fb
    override var bip32HeaderPub: Int = 0x0488B21E   // The 4 byte header that serializes in base58 to "xpub".
    override var bip32HeaderPriv: Int = 0x0488ADE4  // The 4 byte header that serializes in base58 to "xprv"
    override var addressVersion: Int = 0x30
    override var addressSegwitHrp: String = "ltc"
    override var addressScriptVersion: Int = 0x32
    override var coinType: Int = 2

    override val maxBlockSize = 1_000_000
    override val dustRelayTxFee = 3000 // https://github.com/bitcoin/bitcoin/blob/c536dfbcb00fb15963bf5d507b7017c241718bf6/src/policy/policy.h#L50

    override val syncableFromApi = true

    override var dnsSeeds = listOf(
            "x5.dnsseed.thrasher.io",
            "x5.dnsseed.litecointools.com",
            "x5.dnsseed.litecoinpool.org",
            "seed-a.litecoin.loshan.co.uk"
    )
}

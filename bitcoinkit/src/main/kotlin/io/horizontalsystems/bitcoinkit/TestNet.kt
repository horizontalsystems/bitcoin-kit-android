package io.horizontalsystems.bitcoinkit

import io.horizontalsystems.bitcoincore.network.Network
/**
 * Extends from the abstract Network class and overrides all variables. Configures connection to the TestNet.
 */
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
    override val dustRelayTxFee = 3000 // https://github.com/bitcoin/bitcoin/blob/c536dfbcb00fb15963bf5d507b7017c241718bf6/src/policy/policy.h#L50

    override var dnsSeeds = listOf(
        "x5.testnet-seed.bitcoin.jonasschnelli.ch",
        "x5.seed.tbtc.petertodd.org",
        "x5.seed.testnet.bitcoin.sprovoost.nl",
        "testnet-seed.bluematt.me"
    )
}

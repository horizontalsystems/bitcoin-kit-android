package io.horizontalsystems.bitcoinkit

import io.horizontalsystems.bitcoincore.network.Network

/**
 *   Extends from the abstract Network class and overrides all variables. Configures connection to the MainNet.
 */
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
        "seed.bitcoinstats.com",            // Chris Decker
        "seed.btc.petertodd.org",           // Peter Todd
        "seed.bitcoin.sprovoost.nl",        // Sjors Provoost
        "seed.bitnodes.io",                 // Addy Yeow
        "dnsseed.emzy.de",                  // Stephan Oeste
        "seed.bitcoin.wiz.biz"              // Jason Maurice
    )
}

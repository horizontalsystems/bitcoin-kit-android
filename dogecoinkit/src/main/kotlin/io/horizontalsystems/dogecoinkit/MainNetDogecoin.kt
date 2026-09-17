package io.horizontalsystems.dogecoinkit

import io.horizontalsystems.bitcoincore.network.Network

class MainNetDogecoin : Network() {
    override val protocolVersion: Int = 70015
    override var port: Int = 22556

    override var magic: Long = 0xc0c0c0c0L
    override var bip32HeaderPub: Int = 0x02facafd   // The 4 byte header that serializes in base58 to "dgub".
    override var bip32HeaderPriv: Int = 0x02fac398  // The 4 byte header that serializes in base58 to "dgpv"
    override var addressVersion: Int = 0x1e         // 30, addresses start with "D"
    override var addressSegwitHrp: String = ""      // Dogecoin has no segwit, this is never used
    override var addressScriptVersion: Int = 0x16   // 22
    override var coinType: Int = 3
    override val blockchairChainId: String = "dogecoin"

    override val maxBlockSize = 1_000_000

    // Dogecoin has two dust limits: outputs below 0.001 DOGE are invalid outright, and outputs
    // below 0.01 DOGE are only relayed if an extra 0.01 DOGE fee is attached per such output.
    // DustCalculator can express only a single threshold (dust = size * dustRelayTxFee / 1000),
    // so this targets the higher, soft limit: a 182 byte P2PKH output+input costs
    // 182 * 5494 = 999_908 koinu, just under 0.01 DOGE. That keeps us from ever building an
    // output that relays would reject as underpaid.
    // https://github.com/dogecoin/dogecoin/blob/master/doc/fee-recommendation.md
    override val dustRelayTxFee = 5_494_505

    override val syncableFromApi = true

    // seed.doger.dogecoin.com, listed by Dogecoin Core, no longer resolves (checked 2026-09-17).
    override var dnsSeeds = listOf(
        "seed.multidoge.org",
        "seed2.multidoge.org",
    )
}

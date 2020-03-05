package io.horizontalsystems.bitcoincash

import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.transactions.scripts.Sighash
import kotlin.experimental.or

class TestNetBitcoinCash : Network() {

    override var port: Int = 18333

    override var magic: Long = 0xf4f3e5f4
    override var bip32HeaderPub: Int = 0x043587cf
    override var bip32HeaderPriv: Int = 0x04358394
    override var addressVersion: Int = 111
    override var addressSegwitHrp: String = "bchtest"
    override var addressScriptVersion: Int = 196
    override var coinType: Int = 1

    override val maxBlockSize = 32 * 1024 * 1024
    override val dustRelayTxFee = 1000 // https://github.com/Bitcoin-ABC/bitcoin-abc/blob/master/src/policy/policy.h#L78
    override val sigHashForked = true
    override val sigHashValue = Sighash.FORKID or Sighash.ALL

    override var dnsSeeds = listOf(
            "testnet-seed.bitcoinabc.org",          // Bitcoin ABC seeder
            "testnet-seed-abc.bitcoinforks.org",    // bitcoinforks seeders
            "testnet-seed.bitprim.org",             // Bitprim
            "testnet-seed.deadalnix.me",            // Amaury SÉCHET
            "testnet-seeder.criptolayer.net"        // criptolayer.net
    )
}

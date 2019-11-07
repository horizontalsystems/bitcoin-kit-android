package io.horizontalsystems.bitcoincash

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.transactions.scripts.Sighash
import io.horizontalsystems.bitcoincore.utils.HashUtils
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
            version = 536870912,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("00000000000942fa7c29649a86241e4311a84536861c42cf617fda2682fe855e"),
            merkleRoot = HashUtils.toBytesAsLE("bb8410c2a763ea3aa4cb6d2679b8a6a2887afe377f5ba28f1af5a3d52bba7e83"),
            timestamp = 1573097953,
            bits = 486604799,
            nonce = 3758956418,
            hash = HashUtils.toBytesAsLE("0000000017dbffd594e34e02b1033a37da40056a0cf8f4fd3bb8f373336ebc4e")
    ), 1339023)

}

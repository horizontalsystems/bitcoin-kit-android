package io.horizontalsystems.groestlcoinkit

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils

class TestNet : Network() {

    override var port: Int = 17777

    override var magic: Long = 0x0709110B
    override var bip32HeaderPub: Int = 0x043587CF
    override var bip32HeaderPriv: Int = 0x04358394
    override var addressVersion: Int = 111
    override var addressSegwitHrp: String = "tgrs"
    override var addressScriptVersion: Int = 196
    override var coinType: Int = 1

    override val maxBlockSize = 1_000_000

    override var dnsSeeds = listOf(
            "testnet-seed1.groestlcoin.org",
            "testnet-seed2.groestlcoin.org"
    )

    override val bip44CheckpointBlock = Block(BlockHeader(
            version = 3,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("000000ffbb50fc9898cdd36ec163e6ba23230164c0052a28876255b7dcf2cd36"),
            merkleRoot = HashUtils.toBytesAsLE("fb6cde21ddf87d46a9816f9a107a05b375ff65d48ba3a6da48d5d48af62ec177"),
            timestamp = 1440017729,
            bits = 0x1e00ffff,
            nonce = 17166854,
            hash = HashUtils.toBytesAsLE("000000458242a5d60e943f0a9945c29040b32be35582d1bfd47b5c536f10ac30")
    ), 1)

    override val lastCheckpointBlock = Block(BlockHeader(
            version = 536870912,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("000000bd9be35095518f99714b0a67390d126b250a16bfe726d4083cd9661003"),
            merkleRoot = HashUtils.toBytesAsLE("a4b95606216688df433669202442c56d207ab679ffb5f8567161390214e7ced6"),
            timestamp = 1567468793,
            bits = 0x1e00ffff,
            nonce = 2230299648,
            hash = HashUtils.toBytesAsLE("000000a4466ac156b2ca13663d1f8fae30cb20ed8c9d1e5af85ef1fd8fc208c4")
    ), 1294877)

}

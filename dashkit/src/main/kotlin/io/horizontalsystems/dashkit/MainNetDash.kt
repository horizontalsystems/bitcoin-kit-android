package io.horizontalsystems.dashkit

import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.bitcoincore.utils.HashUtils

class MainNetDash : Network() {

    override val protocolVersion = 70214

    override var port: Int = 9999

    override var magic: Long = 0xbd6b0cbf
    override var bip32HeaderPub: Int = 0x0488B21E   // The 4 byte header that serializes in base58 to "xpub".
    override var bip32HeaderPriv: Int = 0x0488ADE4  // The 4 byte header that serializes in base58 to "xprv"
    override var addressVersion: Int = 76
    override var addressSegwitHrp: String = "bc"
    override var addressScriptVersion: Int = 16
    override var coinType: Int = 5

    override val maxBlockSize = 1_000_000
    override val dustRelayTxFee = 1000 // https://github.com/dashpay/dash/blob/master/src/policy/policy.h#L36

    override var dnsSeeds = listOf(
            "dnsseed.dash.org",
            "dnsseed.dashdot.io",
            "dnsseed.masternode.io"
    )

    override val bip44CheckpointBlock = Block(BlockHeader(
            version = 1,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("0000000000000000000000000000000000000000000000000000000000000000"),
            merkleRoot = HashUtils.toBytesAsLE("4a5e1e4baab89f3a32518a88c31bc87f618f76673e2cc77ab2127b7afdeda33b"),
            timestamp = 1231006505,
            bits = 486604799,
            nonce = 2083236893,
            hash = HashUtils.toBytesAsLE("00000ffd590b1485b3caadc19b22e6379c733355108f107a430458cdf3407ab6")
    ), 0)

    override val lastCheckpointBlock = Block(BlockHeader(
            version = 536870912,
            previousBlockHeaderHash = HashUtils.toBytesAsLE("0000000000000010751b0e9a8deb7d6589a339a3ffcb756d2d10f0cf203f5a1c"),
            merkleRoot = HashUtils.toBytesAsLE("ed933d1c7e48da67e96b2822edbb4c76fe8fe19b71241fc8bb51c6035fbf91d5"),
            timestamp = 1573116504,
            bits = 420940927,
            nonce = 1081860501,
            hash = HashUtils.toBytesAsLE("000000000000000087895fde87f60ba1eebf761a962f1a74ded6d5499b0b6660")
    ), 1166976)
}

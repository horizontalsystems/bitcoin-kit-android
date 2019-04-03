package io.horizontalsystems.bitcoinkit.network.messages

import io.horizontalsystems.bitcoinkit.network.Network

class BitcoinMessageParser : IMessageParser {
    override fun parseMessage(command: String, payload: ByteArray, network: Network): Message? {
        return when (command) {
            "merkleblock" -> MerkleBlockMessage(payload)
            "addr" -> AddrMessage(payload)
            "getaddr" -> GetAddrMessage(payload)
            "getblocks" -> GetBlocksMessage(payload)
            "getdata" -> GetDataMessage(payload)
            "getheaders" -> GetHeadersMessage(payload)
            "inv" -> InvMessage(payload)
            "ping" -> PingMessage(payload)
            "pong" -> PongMessage(payload)
            "verack" -> VerAckMessage(payload)
            "version" -> VersionMessage(payload)
            "tx" -> TransactionMessage(payload)
            else -> null
        }
    }
}

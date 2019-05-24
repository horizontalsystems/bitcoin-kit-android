package io.horizontalsystems.dashkit.messages

import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.messages.IMessageParser
import io.horizontalsystems.dashkit.models.CoinbaseTransaction
import io.horizontalsystems.dashkit.models.Masternode
import java.io.ByteArrayInputStream

class MasternodeListDiffMessage(
        val baseBlockHash: ByteArray,
        val blockHash: ByteArray,
        val totalTransactions: Long,
        val merkleHashes: List<ByteArray>,
        val merkleFlags: ByteArray,
        val cbTx: CoinbaseTransaction,
        val deletedMNs: List<ByteArray>,
        val mnList: List<Masternode>) : IMessage {

    override fun toString(): String {
        return "MnListDiffMessage(baseBlockHash=${baseBlockHash.toReversedHex()}, blockHash=${blockHash.toReversedHex()})"
    }

}

class MasternodeListDiffMessageParser : IMessageParser {
    override val command: String = "mnlistdiff"

    override fun parseMessage(payload: ByteArray): IMessage {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val baseBlockHash = input.readBytes(32)
            val blockHash = input.readBytes(32)
            val totalTransactions = input.readUnsignedInt()
            val merkleHashesCount = input.readVarInt()
            val merkleHashes = mutableListOf<ByteArray>()
            repeat(merkleHashesCount.toInt()) {
                merkleHashes.add(input.readBytes(32))
            }
            val merkleFlagsCount = input.readVarInt()
            val merkleFlags = input.readBytes(merkleFlagsCount.toInt())
            val cbTx = CoinbaseTransaction(input)
            val deletedMNsCount = input.readVarInt()
            val deletedMNs = mutableListOf<ByteArray>()
            repeat(deletedMNsCount.toInt()) {
                deletedMNs.add(input.readBytes(32))
            }
            val mnListCount = input.readVarInt()
            val mnList = mutableListOf<Masternode>()
            repeat(mnListCount.toInt()) {
                mnList.add(Masternode(input))
            }

            return MasternodeListDiffMessage(baseBlockHash, blockHash, totalTransactions, merkleHashes, merkleFlags, cbTx, deletedMNs, mnList)
        }
    }
}

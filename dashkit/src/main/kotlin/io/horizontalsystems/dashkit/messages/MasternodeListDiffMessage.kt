package io.horizontalsystems.dashkit.messages

import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.messages.IMessageParser
import io.horizontalsystems.dashkit.models.CoinbaseTransaction
import io.horizontalsystems.dashkit.models.Masternode
import io.horizontalsystems.dashkit.models.Quorum

class MasternodeListDiffMessage(
        val baseBlockHash: ByteArray,
        val blockHash: ByteArray,
        val totalTransactions: Long,
        val merkleHashes: List<ByteArray>,
        val merkleFlags: ByteArray,
        val cbTx: CoinbaseTransaction,
        val deletedMNs: List<ByteArray>,
        val mnList: List<Masternode>,
        val deletedQuorums: List<Pair<Int, ByteArray>>,
        val quorumList: List<Quorum>
) : IMessage {

    override fun toString(): String {
        return "MnListDiffMessage(baseBlockHash=${baseBlockHash.toReversedHex()}, blockHash=${blockHash.toReversedHex()})"
    }

}

class MasternodeListDiffMessageParser : IMessageParser {
    override val command: String = "mnlistdiff"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
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

        val deletedQuorumsCount = input.readVarInt()
        val deletedQuorums = mutableListOf<Pair<Int, ByteArray>>()
        repeat(deletedQuorumsCount.toInt()) {
            deletedQuorums.add(Pair(input.read(), input.readBytes(32)))
        }
        val newQuorumsCount = input.readVarInt()
        val quorumList = mutableListOf<Quorum>()
        repeat(newQuorumsCount.toInt()) {
            quorumList.add(Quorum(input))
        }

        return MasternodeListDiffMessage(baseBlockHash, blockHash, totalTransactions, merkleHashes, merkleFlags, cbTx, deletedMNs, mnList, deletedQuorums, quorumList)
    }
}

package io.horizontalsystems.bitcoinkit.dash.messages

import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.bitcoinkit.dash.models.CoinbaseTransaction
import io.horizontalsystems.bitcoinkit.dash.models.Masternode
import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.network.messages.Message
import java.io.ByteArrayInputStream

class MasternodeListDiffMessage(payload: ByteArray) : Message("mnlistdiff") {

    var baseBlockHash = byteArrayOf()
    var blockHash = byteArrayOf()
    var totalTransactions = 0L
    var merkleHashesCount = 0L
    var merkleHashes = mutableListOf<ByteArray>()
    var merkleFlagsCount = 0L
    var merkleFlags = byteArrayOf()
    val cbTx: CoinbaseTransaction
    var deletedMNsCount = 0L
    var deletedMNs = mutableListOf<ByteArray>()
    var mnListCount = 0L
    var mnList = mutableListOf<Masternode>()

    init {
        val input = BitcoinInput(ByteArrayInputStream(payload))

        baseBlockHash = input.readBytes(32)
        blockHash = input.readBytes(32)
        totalTransactions = input.readUnsignedInt()
        merkleHashesCount = input.readVarInt()
        repeat(merkleHashesCount.toInt()) {
            merkleHashes.add(input.readBytes(32))
        }
        merkleFlagsCount = input.readVarInt()
        merkleFlags = input.readBytes(merkleFlagsCount.toInt())
        cbTx = CoinbaseTransaction(input)
        deletedMNsCount = input.readVarInt()
        repeat(deletedMNsCount.toInt()) {
            deletedMNs.add(input.readBytes(32))
        }
        mnListCount = input.readVarInt()
        repeat(mnListCount.toInt()) {
            mnList.add(Masternode(input))
        }

        input.close()
    }

    override fun getPayload(): ByteArray {
        TODO("not implemented")
    }

    override fun toString(): String {
        return "MnListDiffMessage(baseBlockHash=${baseBlockHash.toHexString()}, blockHash=${blockHash.toHexString()})"
    }
}
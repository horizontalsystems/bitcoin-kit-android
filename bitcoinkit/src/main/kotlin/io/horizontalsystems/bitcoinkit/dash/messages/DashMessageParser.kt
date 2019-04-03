package io.horizontalsystems.bitcoinkit.dash.messages

import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.bitcoinkit.network.messages.IMessageParser
import io.horizontalsystems.bitcoinkit.network.messages.Message

class DashMessageParser : IMessageParser {
    override fun parseMessage(command: String, payload: ByteArray, network: Network): Message? {
        return when (command) {
            "ix" -> TransactionLockMessage(payload)
            "txlvote" -> TransactionLockVoteMessage(payload)
            "mnlistdiff" -> MasternodeListDiffMessage(payload)
            else -> null
        }
    }
}

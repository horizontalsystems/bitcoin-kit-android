package io.horizontalsystems.bitcoinkit.network.messages

import io.horizontalsystems.bitcoinkit.network.Network

interface IMessageParser {
    fun parseMessage(command: String, payload: ByteArray, network: Network): Message?
}

class MessageParserChain : IMessageParser {

    private val concreteParsers = mutableListOf<IMessageParser>()

    override fun parseMessage(command: String, payload: ByteArray, network: Network): Message? {
        for (parser in concreteParsers) {
            val message = parser.parseMessage(command, payload, network)
            if (message != null)
                return message
        }

        return null
    }

    fun addParser(parser: IMessageParser) {
        concreteParsers.add(parser)
    }
}

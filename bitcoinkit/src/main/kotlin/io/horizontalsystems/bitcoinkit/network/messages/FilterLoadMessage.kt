package io.horizontalsystems.bitcoinkit.network.messages

import io.horizontalsystems.bitcoinkit.crypto.BloomFilter

class FilterLoadMessage(bloomFilter: BloomFilter) : IMessage {
    override val command: String = "filterload"

    var filter: BloomFilter = bloomFilter

    override fun toString(): String {
        return "FilterLoadMessage($filter)"
    }
}

class FilterLoadMessageSerializer : IMessageSerializer {
    override val command: String = "filterload"

    override fun serialize(message: IMessage): ByteArray {
        if (message !is FilterLoadMessage) throw WrongSerializer()

        return message.filter.toByteArray()
    }
}

package io.horizontalsystems.bitcoinkit.network.messages

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.models.InventoryItem
import java.io.ByteArrayInputStream

class GetDataMessage(var inventory: List<InventoryItem>) : IMessage {
    override val command: String = "getdata"

    override fun toString(): String {
        return "GetDataMessage(count=${inventory.size})"
    }
}

class GetDataMessageParser : IMessageParser {
    override val command: String = "getdata"

    override fun parseMessage(payload: ByteArray): IMessage {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val count = input.readVarInt() // do not store count
            val inventory = List(count.toInt()) {
                InventoryItem(input)
            }
            return GetDataMessage(inventory)
        }
    }
}

class GetDataMessageSerializer : IMessageSerializer {
    override val command: String = "getdata"

    override fun serialize(message: IMessage): ByteArray {
        if (message !is GetDataMessage) throw WrongSerializer()

        val output = BitcoinOutput().writeVarInt(message.inventory.size.toLong())
        message.inventory.forEach {
            output.write(it.toByteArray())
        }

        return output.toByteArray()
    }
}
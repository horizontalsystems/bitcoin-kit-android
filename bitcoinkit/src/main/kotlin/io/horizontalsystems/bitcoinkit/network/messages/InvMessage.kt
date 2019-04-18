package io.horizontalsystems.bitcoinkit.network.messages

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.models.InventoryItem
import io.horizontalsystems.bitcoinkit.utils.HashUtils
import java.io.ByteArrayInputStream

class InvMessage : IMessage {
    override val command: String = "inv"

    var inventory: List<InventoryItem>

    constructor(type: Int, hash: ByteArray) {
        val inv = InventoryItem()
        inv.type = type
        inv.hash = hash
        inventory = listOf(inv)
    }

    constructor(inventory: List<InventoryItem>) {
        this.inventory = inventory
    }

    override fun toString(): String {
        val invList = inventory.take(10)
                .map { inv -> inv.type.toString() + ":" + HashUtils.toHexStringAsLE(inv.hash) }
                .toTypedArray()
                .joinToString()

        return ("InvMessage(" + inventory.size + ": [" + invList + "])")
    }
}

class InvMessageParser : IMessageParser {
    override val command: String = "inv"

    override fun parseMessage(payload: ByteArray): IMessage {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val count = input.readVarInt() // do not store count
            val inventory = List(count.toInt()) {
                InventoryItem(input)
            }

            return InvMessage(inventory)
        }
    }
}

class InvMessageSerializer : IMessageSerializer {
    override val command: String = "inv"

    override fun serialize(message: IMessage): ByteArray {
        if (message !is InvMessage) throw WrongSerializer()

        val output = BitcoinOutput()
        output.writeVarInt(message.inventory.size.toLong())
        message.inventory.forEach {
            output.write(it.toByteArray())
        }

        return output.toByteArray()
    }
}

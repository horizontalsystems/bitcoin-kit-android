package io.horizontalsystems.bitcoincore.network.messages

import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.models.InventoryItem

class InvMessage : IMessage {
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
                .map { inv -> inv.type.toString() + ":" + inv.hash.toReversedHex() }
                .toTypedArray()
                .joinToString()

        return ("InvMessage(" + inventory.size + ": [" + invList + "])")
    }
}

class InvMessageParser : IMessageParser {
    override val command: String = "inv"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        val count = input.readVarInt() // do not store count
        val inventory = List(count.toInt()) {
            InventoryItem(input)
        }

        return InvMessage(inventory)
    }
}

class InvMessageSerializer : IMessageSerializer {
    override val command: String = "inv"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is InvMessage) {
            return null
        }

        val output = BitcoinOutput()
        output.writeVarInt(message.inventory.size.toLong())
        message.inventory.forEach {
            output.write(it.toByteArray())
        }

        return output.toByteArray()
    }
}

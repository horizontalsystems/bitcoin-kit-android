package io.horizontalsystems.bitcoinkit.network.messages

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.models.InventoryItem
import io.horizontalsystems.bitcoinkit.utils.HashUtils
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * Inventory Message
 *
 *  Size        Field           Description
 *  ====        =====           ===========
 *  VarInt      Count           Number of inventory items
 *  Variable    InventoryItem   One or more inventory items
 */
class InvMessage : Message {

    lateinit var inventory: List<InventoryItem>

    constructor() : super("inv") {
        inventory = listOf()
    }

    constructor(type: Int, hash: ByteArray) : super("inv") {
        val inv = InventoryItem()
        inv.type = type
        inv.hash = hash
        inventory = listOf(inv)
    }

    @Throws(IOException::class)
    constructor(payload: ByteArray) : super("inv") {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val count = input.readVarInt() // do not store count
            inventory = List(count.toInt()) {
                InventoryItem(input)
            }
        }
    }

    fun getBlockHashes(): Array<ByteArray> {
        return inventory
                .filter { iv -> iv.type == InventoryItem.MSG_BLOCK }
                .map { iv -> iv.hash }
                .toTypedArray()
    }

    fun getBlockHashesAsString(): Array<String> {
        return inventory
                .filter { iv -> iv.type == InventoryItem.MSG_BLOCK }
                .map { iv -> HashUtils.toHexStringAsLE(iv.hash) }
                .toTypedArray()
    }

    override fun getPayload(): ByteArray {
        val output = BitcoinOutput()
        output.writeVarInt(inventory.size.toLong())
        for (i in inventory.indices) {
            output.write(inventory[i].toByteArray())
        }
        return output.toByteArray()
    }

    override fun toString(): String {
        val invList = inventory.take(10)
                .map { inv -> inv.type.toString() + ":" + HashUtils.toHexStringAsLE(inv.hash) }
                .toTypedArray()
                .joinToString()

        return ("InvMessage(" + inventory.size + ": [" + invList + "])")
    }
}

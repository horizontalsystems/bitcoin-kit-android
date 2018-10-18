package io.horizontalsystems.bitcoinkit.messages

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.models.NetworkAddressTimestamp
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * Address Message
 *
 *   Size       Field       Description
 *   ====       =====       ===========
 *   VarInt     Count       The number of addresses
 *   Variable   Addresses   One or more network addresses
 */
class AddrMessage : Message {

    lateinit var addresses: Array<NetworkAddressTimestamp> // (uint32_t + net_addr)[]

    constructor() : super("addr") {
        addresses = arrayOf()
    }

    @Throws(IOException::class)
    constructor(payload: ByteArray) : super("addr") {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val count = input.readVarInt() // do not store count

            addresses = Array(count.toInt()) {
                NetworkAddressTimestamp(input)
            }
        }
    }

    override fun getPayload(): ByteArray {
        val output = BitcoinOutput()
        output.writeVarInt(addresses.size.toLong())
        for (i in addresses.indices) {
            val taddr = addresses[i]
            output.writeUnsignedInt(taddr.timestamp)
            output.write(taddr.address.toByteArray(false))
        }
        return output.toByteArray()
    }

    override fun toString(): String {
        return "AddrMessage(count=${addresses.size})"
    }

}

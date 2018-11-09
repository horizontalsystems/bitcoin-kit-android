package io.horizontalsystems.bitcoinkit.messages

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.models.NetworkAddress
import java.io.ByteArrayInputStream

/**
 * Address Message
 *
 *   Size       Field       Description
 *   ====       =====       ===========
 *   VarInt     Count       The number of addresses
 *   Variable   Addresses   One or more network addresses
 */
class AddrMessage constructor(payload: ByteArray) : Message("addr") {

    lateinit var addresses: Array<NetworkAddress> // (uint32_t + net_addr)[]

    init {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val count = input.readVarInt() // do not store count

            addresses = Array(count.toInt()) {
                NetworkAddress(input, false)
            }
        }
    }

    override fun getPayload(): ByteArray {
        val output = BitcoinOutput()
                .writeVarInt(addresses.size.toLong())

        for (i in addresses.indices) {
            val address = addresses[i]
            output.writeUnsignedInt(address.time)
            output.write(address.toByteArray(false))
        }

        return output.toByteArray()
    }

    override fun toString(): String {
        return "AddrMessage(count=${addresses.size})"
    }
}

package io.horizontalsystems.bitcoinkit.network.messages

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.models.NetworkAddress
import java.io.ByteArrayInputStream

class AddrMessage(var addresses: List<NetworkAddress>) : IMessage {
    override val command: String = "addr"

    override fun toString(): String {
        return "AddrMessage(count=${addresses.size})"
    }
}

class AddrMessageParser : IMessageParser {
    override val command = "addr"

    override fun parseMessage(payload: ByteArray): IMessage {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val count = input.readVarInt() // do not store count

            val addresses = List(count.toInt()) {
                NetworkAddress(input, false)
            }

            return AddrMessage(addresses)
        }
    }
}

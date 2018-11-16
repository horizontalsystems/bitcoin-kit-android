package io.horizontalsystems.bitcoinkit.network.messages

import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.models.Header
import io.horizontalsystems.bitcoinkit.utils.HashUtils
import java.io.ByteArrayInputStream

/**
 * Headers Message
 *
 *  Size        Field       Description
 *  ====        =====       ===========
 *  VarInt      Count       Number of headers
 *  Variable    Entries     Header entries
 */
class HeadersMessage() : Message("headers") {

    var headers = arrayOf<Header>()

    constructor(payload: ByteArray) : this() {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val count = input.readVarInt().toInt()

            headers = Array(count) {
                val header = Header(input)
                input.readVarInt() // tx count always zero
                header
            }
        }
    }

    override fun getPayload(): ByteArray {
        TODO("not implemented")
    }

    override fun toString(): String {
        return "HeadersMessage(${headers.size}:[${headers.joinToString { HashUtils.toHexStringAsLE(it.hash) }}])"
    }
}

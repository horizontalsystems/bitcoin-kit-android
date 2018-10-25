package io.horizontalsystems.bitcoinkit.messages

class MempoolMessage() : Message("mempool") {

    constructor(payload: ByteArray) : this()

    override fun getPayload(): ByteArray {
        return ByteArray(0)
    }

    override fun toString(): String {
        return "MempoolMessage()"
    }
}

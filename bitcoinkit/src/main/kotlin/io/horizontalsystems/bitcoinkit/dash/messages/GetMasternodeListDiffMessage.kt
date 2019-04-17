package io.horizontalsystems.bitcoinkit.dash.messages

import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.network.messages.IMessageSerializer
import io.horizontalsystems.bitcoinkit.network.messages.IMessage
import io.horizontalsystems.bitcoinkit.network.messages.WrongSerializer

class GetMasternodeListDiffMessage(val baseBlockHash: ByteArray, val blockHash: ByteArray) : IMessage {
    override val command: String = "getmnlistd"
}

class GetMasternodeListDiffMessageSerializer : IMessageSerializer {
    override val command: String = "getmnlistd"

    override fun serialize(message: IMessage): ByteArray {
        if (message !is GetMasternodeListDiffMessage) throw WrongSerializer()

        return BitcoinOutput()
                .write(message.baseBlockHash)
                .write(message.blockHash)
                .toByteArray()
    }
}

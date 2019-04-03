package io.horizontalsystems.bitcoinkit.dash.messages

import io.horizontalsystems.bitcoinkit.io.BitcoinOutput
import io.horizontalsystems.bitcoinkit.network.messages.Message

class GetMasternodeListDiffMessage : Message("getmnlistd") {

    var baseBlockHash = byteArrayOf()
    var blockHash = byteArrayOf()

    override fun getPayload(): ByteArray {
        return BitcoinOutput()
                .write(baseBlockHash)
                .write(blockHash)
                .toByteArray()
    }

}

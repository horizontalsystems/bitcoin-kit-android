package io.horizontalsystems.bitcoinkit.dash.models

import io.horizontalsystems.bitcoinkit.io.BitcoinOutput

class MasternodeSerializer {

    fun serialize(masternode: Masternode): ByteArray {
        val output = BitcoinOutput()
        output.write(masternode.proRegTxHash)
        output.write(masternode.confirmedHash)
        output.write(masternode.ipAddress)
        output.writeUnsignedShort(masternode.port)
        output.write(masternode.pubKeyOperator)
        output.write(masternode.keyIDVoting)
        output.writeByte(if (masternode.isValid) 0x01 else 0x00)

        return output.toByteArray()
    }

}

package io.horizontalsystems.bitcoincore.serializers

import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.models.TransactionOutput

object OutputSerializer {


    fun deserialize(input: BitcoinInputMarkable, vout: Long, txVersion: Int): TransactionOutput {
        val value = input.readLong()
        val scriptLength = input.readVarInt() // do not store
        val lockingScript = input.readBytes(scriptLength.toInt())
        val index = vout.toInt()
        // 兼容 SAFE 交易
        if ( txVersion >= 103 ){
            val unlockedHeight = input.readLong();
            val reserveLength = input.readVarInt();
            val reserve = input.readBytes( reserveLength.toInt() );
            /* var reserveHex = reserve.toHexString(); */
            return TransactionOutput(value, index, lockingScript,unlockedHeight = unlockedHeight,reserve = reserve)
        }
        return TransactionOutput(value, index, lockingScript)
    }

    fun serialize(output: TransactionOutput): ByteArray {
        val bout = BitcoinOutput();
        bout.writeLong(output.value)
        bout.writeVarInt(output.lockingScript.size.toLong())
        bout.write(output.lockingScript)
        if ( output.unlockedHeight != null ){
            bout.writeLong( output.unlockedHeight!! )
        }
        if ( output.reserve != null ){
            bout.writeVarInt( output.reserve!!.size.toLong() )
            bout.write( output.reserve )
        }
//        return BitcoinOutput()
//                .writeLong(output.value)
//                .writeVarInt(output.lockingScript.size.toLong())
//                .write(output.lockingScript)
//                .toByteArray()
        return bout.toByteArray();
    }
}

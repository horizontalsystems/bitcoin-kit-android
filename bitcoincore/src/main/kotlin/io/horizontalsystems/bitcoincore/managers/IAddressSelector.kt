package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

interface IRestoreKeyConverter {
    fun keysForApiRestore(publicKey: PublicKey): List<String>
    fun bloomFilterElements(publicKey: PublicKey): List<ByteArray>
}

class RestoreKeyConverterChain : IRestoreKeyConverter {

    var converters = mutableListOf<IRestoreKeyConverter>()

    fun add(converter: IRestoreKeyConverter) {
        converters.add(converter)
    }

    override fun keysForApiRestore(publicKey: PublicKey): List<String> {
        val keys = mutableListOf<String>()

        for (converter in converters) {
            keys.addAll(converter.keysForApiRestore(publicKey))
        }

        return keys.distinct()
    }

    override fun bloomFilterElements(publicKey: PublicKey): List<ByteArray> {
        val keys = mutableListOf<ByteArray>()

        for (converter in converters) {
            keys.addAll(converter.bloomFilterElements(publicKey))
        }

        return keys.distinct()
    }
}

class Bip44RestoreKeyConverter(private val addressConverter: IAddressConverter) : IRestoreKeyConverter {

    override fun keysForApiRestore(publicKey: PublicKey): List<String> {
        val legacyAddress = addressConverter.convert(publicKey, ScriptType.P2PKH).string

        return listOf(legacyAddress)
    }

    override fun bloomFilterElements(publicKey: PublicKey): List<ByteArray> {
        return listOf()
    }
}

class Bip49RestoreKeyConverter(private val addressConverter: IAddressConverter) : IRestoreKeyConverter {

    override fun keysForApiRestore(publicKey: PublicKey): List<String> {
        val wpkhShAddress = addressConverter.convert(publicKey, ScriptType.P2WPKHSH).string

        return listOf(wpkhShAddress)
    }

    override fun bloomFilterElements(publicKey: PublicKey): List<ByteArray> {
        throw java.lang.Exception("bloomFilterElements(publicKey:) has not been implemented")
    }
}


class Bip84RestoreKeyConverter(private val addressConverter: IAddressConverter) : IRestoreKeyConverter {

    override fun keysForApiRestore(publicKey: PublicKey): List<String> {
        val segwitAddress = addressConverter.convert(publicKey, ScriptType.P2WPKH).string

        return listOf(segwitAddress)
    }

    override fun bloomFilterElements(publicKey: PublicKey): List<ByteArray> {
        throw Exception("bloomFilterElements(publicKey:) has not been implemented")
    }
}

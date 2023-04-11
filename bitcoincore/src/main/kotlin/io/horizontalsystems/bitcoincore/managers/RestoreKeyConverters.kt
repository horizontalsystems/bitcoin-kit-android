package io.horizontalsystems.bitcoincore.managers

import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.utils.IAddressConverter

interface IRestoreKeyConverter {
    fun keysForApiRestore(publicKey: PublicKey): List<String>
    fun bloomFilterElements(publicKey: PublicKey): List<ByteArray>
}

class RestoreKeyConverterChain : IRestoreKeyConverter {

    private val converters = mutableListOf<IRestoreKeyConverter>()

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
        val legacyAddress = addressConverter.convert(publicKey, ScriptType.P2PKH).stringValue

        return listOf(legacyAddress)
    }

    override fun bloomFilterElements(publicKey: PublicKey): List<ByteArray> {
        return listOf(publicKey.publicKeyHash, publicKey.publicKey)
    }
}

class Bip49RestoreKeyConverter(private val addressConverter: IAddressConverter) : IRestoreKeyConverter {

    override fun keysForApiRestore(publicKey: PublicKey): List<String> {
        val wpkhShAddress = addressConverter.convert(publicKey, ScriptType.P2WPKHSH).stringValue

        return listOf(wpkhShAddress)
    }

    override fun bloomFilterElements(publicKey: PublicKey): List<ByteArray> {
        return listOf(publicKey.scriptHashP2WPKH)
    }
}


class Bip84RestoreKeyConverter(private val addressConverter: IAddressConverter) : IRestoreKeyConverter {

    override fun keysForApiRestore(publicKey: PublicKey): List<String> {
        val segwitAddress = addressConverter.convert(publicKey, ScriptType.P2WPKH).stringValue

        return listOf(segwitAddress)
    }

    override fun bloomFilterElements(publicKey: PublicKey): List<ByteArray> {
        return listOf(publicKey.publicKeyHash)
    }
}

class Bip86RestoreKeyConverter(private val addressConverter: IAddressConverter) : IRestoreKeyConverter {

    override fun keysForApiRestore(publicKey: PublicKey): List<String> {
        val taprootAddress = addressConverter.convert(publicKey, ScriptType.P2TR).stringValue

        return listOf(taprootAddress)
    }

    override fun bloomFilterElements(publicKey: PublicKey): List<ByteArray> {
        return listOf(publicKey.convertedForP2TR)
    }
}

class KeyHashRestoreKeyConverter(
    private val scriptType: ScriptType
) : IRestoreKeyConverter {
    override fun keysForApiRestore(publicKey: PublicKey): List<String> {
        return if (scriptType == ScriptType.P2TR)
            listOf(publicKey.convertedForP2TR.toHexString())
        else
            listOf(publicKey.publicKeyHash.toHexString())
    }

    override fun bloomFilterElements(publicKey: PublicKey): List<ByteArray> {
        return if (scriptType == ScriptType.P2TR)
            listOf(publicKey.convertedForP2TR)
        else
            listOf(publicKey.publicKeyHash)
    }
}

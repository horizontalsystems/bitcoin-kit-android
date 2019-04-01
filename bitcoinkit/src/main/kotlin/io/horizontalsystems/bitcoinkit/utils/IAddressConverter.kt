package io.horizontalsystems.bitcoinkit.utils

import io.horizontalsystems.bitcoinkit.exceptions.AddressFormatException
import io.horizontalsystems.bitcoinkit.models.Address
import io.horizontalsystems.bitcoinkit.transactions.scripts.ScriptType

interface IAddressConverter {
    @Throws
    fun convert(addressString: String): Address

    @Throws
    fun convert(bytes: ByteArray, scriptType: Int = ScriptType.P2PKH): Address
}

class AddressConverterChain : IAddressConverter {
    private val concreteConverters = mutableListOf<IAddressConverter>()

    fun prependConverter(converter: IAddressConverter) {
        concreteConverters.add(0, converter)
    }

    override fun convert(addressString: String): Address {
        val exceptions = mutableListOf<Exception>()

        for (converter in concreteConverters) {
            try {
                return converter.convert(addressString)
            } catch (e: Exception) {
                exceptions.add(e)
            }
        }

        val exception = AddressFormatException("No converter in chain could process the address")
        exceptions.forEach {
            exception.addSuppressed(it)
        }

        throw exception
    }

    override fun convert(bytes: ByteArray, scriptType: Int): Address {
        val exceptions = mutableListOf<Exception>()

        for (converter in concreteConverters) {
            try {
                return converter.convert(bytes, scriptType)
            } catch (e: Exception) {
                exceptions.add(e)
            }
        }

        val exception = AddressFormatException("No converter in chain could process the address")
        exceptions.forEach {
            exception.addSuppressed(it)
        }

        throw exception
    }

}

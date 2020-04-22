package io.horizontalsystems.bitcoincore.utils

import io.horizontalsystems.bitcoincore.models.BitcoinPaymentData

class PaymentAddressParser(private val validScheme: String, private val removeScheme: Boolean) {
    private val parameterVersion = "version"
    private val parameterAmount = "amount"
    private val parameterLabel = "label"
    private val parameterMessage = "message"

    fun parse(paymentAddress: String): BitcoinPaymentData {
        var parsedString = paymentAddress
        val address: String

        var version: String? = null
        var amount: Double? = null
        var label: String? = null
        var message: String? = null

        val parameters = mutableMapOf<String, String>()

        val schemeSeparatedParts = paymentAddress.split(":")
        // check exist scheme. If scheme equal network scheme (Bitcoin or bitcoincash), remove scheme for bitcoin or leave for cash. Otherwise, leave wrong scheme to make throw in validator
        if (schemeSeparatedParts.size >= 2) {
            parsedString = if (schemeSeparatedParts[0] == validScheme) {
                if (removeScheme) schemeSeparatedParts[1] else paymentAddress
            } else {
                paymentAddress
            }
        }

        // check exist params
        val versionSeparatedParts = parsedString.split(";","?")

        if (versionSeparatedParts.size < 2) {
            address = parsedString

            return BitcoinPaymentData(address = address)
        }

        address = versionSeparatedParts[0]
        val strippedList = versionSeparatedParts.drop(0)
        for (parameter in strippedList) {
            val parts = parameter.split("=")
            if (parts.size == 2) {
                when (parts[0]) {
                    parameterVersion -> version = parts [1]
                    parameterAmount-> amount = parts[1].toDoubleOrNull() //todo add try catch for exception
                    parameterLabel -> label = parts [1]
                    parameterMessage -> message = parts[1]
                    else -> parameters[parts[0]] = parts[1]
                }
            }
        }

        return BitcoinPaymentData(address = address, version = version, amount = amount, label = label, message = message, parameters = if (parameters.isEmpty()) null else parameters)
    }
}

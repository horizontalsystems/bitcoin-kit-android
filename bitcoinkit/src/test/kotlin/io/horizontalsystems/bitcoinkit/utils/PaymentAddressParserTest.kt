package io.horizontalsystems.bitcoinkit.utils

import io.horizontalsystems.bitcoinkit.models.BitcoinPaymentData
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentAddressParserTest {

    private lateinit var addressParser: PaymentAddressParser

    @Test
    fun parse_BitcoinPaymentAddress() {
        addressParser = PaymentAddressParser(validScheme = "bitcoin", removeScheme = true)

        var paymentData = BitcoinPaymentData(address = "address_data")
        checkPaymentData(addressParser = addressParser, paymentAddress = "address_data", paymentData = paymentData)

        // Check bitcoin addresses parsing with drop scheme if it's valid
        paymentData = BitcoinPaymentData(address = "address_data")
        checkPaymentData(addressParser = addressParser, paymentAddress = "bitcoin:address_data", paymentData = paymentData)

        // invalid scheme - need to keep scheme
        paymentData = BitcoinPaymentData(address = "bitcoincash:address_data")
        checkPaymentData(addressParser = addressParser, paymentAddress = "bitcoincash:address_data", paymentData = paymentData)

        // check parameters
        paymentData = BitcoinPaymentData(address = "address_data", version = "1.0")
        checkPaymentData(addressParser = addressParser, paymentAddress = "address_data;version=1.0", paymentData = paymentData)

        paymentData = BitcoinPaymentData(address = "address_data", version = "1.0", label = "test")
        checkPaymentData(addressParser = addressParser, paymentAddress = "bitcoin:address_data;version=1.0?label=test", paymentData = paymentData)

        paymentData = BitcoinPaymentData(address = "address_data", amount = 0.01)
        checkPaymentData(addressParser = addressParser, paymentAddress = "bitcoin:address_data?amount=0.01", paymentData = paymentData)

        paymentData = BitcoinPaymentData(address = "address_data", amount = 0.01, label = "test_sender")
        checkPaymentData(addressParser = addressParser, paymentAddress = "bitcoin:address_data?amount=0.01?label=test_sender", paymentData = paymentData)

        paymentData = BitcoinPaymentData(address = "address_data", parameters = mutableMapOf("custom" to "any"))
        checkPaymentData(addressParser = addressParser, paymentAddress = "bitcoin:address_data?custom=any", paymentData = paymentData)
    }

    @Test
    fun parse_BitcoinCashPaymentAddress() {
        addressParser = PaymentAddressParser(validScheme = "bitcoincash", removeScheme = false)

        var paymentData = BitcoinPaymentData(address = "address_data")
        checkPaymentData(addressParser = addressParser, paymentAddress = "address_data", paymentData = paymentData)

        // Check bitcoincash addresses parsing with keep scheme if it's valid
        paymentData = BitcoinPaymentData(address = "bitcoincash:address_data")
        checkPaymentData(addressParser = addressParser, paymentAddress = "bitcoincash:address_data", paymentData = paymentData)

        // invalid scheme - need to leave scheme
        paymentData = BitcoinPaymentData(address = "bitcoin:address_data")
        checkPaymentData(addressParser = addressParser, paymentAddress = "bitcoin:address_data", paymentData = paymentData)

        // check parameters
        paymentData = BitcoinPaymentData(address = "address_data", version = "1.0")
        checkPaymentData(addressParser = addressParser, paymentAddress = "address_data;version=1.0", paymentData = paymentData)

        paymentData = BitcoinPaymentData(address = "bitcoincash:address_data", version = "1.0", label = "test")
        checkPaymentData(addressParser = addressParser, paymentAddress = "bitcoincash:address_data;version=1.0?label=test", paymentData = paymentData)

        paymentData = BitcoinPaymentData(address = "bitcoincash:address_data", amount = 0.01)
        checkPaymentData(addressParser = addressParser, paymentAddress = "bitcoincash:address_data?amount=0.01", paymentData = paymentData)

        paymentData = BitcoinPaymentData(address = "bitcoincash:address_data", amount = 0.01, label = "test_sender")
        checkPaymentData(addressParser = addressParser, paymentAddress = "bitcoincash:address_data?amount=0.01?label=test_sender", paymentData = paymentData)

        paymentData = BitcoinPaymentData(address = "bitcoincash:address_data", parameters = mutableMapOf("custom" to "any"))
        checkPaymentData(addressParser = addressParser, paymentAddress = "bitcoincash:address_data?custom=any", paymentData = paymentData)
    }

    private fun checkPaymentData(addressParser: PaymentAddressParser, paymentAddress: String, paymentData: BitcoinPaymentData) {
        val bitcoinPaymentData = addressParser.parse(paymentAddress = paymentAddress)
        assertEquals(bitcoinPaymentData, paymentData)
    }
}

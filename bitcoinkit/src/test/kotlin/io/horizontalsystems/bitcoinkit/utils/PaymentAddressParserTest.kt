package io.horizontalsystems.bitcoinkit.utils

import io.horizontalsystems.bitcoinkit.models.BitcoinPaymentData
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentAddressParserTest {

    private lateinit var addressParser: PaymentAddressParser

    @Test
    fun parse_BitcoinPaymentAddress() {
        addressParser = PaymentAddressParser("bitcoin", true)

        var paymentData = BitcoinPaymentData(address = "address_data")
        checkPaymentData(addressParser, "address_data", paymentData)

        // Check bitcoin addresses parsing with drop scheme if it's valid
        paymentData = BitcoinPaymentData(address = "address_data")
        checkPaymentData(addressParser, "bitcoin:address_data", paymentData)

        // invalid scheme - need to keep scheme
        paymentData = BitcoinPaymentData(address = "bitcoincash:address_data")
        checkPaymentData(addressParser, "bitcoincash:address_data", paymentData)

        // check parameters
        paymentData = BitcoinPaymentData(address = "address_data", version = "1.0")
        checkPaymentData(addressParser, "address_data;version=1.0", paymentData)

        paymentData = BitcoinPaymentData(address = "address_data", version = "1.0", label = "test")
        checkPaymentData(addressParser, "bitcoin:address_data;version=1.0?label=test", paymentData)

        paymentData = BitcoinPaymentData(address = "address_data", amount = 0.01)
        checkPaymentData(addressParser, "bitcoin:address_data?amount=0.01", paymentData)

        paymentData = BitcoinPaymentData(address = "address_data", amount = 0.01, label = "test_sender")
        checkPaymentData(addressParser, "bitcoin:address_data?amount=0.01?label=test_sender", paymentData)

        paymentData = BitcoinPaymentData(address = "address_data", parameters = mutableMapOf("custom" to "any"))
        checkPaymentData(addressParser, "bitcoin:address_data?custom=any", paymentData)
    }

    @Test
    fun parse_BitcoinCashPaymentAddress() {
        addressParser = PaymentAddressParser("bitcoincash", false)

        var paymentData = BitcoinPaymentData(address = "address_data")
        checkPaymentData(addressParser, "address_data", paymentData)

        // Check bitcoincash addresses parsing with keep scheme if it's valid
        paymentData = BitcoinPaymentData(address = "bitcoincash:address_data")
        checkPaymentData(addressParser, "bitcoincash:address_data", paymentData)

        // invalid scheme - need to leave scheme
        paymentData = BitcoinPaymentData(address = "bitcoin:address_data")
        checkPaymentData(addressParser, "bitcoin:address_data", paymentData)

        // check parameters
        paymentData = BitcoinPaymentData(address = "address_data", version = "1.0")
        checkPaymentData(addressParser, "address_data;version=1.0", paymentData)

        paymentData = BitcoinPaymentData(address = "bitcoincash:address_data", version = "1.0", label = "test")
        checkPaymentData(addressParser, "bitcoincash:address_data;version=1.0?label=test", paymentData)

        paymentData = BitcoinPaymentData(address = "bitcoincash:address_data", amount = 0.01)
        checkPaymentData(addressParser, "bitcoincash:address_data?amount=0.01", paymentData)

        paymentData = BitcoinPaymentData(address = "bitcoincash:address_data", amount = 0.01, label = "test_sender")
        checkPaymentData(addressParser, "bitcoincash:address_data?amount=0.01?label=test_sender", paymentData)

        paymentData = BitcoinPaymentData(address = "bitcoincash:address_data", parameters = mutableMapOf("custom" to "any"))
        checkPaymentData(addressParser, "bitcoincash:address_data?custom=any", paymentData)
    }

    private fun checkPaymentData(addressParser: PaymentAddressParser, paymentAddress: String, paymentData: BitcoinPaymentData) {
        val bitcoinPaymentData = addressParser.parse(paymentAddress)
        assertEquals(bitcoinPaymentData, paymentData)
    }
}

package io.horizontalsystems.bitcoincore.transactions

import com.nhaarman.mockitokotlin2.*
import io.horizontalsystems.bitcoincore.managers.SelectedUnspentOutputInfo
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelector
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.randomBytes
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import org.junit.Assert.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TransactionFeeCalculatorTests : Spek({
    val unspentOutputSelector = mock<UnspentOutputSelector>()
    val transactionSizeCalculator = mock<TransactionSizeCalculator> {
        on { signatureLength } doReturn 74 + 1
        on { pubKeyLength } doReturn 33 + 1
    }

    val toAddress = LegacyAddress("toAddress", randomBytes(), AddressType.P2PKH)
    val changeAddress = LegacyAddress("changeAddress", randomBytes(), AddressType.P2PKH)

    val value = 100_000_000L
    val fee = 1000L
    val feeRate = 10
    val senderPay = true

    val unspentOutputs = listOf(UnspentOutput(TransactionOutput(200_000_000, 0, randomBytes(), ScriptType.P2PKH), PublicKey(0, 0, false, randomBytes(), randomBytes()), Transaction(), null))
    val calculator = TransactionFeeCalculator(unspentOutputSelector, transactionSizeCalculator)

    var selectedOutputsInfo = SelectedUnspentOutputInfo(unspentOutputs, 100_000_000, fee, true)

    beforeEachTest {
        whenever(unspentOutputSelector.select(any(), any(), any(), any(), any())).thenReturn(selectedOutputsInfo)
    }

    afterEachTest {
        reset(unspentOutputSelector, transactionSizeCalculator)
    }

    describe("#fee(value,feeRate,senderPay,toAddress?,changeAddress)") {

        context("when toAddress exists") {

            context("selects unspent outputs with toAddress scriptType") {

                it("selects unspent outputs with given parameters") {
                    calculator.fee(value, feeRate, senderPay, toAddress, changeAddress)
                    verify(unspentOutputSelector).select(value, feeRate, toAddress.scriptType, changeAddress.scriptType, senderPay)
                }


            }

        }

        context("when toAddress is nil") {
            it("selects unspent outputs with changeAddress scriptType") {
                val resultFee = calculator.fee(value, feeRate, senderPay, null, changeAddress)

                verify(unspentOutputSelector).select(value, feeRate, outputType = changeAddress.scriptType, changeType = changeAddress.scriptType, senderPay = true)
                assertEquals(fee, resultFee)
            }
        }
    }

    describe("fee(inputScriptType,outputScriptType,feeRate,signatureScriptFunction") {

        it("calculates fee from transaction size returns it") {
            val signatureData = listOf(randomBytes(transactionSizeCalculator.signatureLength), randomBytes(transactionSizeCalculator.pubKeyLength))
            val signatureScript = randomBytes(100)
            val transactionSize = 500L

            whenever(transactionSizeCalculator.transactionSize(any(), any())).thenReturn(transactionSize)

            val resultFee = calculator.fee(ScriptType.P2PKH, ScriptType.P2PKH, feeRate) { signature, publicKey ->
                assertEquals(signature.size, signatureData[0].size)
                assertEquals(publicKey.size, signatureData[1].size)

                signatureScript
            }

            val expectedFee = (transactionSize + signatureScript.size) * feeRate
            assertEquals(expectedFee, resultFee)

            verify(transactionSizeCalculator).transactionSize(inputs = listOf(ScriptType.P2PKH), outputs = listOf(ScriptType.P2PKH))
        }
    }

    describe("#feeWithUnspentOutputs") {
        it("selects unspent outputs with given parameters and returns it") {
            val feeWithUnspentOutputs = calculator.feeWithUnspentOutputs(value, feeRate, toAddress.scriptType, changeAddress.scriptType, senderPay)
            verify(unspentOutputSelector).select(value, feeRate, outputType = toAddress.scriptType, changeType = changeAddress.scriptType, senderPay = true)

            assertEquals(feeWithUnspentOutputs.fee, selectedOutputsInfo.fee)
        }
    }
})

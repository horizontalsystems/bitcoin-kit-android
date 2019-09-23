package io.horizontalsystems.bitcoincore.transactions.builder

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.randomBytes
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.scripts.OpCodes
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptBuilder
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import org.junit.jupiter.api.Assertions.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TransactionBuilderTest : Spek({

    val toAddressPKH = LegacyAddress("toAddressPKH", randomBytes(32), AddressType.P2PKH)
    val toAddressSH = LegacyAddress("toAddressSH", randomBytes(32), AddressType.P2SH)
    val changeAddressPKH = LegacyAddress("changeAddressPKH", randomBytes(32), AddressType.P2PKH)
    val changeAddressWPKH = SegWitAddress("changeAddressWPKH", randomBytes(20), AddressType.WITNESS, 0)

    val signatureData = listOf(randomBytes(72), randomBytes(64))
    val sendingValue = 100_000_000L
    val fee = 1000L
    val lastBlockHeight = 1000L

    val createOutput: (Address) -> TransactionOutput = {
        TransactionOutput(0L, 0, ByteArray(0), keyHash = it.hash)
    }

    val inputSigner = mock<InputSigner> {
        on { sigScriptData(any(), any(), any(), any()) } doReturn signatureData
    }

    val scriptBuilder = mock<ScriptBuilder> {
        on { lockingScript(any()) } doReturn ByteArray(0)
        on { unlockingScript(any()) } doReturn ScriptBuilder().unlockingScript(signatureData)
    }

    val transactionOutputHash = randomBytes(32)
    val transactionOutput = mock<TransactionOutput> {
        on { value } doReturn 200_000_000L
        on { index } doReturn 0
        on { lockingScript } doReturn randomBytes(32)
        on { scriptType } doReturn ScriptType.P2PKH
        on { transactionHash } doReturn transactionOutputHash
    }

    lateinit var unspentOutput: UnspentOutput
    lateinit var inputToSign: InputToSign
    lateinit var fullTransaction: FullTransaction

    beforeEachTest {
        val transactionInput = mock<TransactionInput> {
            on { previousOutputTxHash } doReturn transactionOutputHash
            on { previousOutputIndex } doReturn 0
            on { sigScript } doReturn ByteArray(0)
        }

        val publicKey = mock<PublicKey> {
            on { publicKeyHash } doReturn randomBytes(32)
        }

        unspentOutput = mock {
            on { this.publicKey } doReturn publicKey
            on { this.output } doReturn transactionOutput
        }

        inputToSign = mock {
            on { input } doReturn transactionInput
            on { previousOutput } doReturn transactionOutput
            on { previousOutputPublicKey } doReturn publicKey
        }
    }

    val builder by memoized {
        TransactionBuilder(scriptBuilder, inputSigner)
    }

    describe("#buildTransaction") {

        context("when unspentOutput is P2PKH, senderPay is true, addChangeOutput is true") {
            beforeEach {
                fullTransaction = builder.buildTransaction(sendingValue, listOf(unspentOutput), fee, true, toAddressPKH, changeAddressPKH, lastBlockHeight)
            }

            it("adds input from unspentOutput") {
                assertEquals(1, fullTransaction.inputs.size)
                assertArrayEquals(inputToSign.input.previousOutputTxHash, fullTransaction.inputs[0].previousOutputTxHash)
            }

            it("adds 1 output for toAddress") {
                val toOutput = createOutput(toAddressPKH)

                assertEquals(2, fullTransaction.outputs.size)
                assertEquals(0, fullTransaction.outputs[0].index)
                assertEquals(toOutput.keyHash, fullTransaction.outputs[0].keyHash)
            }

            it("adds 1 output for changeAddress") {
                val toOutput = createOutput(changeAddressPKH)

                assertEquals(2, fullTransaction.outputs.size)
                assertEquals(1, fullTransaction.outputs[1].index)
                assertEquals(toOutput.keyHash, fullTransaction.outputs[1].keyHash)
            }

            it("signs the input") {
                val signature = OpCodes.push(signatureData[0]) + OpCodes.push(signatureData[1])
                assertArrayEquals(signature, fullTransaction.inputs[0].sigScript)
            }

            it("sets transaction properties") {
                assertEquals(Transaction.Status.NEW, fullTransaction.header.status)
                assertEquals(true, fullTransaction.header.isMine)
                assertEquals(true, fullTransaction.header.isOutgoing)
                assertEquals(false, fullTransaction.header.segwit)
            }
        }

        context("when changeAddress is nil") {
            beforeEach {
                fullTransaction = builder.buildTransaction(sendingValue, listOf(unspentOutput), fee, true, toAddressPKH, null, lastBlockHeight)
            }

            it("doesn't add 1 output for changeAddress") {
                val output = fullTransaction.outputs[0]

                assertEquals(1, fullTransaction.outputs.size)
                assertEquals(sendingValue, output.value)
                assertArrayEquals(toAddressPKH.hash, output.keyHash)
            }
        }

        context("when senderPay is false") {
            beforeEach {
                fullTransaction = builder.buildTransaction(sendingValue, listOf(unspentOutput), fee, false, toAddressPKH, changeAddressPKH, lastBlockHeight)
            }

            it("subtracts fee from value in receiver output") {
                val receivedValue = sendingValue - fee
                val output1 = fullTransaction.outputs[0]

                assertEquals(receivedValue, output1.value)
                assertArrayEquals(toAddressPKH.hash, output1.keyHash)
            }

            it("puts the remained value in change output") {
                val changeValue = unspentOutput.output.value - sendingValue
                val output2 = fullTransaction.outputs[1]

                assertEquals(changeValue, output2.value)
                assertArrayEquals(changeAddressPKH.hash, output2.keyHash)
            }
        }

        context("when toAddress and/or changeAddress types are P2SH or P2WPKH") {
            beforeEach {
                fullTransaction = builder.buildTransaction(sendingValue, listOf(unspentOutput), fee, true, toAddressSH, changeAddressWPKH, lastBlockHeight)
            }

            it("generates output with SH") {
                val output1 = fullTransaction.outputs[0]

                assertEquals(2, fullTransaction.outputs.size)
                assertEquals(ScriptType.P2SH, output1.scriptType)
                assertArrayEquals(toAddressSH.hash, output1.keyHash)
            }

            it("generates output with WPKH") {
                val changeOutput = fullTransaction.outputs[1]

                assertEquals(2, fullTransaction.outputs.size)

                assertEquals(ScriptType.P2WPKH, changeOutput.scriptType)
                assertArrayEquals(changeAddressWPKH.hash, changeOutput.keyHash)
            }
        }

        context("when unspent output is P2WPKH") {
            beforeEach {
                whenever(unspentOutput.output.scriptType).thenReturn(ScriptType.P2WPKH)

                fullTransaction = builder.buildTransaction(sendingValue, listOf(unspentOutput), fee, true, toAddressPKH, changeAddressPKH, lastBlockHeight)
            }

            it("sets P2WPKH unlocking script to witnessData") {
                assertArrayEquals(signatureData.toTypedArray(), fullTransaction.inputs[0].witness.toTypedArray())
            }

            it("sets empty data to signatureScript") {
                assertArrayEquals(ByteArray(0), fullTransaction.inputs[0].sigScript)
            }

            it("sets segWit flag to true") {
                assertTrue(fullTransaction.header.segwit)
            }
        }

        context("when unspent output is P2WPKH(SH") {
            lateinit var signatureScript: ByteArray

            beforeEach {
                val witnessProgram = OpCodes.scriptWPKH(unspentOutput.publicKey.publicKeyHash)
                signatureScript = ScriptBuilder().unlockingScript(listOf(witnessProgram))

                whenever(unspentOutput.output.scriptType).thenReturn(ScriptType.P2WPKHSH)
                whenever(scriptBuilder.unlockingScript(any())).thenReturn(signatureScript)

                fullTransaction = builder.buildTransaction(sendingValue, listOf(unspentOutput), fee, true, toAddressPKH, changeAddressPKH, lastBlockHeight)
            }

            it("sets P2WPKH unlocking script to witnessData") {
                assertArrayEquals(signatureData.toTypedArray(), fullTransaction.inputs[0].witness.toTypedArray())
            }

            it("sets P2SH unlocking script to signatureScript") {
                assertArrayEquals(signatureScript, fullTransaction.inputs[0].sigScript)
            }

            it("sets segWit flag to true") {
                assertTrue(fullTransaction.header.segwit)
            }
        }

        context("value less than fee") {
            it("throws feeMoreThanValue exception") {
                try {
                    builder.buildTransaction(fee - 1, listOf(unspentOutput), fee, false, toAddressPKH, changeAddressPKH, lastBlockHeight)
                    fail("Expecting an exception")
                } catch (ex: TransactionBuilder.BuilderException) {
                    assertTrue(ex is TransactionBuilder.BuilderException.FeeMoreThanValue)
                } catch (ex: Exception) {
                    fail("Expecting an exception")
                }
            }
        }

        context("when unspent output is not supported") {
            beforeEach {
                whenever(unspentOutput.output.scriptType).thenReturn(ScriptType.P2SH)
            }

            it("throws notSupportedScriptType exception") {
                try {
                    builder.buildTransaction(sendingValue, listOf(unspentOutput), fee, false, toAddressPKH, changeAddressPKH, lastBlockHeight)
                    fail("Expecting an exception")
                } catch (ex: TransactionBuilder.BuilderException) {
                    assertTrue(ex is TransactionBuilder.BuilderException.NotSupportedScriptType)
                } catch (ex: Exception) {
                    fail("Expecting an exception")
                }
            }
        }
    }

    describe("#buildTransaction(P2SH)") {
        val signatureScript = randomBytes(100)
        val signatureScriptFunction: (a: ByteArray, b: ByteArray) -> ByteArray = { _, _ ->
            signatureScript
        }

        beforeEach {
            whenever(transactionOutput.scriptType).thenReturn(ScriptType.P2SH)
        }

        context("when fee is valid, unspent output type is P2SH") {
            beforeEach {
                fullTransaction = builder.buildTransaction(unspentOutput, toAddressPKH, fee, lastBlockHeight, signatureScriptFunction)
            }

            it("adds input from unspentOutput") {
                assertEquals(1, fullTransaction.inputs.size)
                assertEquals(inputToSign.input.previousOutputIndex, fullTransaction.inputs[0].previousOutputIndex)
                assertArrayEquals(inputToSign.input.previousOutputTxHash, fullTransaction.inputs[0].previousOutputTxHash)
            }

            it("adds 1 output for toAddress") {
                val toOutput = createOutput(toAddressPKH)

                assertEquals(1, fullTransaction.outputs.size)
                assertEquals(0, fullTransaction.outputs[0].index)
                assertEquals(toOutput.keyHash, fullTransaction.outputs[0].keyHash)
            }

            it("signs the input") {
                assertArrayEquals(signatureScript, fullTransaction.inputs[0].sigScript)
            }

            it("sets transaction properties") {
                assertEquals(Transaction.Status.NEW, fullTransaction.header.status)
                assertEquals(true, fullTransaction.header.isMine)
                assertEquals(false, fullTransaction.header.isOutgoing)
                assertEquals(false, fullTransaction.header.segwit)
            }
        }
    }
})

package io.horizontalsystems.dashkit.instantsend

import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.dashkit.DashKitErrors
import io.horizontalsystems.dashkit.IDashStorage
import io.horizontalsystems.dashkit.InstantSend
import io.horizontalsystems.dashkit.models.InstantTransactionInput
import io.horizontalsystems.dashkit.models.InstantTransactionState

class InstantTransactionManager(
        private val storage: IDashStorage,
        private val instantSendFactory: InstantSendFactory,
        private val state: InstantTransactionState
) {
    init {
        state.instantTransactionHashes = storage.instantTransactionHashes().toMutableList()
    }

    fun instantTransactionInputs(txHash: ByteArray, instantTransaction: FullTransaction?): List<InstantTransactionInput> {
        // check if inputs already created
        val inputs = storage.instantTransactionInputs(txHash)
        if (inputs.isNotEmpty()) {
            return inputs
        }

        // if not check coming ix
        instantTransaction?.let { transaction ->
            return makeInputs(txHash, transaction.inputs)
        }

        // if we can't get inputs and ix is null, try get tx inputs from db
        return makeInputs(txHash, storage.getTransactionInputs(txHash))
    }

    @Throws
    fun updateInput(inputTxHash: ByteArray, transactionInputs: List<InstantTransactionInput>) {
        val updatedInputs = transactionInputs.toMutableList()

        val inputIndex = transactionInputs.indexOfFirst { it.inputTxHash.contentEquals(inputTxHash) }
        if (inputIndex == -1) {
            throw DashKitErrors.LockVoteValidation.TxInputNotFound()
        }

        val input = transactionInputs[inputIndex]
        val increasedInput = instantSendFactory.instantTransactionInput(input.txHash, input.inputTxHash, input.voteCount + 1, input.blockHeight)
        storage.addInstantTransactionInput(increasedInput)

        updatedInputs[inputIndex] = increasedInput
        if (updatedInputs.none { it.voteCount < InstantSend.requiredVoteCount }) {
            state.append(input.txHash)
            storage.addInstantTransactionHash(input.txHash)
            storage.removeInstantTransactionInputs(input.txHash)
        }
    }

    fun isTransactionInstant(txHash: ByteArray): Boolean {
        return state.instantTransactionHashes.any { it.contentEquals(txHash) }
    }

    fun isTransactionExists(txHash: ByteArray): Boolean {
        return storage.isTransactionExists(txHash)
    }

    fun makeInstant(txHash: ByteArray) {
        state.append(txHash)
        storage.addInstantTransactionHash(txHash)
    }

    private fun makeInputs(txHash: ByteArray, inputs: List<TransactionInput>): List<InstantTransactionInput> {
        val instantInputs = mutableListOf<InstantTransactionInput>()
        for (input in inputs) {
            val instantInput = instantSendFactory.instantTransactionInput(txHash, input.previousOutputTxHash, 0, null)

            storage.addInstantTransactionInput(instantInput)
            instantInputs.add(instantInput)
        }
        return instantInputs
    }

}
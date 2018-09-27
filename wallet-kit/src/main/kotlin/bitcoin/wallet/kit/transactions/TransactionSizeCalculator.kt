package bitcoin.wallet.kit.transactions

import bitcoin.wallet.kit.scripts.ScriptType

class TransactionSizeCalculator {

    companion object {
        private const val VERSION_SIZE = 4
        private const val OUTPUT_COUNT_SIZE = 1
        private const val INPUT_COUNT_SIZE = 1
        private const val LOCK_TIME_SIZE = 4

        private const val OUTPUT_HEX_SIZE = 32
        private const val OUTPUT_INDEX_SIZE = 4
        private const val SEQUENCE_SIZE = 4
        private const val SCRIPT_SIZE = 1
        private const val VALUE_SIZE = 8

        private const val SIGNATURE_SIZE = 73
        private const val PUB_KEY_SIZE = 33
        private const val PUB_KEY_HASH_SIZE = 20
        private const val SCRIPT_HASH_ADDRESS_SIZE = 20

        private const val OP_PUSHDATA_SIZE = 1
        private const val OP_CHECKSIG_SIZE = 1
        private const val OP_DUP_SIZE = 1
        private const val OP_HASH160_SIZE = 1
        private const val OP_EQUALVERIFY_SIZE = 1
    }

    val emptyTxSize = VERSION_SIZE + OUTPUT_COUNT_SIZE + INPUT_COUNT_SIZE + LOCK_TIME_SIZE

    fun outputSize(scripType: Int) = VALUE_SIZE + SCRIPT_SIZE + getLockingScriptSize(scripType)

    fun inputSize(scriptType: Int): Int {
        return OUTPUT_HEX_SIZE + OUTPUT_INDEX_SIZE + SCRIPT_SIZE + getUnlockingScriptSize(scriptType) + SEQUENCE_SIZE
    }

    private fun getUnlockingScriptSize(scriptType: Int) = OP_PUSHDATA_SIZE + SIGNATURE_SIZE + when (scriptType) {
        ScriptType.P2PK -> 0
        ScriptType.P2PKH -> OP_PUSHDATA_SIZE + PUB_KEY_SIZE
        ScriptType.P2SH -> OP_PUSHDATA_SIZE + SCRIPT_HASH_ADDRESS_SIZE
        else -> 0
    }

    private fun getLockingScriptSize(scriptType: Int) = when (scriptType) {
        ScriptType.P2PK -> OP_PUSHDATA_SIZE + PUB_KEY_SIZE + OP_CHECKSIG_SIZE
        ScriptType.P2PKH -> OP_DUP_SIZE + OP_HASH160_SIZE + OP_PUSHDATA_SIZE + PUB_KEY_HASH_SIZE + OP_EQUALVERIFY_SIZE + OP_CHECKSIG_SIZE
        ScriptType.P2SH -> 23 //todo need to change after adding p2sh addresses
        else -> 0
    }

}

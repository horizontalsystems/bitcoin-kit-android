package io.horizontalsystems.bitcoincore.transactions

import io.horizontalsystems.bitcoincore.core.IStorage
import io.horizontalsystems.bitcoincore.core.PluginManager
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.transactions.scripts.*
import io.horizontalsystems.bitcoincore.utils.IAddressConverter
import io.horizontalsystems.bitcoincore.utils.Utils
import java.util.*

class TransactionExtractor(
        private val addressConverter: IAddressConverter,
        private val storage: IStorage,
        private val pluginManager: PluginManager,
        private val outputsCache: OutputsCache
) {

    fun extractOutputs(transaction: FullTransaction) {
        var nullDataOutput : TransactionOutput? = null
        for (output in transaction.outputs) {
            val payload: ByteArray
            val scriptType: ScriptType

            val lockingScript = output.lockingScript

            if (isP2PKH(lockingScript)) {
                payload = lockingScript.copyOfRange(3, 23)
                scriptType = ScriptType.P2PKH
            } else if (isP2PK(lockingScript)) {
                payload = lockingScript.copyOfRange(1, lockingScript.size - 1)
                scriptType = ScriptType.P2PK
            } else if (isP2SH(lockingScript)) {
                payload = lockingScript.copyOfRange(2, lockingScript.size - 1)
                scriptType = ScriptType.P2SH
            } else if (isP2WPKH(lockingScript)) {
                payload = lockingScript
                scriptType = ScriptType.P2WPKH
            } else if (isNullData(lockingScript)) {
                payload = lockingScript
                scriptType = ScriptType.NULL_DATA
                nullDataOutput = output
            } else continue

            output.scriptType = scriptType
            output.keyHash = payload

            // Set public key if exist
            getPublicKey(output)?.let {
                transaction.header.isMine = true
                output.setPublicKey(it)
            }
        }

        nullDataOutput?.let {
            pluginManager.processTransactionWithNullData(transaction, it)
        }

    }

    fun extractInputs(transaction: FullTransaction) {
        for (input in transaction.inputs) {
            val previousOutput = storage.getPreviousOutput(input)
            if (previousOutput != null) {
                input.address = previousOutput.address
                input.keyHash = previousOutput.keyHash
                continue
            }

            val payload: ByteArray
            val scriptType: ScriptType
            val sigScript = input.sigScript

            //  P2SH script {push-sig}{signature}{push-redeem}{script}
            val scriptHash = getScriptHash(sigScript)
            if (scriptHash != null) {
                payload = scriptHash
                scriptType = ScriptType.P2SH
            }
            //  P2PKH {signature}{public-key} script is 107±1 bytes
            else if (sigScript.size >= 106 && sigScript[0] in 71..74) {
                val signOffset = sigScript[0].toInt()
                val pubKeySize = sigScript[signOffset + 1].toInt()

                if ((pubKeySize == 33 || pubKeySize == 65) && sigScript.size == signOffset + pubKeySize + 2) {
                    scriptType = ScriptType.P2PKH
                    payload = Arrays.copyOfRange(sigScript, signOffset + 2, sigScript.size)
                } else continue
            }
            //  P2WPKHSH 0x16 00 14 {20-byte-key-hash}
            else if (sigScript.size == 23 && sigScript[0] == 0x16.toByte() &&
                    (sigScript[1] == 0.toByte() || sigScript[1] in 0x50..0x61) &&
                    sigScript[2] == 0x14.toByte()) {
                payload = sigScript.drop(1).toByteArray()
                scriptType = ScriptType.P2WPKHSH
            } else continue

            try {
                val keyHash = Utils.sha256Hash160(payload)
                val address = addressConverter.convert(keyHash, scriptType)
                input.keyHash = address.hash
                input.address = address.string

            } catch (e: Exception) {
            }
        }
    }

    fun extractAddress(transaction: FullTransaction) {
        for (output in transaction.outputs) {
            val outKeyHash = output.keyHash ?: continue
            val scriptType = output.scriptType

            val pubkeyHash = when (scriptType) {
                ScriptType.P2PK -> Utils.sha256Hash160(outKeyHash)
                ScriptType.P2WPKHSH -> Utils.sha256Hash160(OpCodes.scriptWPKH(outKeyHash))
                else -> outKeyHash
            }

            try {
                output.address = addressConverter.convert(pubkeyHash, scriptType).string
            } catch (e: Exception) {
            }
        }
    }

    private fun getPublicKey(output: TransactionOutput): PublicKey? {
        var keyHash = output.keyHash ?: return null

        if (output.scriptType == ScriptType.P2WPKH) {
            keyHash = keyHash.drop(2).toByteArray()
        } else if (output.scriptType == ScriptType.P2SH) {
            storage.getPublicKeyByScriptHashForP2PWKH(keyHash)?.let {
                output.scriptType = ScriptType.P2WPKHSH
                output.keyHash = it.publicKeyHash
                return it
            }
        }

        return storage.getPublicKeyByKeyOrKeyHash(keyHash)
    }

    //
    // Parse Outputs
    //

    // 25 bytes script: 76 A9 14 {20-byte-key-hash} 88 AC
    private fun isP2PKH(lockingScript: ByteArray): Boolean {
        return (lockingScript.size == 25 &&
                lockingScript[0] == OP_DUP.toByte() &&
                lockingScript[1] == OP_HASH160.toByte() &&
                lockingScript[2] == 20.toByte() &&
                lockingScript[23] == OP_EQUALVERIFY.toByte() &&
                lockingScript[24] == OP_CHECKSIG.toByte())
    }

    // 35/67 bytes script: {push-length-byte 33/65}{33/65-byte-public-key} AC
    private fun isP2PK(lockingScript: ByteArray): Boolean {
        return ((lockingScript.size == 35 || lockingScript.size == 67) &&
                (lockingScript[0] == 33.toByte() || lockingScript[0] == 65.toByte()) &&
                lockingScript[lockingScript.size - 1] == OP_CHECKSIG.toByte())
    }

    // 23 bytes script: A9 14 {20-byte-script-hash} 87
    private fun isP2SH(lockingScript: ByteArray): Boolean {
        return (lockingScript.size == 23 &&
                lockingScript[0] == OP_HASH160.toByte() &&
                lockingScript[1] == 20.toByte() &&
                lockingScript[lockingScript.size - 1] == OP_EQUAL.toByte())
    }

    // 22 bytes script: {version-byte 00/81-96} 14 {20-byte-key-hash}
    private fun isP2WPKH(lockingScript: ByteArray): Boolean {
        return (lockingScript.size == 22 &&
                (lockingScript[0] == 0.toByte() || lockingScript[0] in 0x50..0x61) &&
                lockingScript[1] == 20.toByte())
    }

    private fun isNullData(lockingScript: ByteArray): Boolean {
        return lockingScript.isNotEmpty() && lockingScript[0] == OP_RETURN.toByte()
    }

    //
    // Parse Inputs
    //

    // P2SH input {push-sig}{signature}{push-redeem}{script}
    private fun getScriptHash(bytes: ByteArray): ByteArray? {
        val script = Script(bytes)
        if (script.chunks.isEmpty())
            return null

        //  Grab the raw redeem script bytes
        val redeemChunk = script.chunks.last()
        if (redeemChunk.data == null)
            return null

        val redeemScript = Script(redeemChunk.data)
        if (redeemScript.chunks.isEmpty())
            return null

        var chunkLast = redeemScript.chunks.last()
        if (chunkLast.opcode == OP_ENDIF && redeemScript.chunks.size > 1) {
            val chunk = redeemScript.chunks.takeLast(2).firstOrNull()
            if (chunk != null) {
                chunkLast = chunk
            }
        }

        val checks = listOf(OP_CHECKSIG, OP_CHECKSIGVERIFY, OP_CHECKMULTISIGVERIFY, OP_CHECKMULTISIG)
        if (checks.contains(chunkLast.opcode)) {
            return redeemChunk.data
        }

        return null
    }

    fun extract(transaction: FullTransaction) {
        extractOutputs(transaction)

        if (outputsCache.hasOutputs(transaction.inputs)) {
            transaction.header.isMine = true
            transaction.header.isOutgoing = true
        }

        if (transaction.header.isMine) {
            outputsCache.add(transaction.outputs)
            extractAddress(transaction)
            extractInputs(transaction)
        }
    }
}

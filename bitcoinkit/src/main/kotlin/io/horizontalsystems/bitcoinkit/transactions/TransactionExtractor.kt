package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.models.PublicKey
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.models.TransactionOutput
import io.horizontalsystems.bitcoinkit.transactions.scripts.*
import io.horizontalsystems.bitcoinkit.utils.AddressConverter
import io.horizontalsystems.bitcoinkit.utils.Utils
import io.realm.Realm
import java.util.*

class TransactionExtractor(private val addressConverter: AddressConverter) {
    fun extract(transaction: Transaction, realm: Realm, outputsOnly: Boolean = true) = when (outputsOnly) {
        true -> parseOutputs(transaction, realm)
        false -> parseInputs(transaction)
    }

    fun extractAddress(transaction: Transaction) {
        for (output in transaction.outputs) {
            val outKeyHash = output.keyHash ?: continue
            val scriptType = output.scriptType

            val pubkeyHash = when (scriptType) {
                ScriptType.P2PK -> {
                    output.publicKey?.publicKeyHash ?: Utils.sha256Hash160(outKeyHash)
                }
                else -> outKeyHash
            }

            try {
                output.address = addressConverter.convert(pubkeyHash, scriptType).string
            } catch (e: Exception) {
            }
        }
    }

    private fun parseOutputs(transaction: Transaction, realm: Realm) {
        for (output in transaction.outputs) {
            val payload: ByteArray
            val scriptType: Int

            val lockingScript = output.lockingScript

            if (isP2PKH(lockingScript)) {
                payload = Arrays.copyOfRange(lockingScript, 3, 23)
                scriptType = ScriptType.P2PKH
            } else if (isP2PK(lockingScript)) {
                payload = Arrays.copyOfRange(lockingScript, 1, lockingScript.size - 1)
                scriptType = ScriptType.P2PK
            } else if (isP2SH(lockingScript)) {
                payload = Arrays.copyOfRange(lockingScript, 2, lockingScript.size - 1)
                scriptType = ScriptType.P2SH
            } else if (isP2WPKH(lockingScript)) {
                payload = lockingScript
                scriptType = ScriptType.P2WPKH
            } else continue

            output.scriptType = scriptType
            output.keyHash = payload

            // Set public key if exist
            getPublicKey(output, realm)?.let { pubKey ->
                transaction.isMine = true
                output.publicKey = pubKey
            }
        }
    }

    private fun parseInputs(transaction: Transaction) {
        for (input in transaction.inputs) {
            if (input.previousOutput != null) {
                input.address = input.previousOutput?.address
                input.keyHash = input.previousOutput?.keyHash
                continue
            }

            val payload: ByteArray
            val scriptType: Int
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

                if (pubKeySize == 33 || pubKeySize == 65 && sigScript.size == signOffset + pubKeySize + 2) {
                    scriptType = ScriptType.P2PKH
                    payload = Arrays.copyOfRange(sigScript, signOffset + 2, sigScript.size)
                } else continue
            }
            //  P2WPKHSH 0x16 00 14 {20-byte-key-hash}
            else if (sigScript.size == 23 &&
                    sigScript[0] == 0x16.toByte() &&
                    sigScript[1] == 0.toByte() || sigScript[1] in 0x50..0x61 &&
                    sigScript[2] == 0x14.toByte()) {
                payload = sigScript
                scriptType = ScriptType.P2WPKHSH
            } else continue

            try {
                val address = addressConverter.convert(payload, scriptType)
                input.keyHash = address.hash
                input.address = address.string

            } catch (e: Exception) {
            }
        }
    }

    private fun getPublicKey(output: TransactionOutput, realm: Realm): PublicKey? {
        val query = realm.where(PublicKey::class.java)
        if (output.scriptType == ScriptType.P2WPKH) {
            query.equalTo("scriptHashP2WPKH", output.keyHash)
        } else {
            query.equalTo("publicKey", output.keyHash).or()
            query.equalTo("publicKeyHash", output.keyHash)
        }

        return query.findFirst()
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
        return (lockingScript.size == 35 ||
                lockingScript.size == 67 &&
                lockingScript[0] == 33.toByte() ||
                lockingScript[0] == 65.toByte() &&
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
                lockingScript[0] == 0.toByte() &&
                lockingScript[1] == 20.toByte())
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
}

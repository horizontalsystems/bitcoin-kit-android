package io.horizontalsystems.groestlcoinkit.transactions.builder

import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.serializers.TransactionSerializer
import io.horizontalsystems.bitcoincore.storage.InputToSign
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.bitcoincore.transactions.builder.InputSigner

import io.horizontalsystems.bitcoincore.utils.HashUtils
import io.horizontalsystems.groestlcoinkit.serializers.GroestlcoinTransactionSerializer
import io.horizontalsystems.hdwalletkit.ECDSASignature
import io.horizontalsystems.hdwalletkit.ECException
import io.horizontalsystems.hdwalletkit.ECKey
import io.horizontalsystems.hdwalletkit.HDWallet
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.params.ECPrivateKeyParameters
import org.spongycastle.crypto.signers.ECDSASigner
import org.spongycastle.crypto.signers.HMacDSAKCalculator
import java.math.BigInteger

class GroestlcoinInputSigner(val hdWallet: HDWallet, network: Network) : InputSigner(hdWallet, network) {

    override fun sigScriptData(transaction: Transaction, inputsToSign: List<InputToSign>, outputs: List<TransactionOutput>, index: Int): List<ByteArray> {

        val input = inputsToSign[index]
        val prevOutput = input.previousOutput
        val publicKey = input.previousOutputPublicKey

        val privateKey = checkNotNull(hdWallet.privateKey(publicKey.account, publicKey.index, publicKey.external)) {
            throw Error.NoPrivateKey()
        }

        val isWitness = prevOutput.scriptType in arrayOf(ScriptType.P2WPKH, ScriptType.P2WPKHSH)

        val txContent = GroestlcoinTransactionSerializer.serializeForSignature(transaction, inputsToSign, outputs, index, isWitness || network.sigHashForked) + byteArrayOf(network.sigHashValue, 0, 0, 0)
        val signature = createSignature(privateKey, txContent) + network.sigHashValue

        return when {
            prevOutput.scriptType == ScriptType.P2PK -> listOf(signature)
            else -> listOf(signature, publicKey.publicKey)
        }
    }

    @Throws(ECException::class)
    fun createSignature(privateKey: ECKey, contents: ByteArray): ByteArray {
        val privKey = privateKey.privKey
        if (privKey == null)
            throw IllegalStateException("No private key available")
        //
        // Get the double SHA-256 hash of the signed contents
        //
        val contentsHash = HashUtils.sha256(contents)
        //
        // Create the signature
        //
        val sigs: Array<BigInteger>
        try {
            val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
            val privKeyParams = ECPrivateKeyParameters(privKey, ECKey.ecParams)
            signer.init(true, privKeyParams)
            sigs = signer.generateSignature(contentsHash)
        } catch (exc: RuntimeException) {
            throw ECException("Exception while creating signature", exc)
        }

        //
        // Create a canonical signature by adjusting the S component to be less than or equal to
        // half the curve order.
        //
        if (sigs[1].compareTo(ECKey.HALF_CURVE_ORDER) > 0)
            sigs[1] = ECKey.ecParams.getN().subtract(sigs[1])
        return ECDSASignature(sigs[0], sigs[1]).encodeToDER()
    }
}

package io.horizontalsystems.dashkit.instantsend

import io.horizontalsystems.bitcoincore.utils.HSLogger
import org.dashj.bls.InsecureSignature
import org.dashj.bls.JNI
import org.dashj.bls.PublicKey

class BLS {
    private val logger = HSLogger("BLS")

    init {
        System.loadLibrary(JNI.LIBRARY_NAME)
    }

    fun verifySignature(pubKeyOperator: ByteArray, vchMasternodeSignature: ByteArray, hash: ByteArray): Boolean {
        return try {
            val pk = PublicKey.FromBytes(pubKeyOperator)
            val insecureSignature = InsecureSignature.FromBytes(vchMasternodeSignature)

            insecureSignature.Verify(hash, pk)
        } catch (e: Exception) {
            logger.e(e, "Verifying BLS signature failed with exception")

            false
        }
    }

}

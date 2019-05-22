package io.horizontalsystems.dashkit.instantsend

import org.dashj.bls.InsecureSignature
import org.dashj.bls.JNI
import org.dashj.bls.PublicKey
import java.util.logging.Logger

class BLS {
    private val logger = Logger.getLogger("BLS")

    init {
        System.loadLibrary(JNI.LIBRARY_NAME)
    }

    fun verifySignature(pubKeyOperator: ByteArray, vchMasternodeSignature: ByteArray, hash: ByteArray): Boolean {
        return try {
            val pk = PublicKey.FromBytes(pubKeyOperator)
            val insecureSignature = InsecureSignature.FromBytes(vchMasternodeSignature)

            insecureSignature.Verify(hash, pk)
        } catch (e: Exception) {
            logger.severe("Verifying BLS signature failed with exception: $e")

            false
        }
    }

}

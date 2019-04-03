package io.horizontalsystems.bitcoinkit.dash

import fr.cryptohash.*
import java.util.*

object X11Hash {

    private val algorithms = listOf(
            BLAKE512(),
            BMW512(),
            Groestl512(),
            Skein512(),
            JH512(),
            Keccak512(),
            Luffa512(),
            CubeHash512(),
            SHAvite512(),
            SIMD512(),
            ECHO512()
    )

    fun x11(input: ByteArray): ByteArray {
        var hash = input

        algorithms.forEach {
            hash = it.digest(hash)
        }

        return Arrays.copyOfRange(hash, 0, 32)
    }
}

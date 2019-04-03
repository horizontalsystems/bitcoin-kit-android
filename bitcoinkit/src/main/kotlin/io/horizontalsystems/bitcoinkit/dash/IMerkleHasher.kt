package io.horizontalsystems.bitcoinkit.dash

interface IMerkleHasher {
    fun hash(first: ByteArray, second: ByteArray) : ByteArray
}
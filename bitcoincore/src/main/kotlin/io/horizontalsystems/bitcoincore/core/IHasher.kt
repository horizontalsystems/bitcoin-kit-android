package io.horizontalsystems.bitcoincore.core

interface IHasher {
    fun hash(data: ByteArray) : ByteArray
}

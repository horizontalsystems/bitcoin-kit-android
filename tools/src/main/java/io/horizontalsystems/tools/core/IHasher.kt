package io.horizontalsystems.tools.core

interface IHasher {
    fun hash(data: ByteArray) : ByteArray
}

package io.horizontalsystems.bitcoincore.network.messages

import io.horizontalsystems.bitcoincore.exceptions.BitcoinException
import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.utils.HashUtils
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

interface IMessage

interface IMessageParser {
    val command: String
    fun parseMessage(input: BitcoinInputMarkable): IMessage
}

interface IMessageSerializer {
    val command: String
    fun serialize(message: IMessage): ByteArray?
}

class NetworkMessageParser(private val magic: Long) {
    private var messageParsers = hashMapOf<String, IMessageParser>()

    /**
     * Parse stream as message.
     */
    @Throws(IOException::class)
    fun parseMessage(input: BitcoinInput): IMessage {
        val magic = input.readUnsignedInt()
        if (magic != this.magic) {
            throw BitcoinException("Bad magic. (local) ${this.magic}!=$magic")
        }

        val command = getCommandFrom(input.readBytes(12))
        val payloadLength = input.readInt()
        val expectedChecksum = ByteArray(4)
        input.readFully(expectedChecksum)
        val payload = ByteArray(payloadLength)
        input.readFully(payload)

        // check:
        val actualChecksum = getCheckSum(payload)
        if (!expectedChecksum.contentEquals(actualChecksum)) {
            throw BitcoinException("Checksum failed.")
        }

        try {
            BitcoinInputMarkable(payload).use {
                return messageParsers[command]?.parseMessage(it) ?: UnknownMessage(command)
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun add(messageParser: IMessageParser) {
        messageParsers[messageParser.command] = messageParser
    }

    private fun getCommandFrom(cmd: ByteArray): String {
        var n = cmd.size - 1
        while (n >= 0) {
            if (cmd[n].toInt() == 0) {
                n--
            } else {
                break
            }
        }
        if (n <= 0) {
            throw BitcoinException("Bad command bytes.")
        }
        val b = cmd.copyOfRange(0, n + 1)
        return String(b, StandardCharsets.UTF_8)
    }

    private fun getCheckSum(payload: ByteArray): ByteArray {
        val hash = HashUtils.doubleSha256(payload)
        return hash.copyOfRange(0, 4)
    }
}

class NetworkMessageSerializer(private val magic: Long) {
    private var messageSerializers = mutableListOf<IMessageSerializer>()

    fun serialize(msg: IMessage): ByteArray {
        var payload: ByteArray? = null
        var serializer: IMessageSerializer? = null

        for (item in messageSerializers) {
            payload = item.serialize(msg)

            if (payload != null) {
                serializer = item
                break
            }
        }

        if (payload == null || serializer == null) {
            throw NoSerializer(msg)
        }

        return BitcoinOutput()
                .writeInt32(magic)                          // magic
                .write(getCommandBytes(serializer.command)) // command: char[12]
                .writeInt(payload.size)         // length: uint32_t
                .write(getCheckSum(payload))    // checksum: uint32_t
                .write(payload)                 // payload:
                .toByteArray()
    }

    fun add(messageSerializer: IMessageSerializer) {
        messageSerializers.add(messageSerializer)
    }

    private fun getCommandBytes(cmd: String): ByteArray {
        val cmdBytes = cmd.toByteArray()
        if (cmdBytes.isEmpty() || cmdBytes.size > 12) {
            throw IllegalArgumentException("Bad command: $cmd")
        }
        val buffer = ByteArray(12)
        System.arraycopy(cmdBytes, 0, buffer, 0, cmdBytes.size)
        return buffer
    }

    private fun getCheckSum(payload: ByteArray): ByteArray {
        val hash = HashUtils.doubleSha256(payload)
        return Arrays.copyOfRange(hash, 0, 4)
    }
}

class NoSerializer(message: IMessage) : Exception("Cannot serialize message=$message")

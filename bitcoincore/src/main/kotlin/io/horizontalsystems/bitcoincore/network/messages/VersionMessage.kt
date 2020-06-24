package io.horizontalsystems.bitcoincore.network.messages

import io.horizontalsystems.bitcoincore.io.BitcoinInputMarkable
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.models.NetworkAddress
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.utils.NetworkUtils
import java.net.InetAddress

class VersionMessage(val protocolVersion: Int, val services: Long, val timestamp: Long, val recipientAddress: NetworkAddress) : IMessage {

    lateinit var senderAddress: NetworkAddress

    // Random value to identify sending node
    var nonce = 0L

    // User-Agent as defined in <a href="https://github.com/bitcoin/bips/blob/master/bip-0014.mediawiki">BIP 14</a>.
    var subVersion = "/BitcoinKit:0.1.0/"

    // How many blocks are in the chain, according to the other side.
    var lastBlock: Int = 0

    // Whether or not to relay tx invs before a filter is received.
    // See <a href="https://github.com/bitcoin/bips/blob/master/bip-0037.mediawiki#extensions-to-existing-messages">BIP 37</a>.
    var relay = false

    constructor(bestBlock: Int, recipientAddr: InetAddress, network: Network) : this(network.protocolVersion, network.networkServices, System.currentTimeMillis() / 1000, NetworkAddress(recipientAddr, network)) {
        lastBlock = bestBlock
        senderAddress = NetworkAddress(NetworkUtils.getLocalInetAddress(), network)
        nonce = (Math.random() * java.lang.Long.MAX_VALUE).toLong() //Random node id generated at startup.
    }

    fun hasBlockChain(network: Network): Boolean {
        return (services and network.serviceFullNode) == network.serviceFullNode
    }

    // see https://github.com/bitcoin/bips/blob/master/bip-0111.mediawiki
    fun supportsBloomFilter(network: Network): Boolean {
        return when {
            protocolVersion >= network.noBloomVersion -> {
                services and network.serviceBloomFilter == network.serviceBloomFilter
            }
            else -> protocolVersion >= network.bloomFilterVersion
        }
    }

    override fun toString(): String {
        return ("VersionMessage(lastBlock=$lastBlock, protocol=$protocolVersion, services=$services, timestamp=$timestamp), userAgent=$subVersion")
    }
}

class VersionMessageParser : IMessageParser {
    override val command: String = "version"

    override fun parseMessage(input: BitcoinInputMarkable): IMessage {
        val protocolVersion = input.readInt()
        val services = input.readLong()
        val timestamp = input.readLong()
        val recipientAddress = NetworkAddress.parse(input, true)

        val versionMessage = VersionMessage(protocolVersion, services, timestamp, recipientAddress)

        if (protocolVersion >= 106) {
            versionMessage.senderAddress = NetworkAddress.parse(input, true)
            versionMessage.nonce = input.readLong()
            versionMessage.subVersion = input.readString()
            versionMessage.lastBlock = input.readInt()
            if (protocolVersion >= 70001) {
                versionMessage.relay = input.readByte().toInt() != 0
            }
        }

        return versionMessage
    }
}

class VersionMessageSerializer : IMessageSerializer {
    override val command: String = "version"

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is VersionMessage) {
            return null
        }

        val output = BitcoinOutput()
        output.writeInt(message.protocolVersion)
                .writeLong(message.services)
                .writeLong(message.timestamp)
                .write(message.recipientAddress.toByteArray(true))
        if (message.protocolVersion >= 106) {
            output.write(message.senderAddress.toByteArray(true))
                    .writeLong(message.nonce)
                    .writeString(message.subVersion)
                    .writeInt(message.lastBlock)
            if (message.protocolVersion >= 70001) {
                output.writeByte(1)
            }
        }

        return output.toByteArray()
    }
}

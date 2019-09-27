package io.horizontalsystems.groestlcoinkit

import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCoreBuilder
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelectorChain
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.network.messages.*
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup
import io.horizontalsystems.bitcoincore.serializers.BlockHeaderParser
import io.horizontalsystems.bitcoincore.transactions.*
import io.horizontalsystems.bitcoincore.transactions.builder.InputSigner
import io.horizontalsystems.bitcoincore.transactions.builder.TransactionBuilder
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptBuilder
import io.horizontalsystems.groestlcoinkit.core.SingleSha256Hasher
import io.horizontalsystems.groestlcoinkit.network.messages.GroestlcoinNetworkMessageParser
import io.horizontalsystems.groestlcoinkit.network.messages.GroestlcoinNetworkMessageSerializer
import io.horizontalsystems.groestlcoinkit.network.messages.GroestlcoinTransactionMessageParser
import io.horizontalsystems.groestlcoinkit.transactions.builder.GroestlcoinInputSigner
import io.horizontalsystems.groestlcoinkit.transactions.builder.GroestlcoinTransactionBuilder
import io.horizontalsystems.groestlcoinkit.utils.GroestlcoinBase58AddressConverter

class GroestlcoinCoreBuilder : BitcoinCoreBuilder {

    constructor() : super()

    private var network: Network? = null

    override fun setNetwork(network: Network): BitcoinCoreBuilder {
        this.network = network
        super.setNetwork(network)
        return this
    }

    override fun build(): BitcoinCore {
        val network = checkNotNull(this.network)
        val bitcoinCore : BitcoinCore = super.build()
        bitcoinCore.networkMessageSerializer = GroestlcoinNetworkMessageSerializer(network.magic)
        bitcoinCore.networkMessageParser = GroestlcoinNetworkMessageParser(network.magic)

        val transactionBuilder = GroestlcoinTransactionBuilder(ScriptBuilder(), GroestlcoinInputSigner(hdWallet, network))
        bitcoinCore.replaceTransactionBuilder(transactionBuilder)

        bitcoinCore.addMessageParser(AddrMessageParser())
                .addMessageParser(MerkleBlockMessageParser(BlockHeaderParser(GroestlHasher())))
                .addMessageParser(InvMessageParser())
                .addMessageParser(GetDataMessageParser())
                .addMessageParser(PingMessageParser())
                .addMessageParser(PongMessageParser())
                .addMessageParser(GroestlcoinTransactionMessageParser())
                .addMessageParser(VerAckMessageParser())
                .addMessageParser(VersionMessageParser())
                .addMessageParser(RejectMessageParser())

        bitcoinCore.addMessageSerializer(FilterLoadMessageSerializer())
                .addMessageSerializer(GetBlocksMessageSerializer())
                .addMessageSerializer(InvMessageSerializer())
                .addMessageSerializer(GetDataMessageSerializer())
                .addMessageSerializer(MempoolMessageSerializer())
                .addMessageSerializer(PingMessageSerializer())
                .addMessageSerializer(PongMessageSerializer())
                .addMessageSerializer(io.horizontalsystems.groestlcoinkit.network.messages.GroestlcoinTransactionMessageSerializer())
                .addMessageSerializer(VerAckMessageSerializer())
                .addMessageSerializer(VersionMessageSerializer())

        bitcoinCore.peerGroup.setNetworkMessageSerializer(bitcoinCore.networkMessageSerializer)
        bitcoinCore.peerGroup.setNetworkMessageParser(bitcoinCore.networkMessageParser)

        bitcoinCore.prependAddressConverter(GroestlcoinBase58AddressConverter(network.addressVersion, network.addressScriptVersion))

        return bitcoinCore
    }
}

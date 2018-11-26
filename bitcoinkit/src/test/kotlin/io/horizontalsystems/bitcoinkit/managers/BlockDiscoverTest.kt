package io.horizontalsystems.bitcoinkit.managers

import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.models.Block
import io.horizontalsystems.bitcoinkit.network.Network
import io.horizontalsystems.hdwalletkit.HDPublicKey
import io.horizontalsystems.hdwalletkit.HDWallet
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class BlockDiscoverTest {

    lateinit var blockDiscover: BlockDiscover

    private val hdWallet = mock(HDWallet::class.java)
    private val apiManager = mock(IApiManager::class.java)
    private val network = mock(Network::class.java)
    private val checkpointBlock = mock(Block::class.java)

    @Before
    fun setup() {
        whenever(hdWallet.gapLimit).thenReturn(1)

        whenever(network.checkpointBlock).thenReturn(checkpointBlock)
        whenever(checkpointBlock.height).thenReturn(1000)

        blockDiscover = BlockDiscover(hdWallet, apiManager, network)
    }

    @Test
    fun fetchFromApi_true() {
        val externalPublicKey1 = HDPublicKey().apply {
            index = 0; external = true; publicKey = byteArrayOf(1); publicKeyHash = "a0".hexStringToByteArray();
        }
        val externalPublicKey2 = HDPublicKey().apply {
            index = 1; external = true; publicKey = byteArrayOf(2); publicKeyHash = "a1".hexStringToByteArray()
        }
        val externalPublicKey3 = HDPublicKey().apply {
            index = 2; external = true; publicKey = byteArrayOf(3); publicKeyHash = "a2".hexStringToByteArray()
        }

        val blockHashes1 = listOf(BlockResponse("000001", 1))
        val blockHashes2 = listOf(BlockResponse("000002", 2))
        val blockHashes3 = listOf<BlockResponse>()

        whenever(hdWallet.hdPublicKey(0, true)).thenReturn(externalPublicKey1)
        whenever(hdWallet.hdPublicKey(1, true)).thenReturn(externalPublicKey2)
        whenever(hdWallet.hdPublicKey(2, true)).thenReturn(externalPublicKey3)

        whenever(apiManager.getBlockHashes(argThat { publicKeyHex == "a0" })).thenReturn(Single.just(blockHashes1))
        whenever(apiManager.getBlockHashes(argThat { publicKeyHex == "a1" })).thenReturn(Single.just(blockHashes2))
        whenever(apiManager.getBlockHashes(argThat { publicKeyHex == "a2" })).thenReturn(Single.just(blockHashes3))

        blockDiscover.fetchFromApi(true)
                .test()
                .assertValue {
                    val keys = it.first

                    keys.any { key -> key.publicKey.contentEquals(byteArrayOf(1)) }
                            && keys.any { key -> key.publicKey.contentEquals(byteArrayOf(2)) }
                            && keys.any { key -> key.publicKey.contentEquals(byteArrayOf(3)) }
                }
                .assertValue {
                    val blocks = it.second

                    blocks.any { block -> block.height == 1 } && blocks.any { block -> block.height == 2 }
                }

    }
}

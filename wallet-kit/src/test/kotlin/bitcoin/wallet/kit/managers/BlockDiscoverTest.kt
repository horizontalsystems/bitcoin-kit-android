package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.hdwallet.HDWallet
import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.network.NetworkParameters
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class BlockDiscoverTest {

    lateinit var blockDiscover: BlockDiscover

    private val hdWallet = mock(HDWallet::class.java)
    private val apiManager = mock(ApiManager::class.java)
    private val network = mock(NetworkParameters::class.java)
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
        val externalPublicKey1 = mock(PublicKey::class.java)
        val externalPublicKey2 = mock(PublicKey::class.java)
        val externalPublicKey3 = mock(PublicKey::class.java)
        val externalPublicKey1Address = "externalPublicKey1Address"
        val externalPublicKey2Address = "externalPublicKey2Address"
        val externalPublicKey3Address = "externalPublicKey3Address"

        val blockHashes1 = listOf(BlockResponse("000001", 1))
        val blockHashes2 = listOf(BlockResponse("000002", 2))
        val blockHashes3 = listOf<BlockResponse>()

        whenever(hdWallet.publicKey(0, true)).thenReturn(externalPublicKey1)
        whenever(hdWallet.publicKey(1, true)).thenReturn(externalPublicKey2)
        whenever(hdWallet.publicKey(2, true)).thenReturn(externalPublicKey3)

        whenever(externalPublicKey1.address).thenReturn(externalPublicKey1Address)
        whenever(externalPublicKey2.address).thenReturn(externalPublicKey2Address)
        whenever(externalPublicKey3.address).thenReturn(externalPublicKey3Address)

        whenever(apiManager.getBlockHashes(externalPublicKey1Address)).thenReturn(Observable.just(blockHashes1))
        whenever(apiManager.getBlockHashes(externalPublicKey2Address)).thenReturn(Observable.just(blockHashes2))
        whenever(apiManager.getBlockHashes(externalPublicKey3Address)).thenReturn(Observable.just(blockHashes3))

        blockDiscover.fetchFromApi(true)
                .test()
                .assertValue {
                    val keys = it.first

                    keys.contains(externalPublicKey1)
                            && keys.contains(externalPublicKey2)
                            && keys.contains(externalPublicKey3)
                }
                .assertValue {
                    val blocks = it.second

                    blocks.any { it.height == 1 } && blocks.any { it.height == 2 }
                }

    }
}
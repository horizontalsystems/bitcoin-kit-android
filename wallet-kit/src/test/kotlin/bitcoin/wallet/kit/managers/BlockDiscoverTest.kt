package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.core.hexStringToByteArray
import bitcoin.wallet.kit.hdwallet.Address
import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.wallet.kit.utils.AddressConverter
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.hdwalletkit.HDPublicKey
import io.horizontalsystems.hdwalletkit.HDWallet
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
    private val addressConverter = mock(AddressConverter::class.java)

    @Before
    fun setup() {
        whenever(hdWallet.gapLimit).thenReturn(1)

        whenever(network.checkpointBlock).thenReturn(checkpointBlock)
        whenever(checkpointBlock.height).thenReturn(1000)

        blockDiscover = BlockDiscover(hdWallet, apiManager, network, addressConverter)
    }

    @Test
    fun fetchFromApi_true() {
        val externalPublicKey1 = HDPublicKey().apply {
            index = 0; external = true; publicKey = byteArrayOf(1); publicKeyHash = "a0".hexStringToByteArray()
        }
        val externalPublicKey2 = HDPublicKey().apply {
            index = 1; external = true; publicKey = byteArrayOf(2); publicKeyHash = "a1".hexStringToByteArray()
        }
        val externalPublicKey3 = HDPublicKey().apply {
            index = 2; external = true; publicKey = byteArrayOf(3); publicKeyHash = "a2".hexStringToByteArray()
        }

        val address1 = mock(Address::class.java)
        val address2 = mock(Address::class.java)
        val address3 = mock(Address::class.java)

        val externalPublicKey1Address = "externalPublicKey1Address"
        val externalPublicKey2Address = "externalPublicKey2Address"
        val externalPublicKey3Address = "externalPublicKey3Address"

        val blockHashes1 = listOf(BlockResponse("000001", 1))
        val blockHashes2 = listOf(BlockResponse("000002", 2))
        val blockHashes3 = listOf<BlockResponse>()

        whenever(hdWallet.hdPublicKey(0, true)).thenReturn(externalPublicKey1)
        whenever(hdWallet.hdPublicKey(1, true)).thenReturn(externalPublicKey2)
        whenever(hdWallet.hdPublicKey(2, true)).thenReturn(externalPublicKey3)

        whenever(addressConverter.convert(byteArrayOf(1))).thenReturn(address1)
        whenever(addressConverter.convert(byteArrayOf(2))).thenReturn(address2)
        whenever(addressConverter.convert(byteArrayOf(3))).thenReturn(address3)

        whenever(address1.toString()).thenReturn(externalPublicKey1Address)
        whenever(address2.toString()).thenReturn(externalPublicKey2Address)
        whenever(address3.toString()).thenReturn(externalPublicKey3Address)

        whenever(apiManager.getBlockHashes(externalPublicKey1Address)).thenReturn(Observable.just(blockHashes1))
        whenever(apiManager.getBlockHashes(externalPublicKey2Address)).thenReturn(Observable.just(blockHashes2))
        whenever(apiManager.getBlockHashes(externalPublicKey3Address)).thenReturn(Observable.just(blockHashes3))

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
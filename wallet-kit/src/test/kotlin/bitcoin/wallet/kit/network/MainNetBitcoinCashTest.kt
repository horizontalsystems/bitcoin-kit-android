package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.blocks.validators.BitcoinCashValidator
import com.nhaarman.mockito_kotlin.any
import helpers.Fixtures
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(MainNetBitcoinCash::class)

class MainNetBitcoinCashTest {
    private val blockValidator = mock(BitcoinCashValidator::class.java)

    lateinit var network: MainNetBitcoinCash

    @Before
    fun setup() {
        PowerMockito
                .whenNew(BitcoinCashValidator::class.java)
                .withAnyArguments()
                .thenReturn(blockValidator)

        network = MainNetBitcoinCash()
    }

    @Test
    fun validate() {
        val block1 = Fixtures.checkpointBlock2
        val blockPrev = block1.previousBlock!!

        network.validateBlock(block1, blockPrev)

        verify(blockValidator).validate(any(), any())
    }

}

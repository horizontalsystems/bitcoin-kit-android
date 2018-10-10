package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.blocks.validators.TestnetValidator
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import helpers.Fixtures
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(TestNet::class)

class TestNetTest {

    private val validator = Mockito.mock(TestnetValidator::class.java)
    private lateinit var network: TestNet

    @Before
    fun setup() {
        PowerMockito
                .whenNew(TestnetValidator::class.java)
                .withAnyArguments()
                .thenReturn(validator)

        network = TestNet()
    }

    @Test
    fun validate_headers() {
        val block1 = Fixtures.checkpointBlock2
        val blockPrev = block1.previousBlock!!

        network.validate(block1, blockPrev)

        verify(validator).validateBits(any(), any())
    }

    @Test
    fun validate_bits() {
        val block1 = Fixtures.checkpointBlock2
        val blockPrev = block1.previousBlock!!

        network.validate(block1, blockPrev)

        verify(validator).validateHeader(any(), any())
    }

    @Test
    fun validate_difficultyTransitions() {
        val block1 = Fixtures.checkpointBlock2
        val blockPrev = block1.previousBlock!!

        whenever(validator.isDifficultyTransitionEdge(block1.height)).thenReturn(true)

        network.validate(block1, blockPrev)

        verify(validator).checkDifficultyTransitions(any())
    }
}

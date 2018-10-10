package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.blocks.validators.MainnetValidator
import bitcoin.walllet.kit.io.BitcoinInput
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import helpers.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(MainNet::class)

class MainNetTest {

    private val validator = mock(MainnetValidator::class.java)
    private lateinit var network: MainNet

    @Before
    fun setup() {
        PowerMockito
                .whenNew(MainnetValidator::class.java)
                .withAnyArguments()
                .thenReturn(validator)

        network = MainNet()
    }

    @Test
    fun packetMagic() {
        val stream = BitcoinInput(byteArrayOf(
                0xf9.toByte(),
                0xbe.toByte(),
                0xb4.toByte(),
                0xd9.toByte()
        ))

        val magic = stream.readUnsignedInt()
        assertEquals(magic, network.magic)
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

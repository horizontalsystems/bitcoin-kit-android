package io.horizontalsystems.bitcoinkit.network

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import helpers.Fixtures
import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidator
import io.horizontalsystems.bitcoinkit.io.BitcoinInput
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

    private val validator = mock(BlockValidator::class.java)
    private lateinit var network: MainNet

    @Before
    fun setup() {
        PowerMockito
                .whenNew(BlockValidator::class.java)
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
    fun validateBlock() {
        val block1 = Fixtures.checkpointBlock2
        val blockPrev = block1.previousBlock!!

        network.validateBlock(block1, blockPrev)

        verify(validator).validate(any(), any())
    }

}

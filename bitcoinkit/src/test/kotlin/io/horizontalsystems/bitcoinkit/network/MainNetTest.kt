package io.horizontalsystems.bitcoinkit.network

import com.nhaarman.mockito_kotlin.verify
import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidator
import io.horizontalsystems.bitcoinkit.core.IStorage
import io.horizontalsystems.bitcoinkit.io.BitcoinInput
import io.horizontalsystems.bitcoinkit.models.Block
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

    private val block1 = mock(Block::class.java)
    private val block2 = mock(Block::class.java)
    private val storage = mock(IStorage::class.java)
    private val validator = mock(BlockValidator::class.java)

    private lateinit var network: MainNet

    @Before
    fun setup() {
        PowerMockito
                .whenNew(BlockValidator::class.java)
                .withAnyArguments()
                .thenReturn(validator)

        network = MainNet(storage)
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
        network.validateBlock(block1, block2)

        verify(validator).validate(block1, block2)
    }

}

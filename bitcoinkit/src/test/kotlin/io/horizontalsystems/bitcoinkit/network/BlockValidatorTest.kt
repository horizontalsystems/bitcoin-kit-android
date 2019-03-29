//package io.horizontalsystems.bitcoinkit.network
//
//import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidator
//import io.horizontalsystems.bitcoinkit.blocks.validators.BlockValidatorException
//import io.horizontalsystems.bitcoinkit.core.IStorage
//import io.horizontalsystems.bitcoinkit.crypto.CompactBits
//import io.horizontalsystems.bitcoinkit.models.Block
//import io.horizontalsystems.bitcoinkit.models.Header
//import io.horizontalsystems.bitcoinkit.utils.HashUtils
//import org.junit.Assert.*
//import org.junit.Before
//import org.junit.Test
//import org.mockito.Mockito.mock
//import java.math.BigInteger
//
//class BlockValidatorTest {
//    private val storage = mock(IStorage::class.java)
//    private val network = MainNet(storage)
//    private lateinit var validator: BlockValidator
//
//    @Before
//    fun setup() {
//        validator = BlockValidator(network, storage)
//    }
//
//    @Test
//    fun validateHeader() {
//        val block1 = Block(Fixtures.block1.header!!, 1)
//        val block2 = Block(Fixtures.block2.header!!, block1)
//
//        try {
//            validator.validateHeader(block2, block1)
//        } catch (e: BlockValidatorException) {
//            fail("Header validation failed with: ${e.message}")
//        }
//    }
//
//    @Test(expected = BlockValidatorException.InvalidProveOfWork::class)
//    fun validateHeader_invalidPOW() {
//        val block1 = Block(Fixtures.block1.header!!, 1)
//        val block2 = Block(Fixtures.block2.header!!, block1)
//
//        block2.header?.bits = CompactBits.encode(BigInteger(block2.headerHashReversedHex, 16).minus(BigInteger.valueOf(1L)))
//
//        validator.validateHeader(block2, block1)
//    }
//
//
//    @Test
//    fun validateHeader_invalidHeader() {
//        try {
//            validator.validateHeader(Block(), Block())
//            fail("Expected exception: NoHeader")
//        } catch (e: BlockValidatorException.NoHeader) {
//        }
//
//        val block1 = Block().apply { }
//        val block2 = Block(Fixtures.block2.header!!, block1)
//
//        try {
//            validator.validateHeader(block2, block1)
//            fail("Expected exception: WrongPreviousHeader")
//        } catch (e: BlockValidatorException.WrongPreviousHeader) {
//        }
//    }
//
//    @Test
//    fun validateBits() {
//        val block1 = Block(Fixtures.block1.header!!, 1)
//        val block2 = Block(Fixtures.block2.header!!, block1)
//
//        try {
//            validator.validateBits(block2, block1)
//        } catch (e: BlockValidatorException) {
//            fail("Bits validation failed with: ${e.message}")
//        }
//    }
//
//    @Test
//    fun validateHeader_invalidBits() {
//        try {
//            validator.validateBits(Block(), Block())
//            fail("Expected exception: NoHeader")
//        } catch (e: BlockValidatorException.NoHeader) {
//        }
//
//        val block1 = Block(Header().apply { bits = 1 }, 1)
//        val block2 = Block(Fixtures.block2.header!!, Block())
//
//        try {
//            validator.validateBits(block2, block1)
//            fail("Expected exception: NotEqualBits")
//        } catch (e: BlockValidatorException.NotEqualBits) {
//
//        }
//    }
//
//    @Test
//    fun checkDifficultyTransitions() {
//        val check1 = Fixtures.checkpointBlock1
//
//        var checkPrev = check1
//        val prevsHead = Header().apply {
//            version = 536870912
//            prevHash = HashUtils.toBytesAsLE("000000000000000000124a73e879fd66a1b29d1b4b3f1a81de3cbcbe579e21a8")
//            merkleHash = HashUtils.toBytesAsLE("7904930640df999005df3b57f9c6f542088af33c3d773dcec2939f55ced359b8")
//            timestamp = 1535129301
//            bits = 388763047
//            nonce = 59591417
//        }
//
//        for (i in 1 until 2016) {
//            checkPrev = Block(prevsHead, checkPrev)
//        }
//
//        val check2Head = Header().apply {
//            version = 536870912
//            prevHash = HashUtils.toBytesAsLE("0000000000000000001d9d48d93793aaa85b5f6d17c176d4ef905c7e7112b1cf")
//            merkleHash = HashUtils.toBytesAsLE("3ad0fa0e8c100db5831ebea7cabf6addae2c372e6e1d84f6243555df5bbfa351")
//            timestamp = 1535129431
//            bits = 388618029
//            nonce = 2367954839
//        }
//
//        val check2 = Block(check2Head, checkPrev)
//
//        try {
//            validator.checkDifficultyTransitions(check2)
//        } catch (e: Exception) {
//            fail(e.message)
//        }
//    }
//
//    @Test
//    fun getPrevious() {
//        val block = Fixtures.block2
//
//        assertEquals(validator.getPrevious(block, 1), block.previousBlock)
//        assertEquals(validator.getPrevious(block, 4), null)
//    }
//
//    @Test
//    fun getPreviousWindow() {
//        val block3 = Fixtures.block3
//        val block2 = block3.previousBlock
//        val block1 = block2?.previousBlock
//
//        val previousWindow = validator.getPreviousWindow(block3, 2)
//        assertArrayEquals(
//                arrayOf(block1, block2),
//                previousWindow
//        )
//
//        assertEquals(null, validator.getPreviousWindow(block3, 3))
//    }
//
//    @Test
//    fun difficultyTransitionPoint() {
//        assertEquals(validator.isDifficultyTransitionEdge(0), true)
//        assertEquals(validator.isDifficultyTransitionEdge(2015), false)
//        assertEquals(validator.isDifficultyTransitionEdge(2016), true)
//        assertEquals(validator.isDifficultyTransitionEdge(4032), true)
//    }
//}

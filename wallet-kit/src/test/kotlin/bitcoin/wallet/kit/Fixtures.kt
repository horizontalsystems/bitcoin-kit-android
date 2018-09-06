package bitcoin.wallet.kit

import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.Header
import bitcoin.walllet.kit.utils.HashUtils

object Fixtures {

    var checkpointBlock1 = Block().apply {
        height = 0 // 536256
        header = Header().apply {
            version = 1
            prevHash = HashUtils.toBytesAsLittleEndian("00000000000000000000943de85f4495f053ff55f27d135edc61c27990c2eec5")
            merkleHash = HashUtils.toBytesAsLittleEndian("167bf70981d49388d07881b1a448ff9b79cf2a32716e45c535345823d8cdd541")
            timestamp = 1533980459
            bits = 388763047
            nonce = 1545867530
        }
    }

    var block3 = Block().apply {
        height = 2015 // 538271
        header = Header().apply {
            version = 1
            prevHash = HashUtils.toBytesAsLittleEndian("000000000000000000124a73e879fd66a1b29d1b4b3f1a81de3cbcbe579e21a8")
            merkleHash = HashUtils.toBytesAsLittleEndian("7904930640df999005df3b57f9c6f542088af33c3d773dcec2939f55ced359b8")
            timestamp = 1535129301
            bits = 388763047
            nonce = 59591417
        }
    }

    var checkpointBlock2 = Block().apply {
        previousBlock = block3
        height = 2016 // 538272
        header = Header().apply {
            version = 1
            prevHash = HashUtils.toBytesAsLittleEndian("0000000000000000001d9d48d93793aaa85b5f6d17c176d4ef905c7e7112b1cf")
            merkleHash = HashUtils.toBytesAsLittleEndian("3ad0fa0e8c100db5831ebea7cabf6addae2c372e6e1d84f6243555df5bbfa351")
            timestamp = 1535129431
            bits = 388618029
            nonce = 2367954839
        }
    }

}

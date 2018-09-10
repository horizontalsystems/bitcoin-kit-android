package bitcoin.wallet.kit

import bitcoin.wallet.kit.models.Block
import bitcoin.wallet.kit.models.Header
import bitcoin.walllet.kit.utils.HashUtils

object Fixtures {

    var checkpointBlock1 = Block().apply {
        height = 0 // 536256
        header = Header().apply {
            version = 536870912
            prevHash = HashUtils.toBytesAsLittleEndian("00000000000000000000943de85f4495f053ff55f27d135edc61c27990c2eec5")
            merkleHash = HashUtils.toBytesAsLittleEndian("167bf70981d49388d07881b1a448ff9b79cf2a32716e45c535345823d8cdd541")
            timestamp = 1533980459
            bits = 388763047
            nonce = 1545867530
        }
    }

    var block1 = Block().apply {
        height = 2013 // 538269
        header = Header().apply {
            version = 536870912
            prevHash = HashUtils.toBytesAsLittleEndian("00000000000000000011206e641083b68ffc41b7fe6ee1af4a5d69995d1b2d0e")
            merkleHash = HashUtils.toBytesAsLittleEndian("5510c0c3d1fd9d2b56a34aab98c29860015caf248fa62a1907b197ddec17c788")
            timestamp = 1535128609
            bits = 388763047
            nonce = 2295801359
        }
    }

    var block2 = Block().apply {
        height = 2014 // 538270
        header = Header().apply {
            version = 536870912
            prevHash = HashUtils.toBytesAsLittleEndian("0000000000000000000a876dbca5804f792afa90b6dc7946dedb5866245d0c55")
            merkleHash = HashUtils.toBytesAsLittleEndian("ccf2737e44e435e2e11481755b00d161815a24e605d605a17bf20da49320ad7d")
            timestamp = 1535128839
            bits = 388763047
            nonce = 3401296263
        }
        previousBlock = block1
    }

    var block3 = Block().apply {
        height = 2015 // 538271
        header = Header().apply {
            version = 536870912
            prevHash = HashUtils.toBytesAsLittleEndian("000000000000000000124a73e879fd66a1b29d1b4b3f1a81de3cbcbe579e21a8")
            merkleHash = HashUtils.toBytesAsLittleEndian("7904930640df999005df3b57f9c6f542088af33c3d773dcec2939f55ced359b8")
            timestamp = 1535129301
            bits = 388763047
            nonce = 59591417
        }
        previousBlock = block2
    }

    var checkpointBlock2 = Block().apply {
        previousBlock = block3
        height = 2016 // 538272
        header = Header().apply {
            version = 536870912
            prevHash = HashUtils.toBytesAsLittleEndian("0000000000000000001d9d48d93793aaa85b5f6d17c176d4ef905c7e7112b1cf")
            merkleHash = HashUtils.toBytesAsLittleEndian("3ad0fa0e8c100db5831ebea7cabf6addae2c372e6e1d84f6243555df5bbfa351")
            timestamp = 1535129431
            bits = 388618029
            nonce = 2367954839
        }
    }

}

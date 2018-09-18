package bitcoin.wallet.kit

import bitcoin.wallet.kit.core.hexStringToByteArray
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.models.TransactionInput
import bitcoin.wallet.kit.models.TransactionOutput
import bitcoin.wallet.kit.scripts.ScriptType

object TestData {
    // P2PKH: TestNet tx => 68f297d8a8c9af30cd5a9d6d1eeec5ed3df7be1e4b62f2ced135af6ffe7814c2
    val transactionP2PKH = Transaction().apply {
        version = 1
        lockTime = 0
        inputs.add(TransactionInput().apply {
            previousOutputTxHash = "093f5f5c5e57ae2ae9728147547e183e2ef5c9e6e879a78bee6ceb59db2b4797".hexStringToByteArray()
            previousOutputIndex = 1
            sigScript = "473044022018f03676d057a3cb350d9778697ff61da47b813c82fe9fb0f2ea87b231fb865b02200706f5cbbc5ebae6f7bd77e346767bce11c8476aea607671d7321e86a3186ec1012102ce0ef85579f055e2184c935e75e71458db8c4b759cd455b0aa5d91761794eef0".hexStringToByteArray()
            sequence = 4294967295
        })

        outputs.add(TransactionOutput().apply {
            value = 94734191
            index = 0
            lockingScript = "76a91437a9bfe84d9e4883ace248509bbf14c9d72af01788ac".hexStringToByteArray()
            scriptType = ScriptType.UNKNOWN
        })

        outputs.add(TransactionOutput().apply {
            value = 100000
            index = 1
            lockingScript = "76a91437a9bfe84d9e4883ace248509bbf14c9d72af01788ac".hexStringToByteArray()
            scriptType = ScriptType.UNKNOWN
        })
    }

    // P2SH: TestNet tx => 15414ebf101d18652ddc63f549da7f992009ef2fb0bba8b24c7a5bf89ad36d5e
    val transactionP2SH = Transaction().apply {
        version = 1
        lockTime = 0
        inputs.add(TransactionInput().apply {
            previousOutputTxHash = "5fd757818fa9219af9214d7489a37a046a0420db2ae0cbd6cc01b3cceba353d0".hexStringToByteArray()
            previousOutputIndex = 1
            sigScript = "160014a6c37c1214070be70236a6e0175135625de64377".hexStringToByteArray()
            sequence = 4294967294
        })

        outputs.add(TransactionOutput().apply {
            value = 617795422
            index = 0
            lockingScript = "a91414020b83b8c2e50152259f3fe5ccfe206c7842d787".hexStringToByteArray()
            scriptType = ScriptType.UNKNOWN
        })

        outputs.add(TransactionOutput().apply {
            value = 1407000
            index = 1
            lockingScript = "a91474cc925ea13e9e2e323055afa5e59f715f62036e87".hexStringToByteArray()
            scriptType = ScriptType.UNKNOWN
        })
    }

    // P2PK: TestNet tx => a6d1ce683f38a84cfd88a9d48b0ba2d7a8def00f8517e3da02c86fce6c7863d7
    val transactionP2PK = Transaction().apply {
        version = 1
        lockTime = 0
        inputs.add(TransactionInput().apply {
            previousOutputTxHash = "0437cd7f8525ceed2324359c2d0ba26006d92d856a9c20fa0241106ee5a597c9".hexStringToByteArray()
            previousOutputIndex = 0
            sigScript = "47304402204e45e16932b8af514961a1d3a1a25fdf3f4f7732e9d624c6c61548ab5fb8cd410220181522ec8eca07de4860a4acdd12909d831cc56cbbac4622082221a8768d1d0901".hexStringToByteArray()
            sequence = 4294967295
        })

        outputs.add(TransactionOutput().apply {
            value = 1000000000
            index = 0
            lockingScript = "4104ae1a62fe09c5f51b13905f07f06b99a2f7159b2225f374cd378d71302fa28414e7aab37397f554a7df5f142c21c1b7303b8a0626f1baded5c72a704f7e6cd84cac".hexStringToByteArray()
            scriptType = ScriptType.UNKNOWN
        })

        outputs.add(TransactionOutput().apply {
            value = 4000000000
            index = 1
            lockingScript = "410411db93e1dcdb8a016b49840f8c53bc1eb68a382e97b1482ecad7b148a6909a5cb2e0eaddfb84ccf9744464f82e160bfa9b8b64f9d4c03f999b8643f656b412a3ac".hexStringToByteArray()
            scriptType = ScriptType.UNKNOWN
        })
    }

}

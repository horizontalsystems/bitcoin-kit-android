package io.horizontalsystems.bitcoincore.transactions.scripts

import io.horizontalsystems.bitcoincore.utils.Utils

// push value
const val OP_0 = 0x00 // push empty vector
const val OP_FALSE = OP_0
const val OP_PUSHDATA1 = 0x4c
const val OP_PUSHDATA2 = 0x4d
const val OP_PUSHDATA4 = 0x4e
const val OP_1NEGATE = 0x4f
const val OP_RESERVED = 0x50
const val OP_1 = 0x51
const val OP_TRUE = OP_1
const val OP_2 = 0x52
const val OP_3 = 0x53
const val OP_4 = 0x54
const val OP_5 = 0x55
const val OP_6 = 0x56
const val OP_7 = 0x57
const val OP_8 = 0x58
const val OP_9 = 0x59
const val OP_10 = 0x5a
const val OP_11 = 0x5b
const val OP_12 = 0x5c
const val OP_13 = 0x5d
const val OP_14 = 0x5e
const val OP_15 = 0x5f
const val OP_16 = 0x60

// control
const val OP_NOP = 0x61
const val OP_VER = 0x62
const val OP_IF = 0x63
const val OP_NOTIF = 0x64
const val OP_VERIF = 0x65
const val OP_VERNOTIF = 0x66
const val OP_ELSE = 0x67
const val OP_ENDIF = 0x68
const val OP_VERIFY = 0x69
const val OP_RETURN = 0x6a

// stack ops
const val OP_TOALTSTACK = 0x6b
const val OP_FROMALTSTACK = 0x6c
const val OP_2DROP = 0x6d
const val OP_2DUP = 0x6e
const val OP_3DUP = 0x6f
const val OP_2OVER = 0x70
const val OP_2ROT = 0x71
const val OP_2SWAP = 0x72
const val OP_IFDUP = 0x73
const val OP_DEPTH = 0x74
const val OP_DROP = 0x75
const val OP_DUP = 0x76
const val OP_NIP = 0x77
const val OP_OVER = 0x78
const val OP_PICK = 0x79
const val OP_ROLL = 0x7a
const val OP_ROT = 0x7b
const val OP_SWAP = 0x7c
const val OP_TUCK = 0x7d

// splice ops
const val OP_CAT = 0x7e
const val OP_SUBSTR = 0x7f
const val OP_LEFT = 0x80
const val OP_RIGHT = 0x81
const val OP_SIZE = 0x82

// bit logic
const val OP_INVERT = 0x83
const val OP_AND = 0x84
const val OP_OR = 0x85
const val OP_XOR = 0x86
const val OP_EQUAL = 0x87
const val OP_EQUALVERIFY = 0x88
const val OP_RESERVED1 = 0x89
const val OP_RESERVED2 = 0x8a

// numeric
const val OP_1ADD = 0x8b
const val OP_1SUB = 0x8c
const val OP_2MUL = 0x8d
const val OP_2DIV = 0x8e
const val OP_NEGATE = 0x8f
const val OP_ABS = 0x90
const val OP_NOT = 0x91
const val OP_0NOTEQUAL = 0x92
const val OP_ADD = 0x93
const val OP_SUB = 0x94
const val OP_MUL = 0x95
const val OP_DIV = 0x96
const val OP_MOD = 0x97
const val OP_LSHIFT = 0x98
const val OP_RSHIFT = 0x99
const val OP_BOOLAND = 0x9a
const val OP_BOOLOR = 0x9b
const val OP_NUMEQUAL = 0x9c
const val OP_NUMEQUALVERIFY = 0x9d
const val OP_NUMNOTEQUAL = 0x9e
const val OP_LESSTHAN = 0x9f
const val OP_GREATERTHAN = 0xa0
const val OP_LESSTHANOREQUAL = 0xa1
const val OP_GREATERTHANOREQUAL = 0xa2
const val OP_MIN = 0xa3
const val OP_MAX = 0xa4
const val OP_WITHIN = 0xa5

// crypto
const val OP_RIPEMD160 = 0xa6
const val OP_SHA1 = 0xa7
const val OP_SHA256 = 0xa8
const val OP_HASH160 = 0xa9
const val OP_HASH256 = 0xaa
const val OP_CODESEPARATOR = 0xab
const val OP_CHECKSIG = 0xac
const val OP_CHECKSIGVERIFY = 0xad
const val OP_CHECKMULTISIG = 0xae
const val OP_CHECKMULTISIGVERIFY = 0xaf

// block state
/** Check lock time of the block. Introduced in BIP 65, replacing OP_NOP2  */
const val OP_CHECKLOCKTIMEVERIFY = 0xb1
const val OP_CHECKSEQUENCEVERIFY = 0xb2

// expansion
const val OP_NOP1 = 0xb0
/** Deprecated by BIP 65  */
@Deprecated("")
const val OP_NOP2 = OP_CHECKLOCKTIMEVERIFY
/** Deprecated by BIP 112  */
@Deprecated("")
const val OP_NOP3 = OP_CHECKSEQUENCEVERIFY
const val OP_NOP4 = 0xb3
const val OP_NOP5 = 0xb4
const val OP_NOP6 = 0xb5
const val OP_NOP7 = 0xb6
const val OP_NOP8 = 0xb7
const val OP_NOP9 = 0xb8
const val OP_NOP10 = 0xb9
const val OP_INVALIDOPCODE = 0xff

/** Sighash Types */
object Sighash {
    const val ALL: Byte = 1              // Sign all outputs
    const val NONE = 2             // Do not sign outputs (zero sequences)
    const val SINGLE = 3           // Sign output at the same index (zero sequences)
    const val FORKID: Byte = 0x40        // Bitcoin Cash SIGHASH_FORKID
    const val ANYONECANPAY = 0x80  // Sign only the current input (mask)
}

object OpCodes {

    private val opCodeMap = hashMapOf(
            OP_0 to "0",
            OP_PUSHDATA1 to "PUSHDATA1",
            OP_PUSHDATA2 to "PUSHDATA2",
            OP_PUSHDATA4 to "PUSHDATA4",
            OP_1NEGATE to "1NEGATE",
            OP_RESERVED to "RESERVED",
            OP_1 to "1",
            OP_2 to "2",
            OP_3 to "3",
            OP_4 to "4",
            OP_5 to "5",
            OP_6 to "6",
            OP_7 to "7",
            OP_8 to "8",
            OP_9 to "9",
            OP_10 to "10",
            OP_11 to "11",
            OP_12 to "12",
            OP_13 to "13",
            OP_14 to "14",
            OP_15 to "15",
            OP_16 to "16",
            OP_NOP to "NOP",
            OP_VER to "VER",
            OP_IF to "IF",
            OP_NOTIF to "NOTIF",
            OP_VERIF to "VERIF",
            OP_VERNOTIF to "VERNOTIF",
            OP_ELSE to "ELSE",
            OP_ENDIF to "ENDIF",
            OP_VERIFY to "VERIFY",
            OP_RETURN to "RETURN",
            OP_TOALTSTACK to "TOALTSTACK",
            OP_FROMALTSTACK to "FROMALTSTACK",
            OP_2DROP to "2DROP",
            OP_2DUP to "2DUP",
            OP_3DUP to "3DUP",
            OP_2OVER to "2OVER",
            OP_2ROT to "2ROT",
            OP_2SWAP to "2SWAP",
            OP_IFDUP to "IFDUP",
            OP_DEPTH to "DEPTH",
            OP_DROP to "DROP",
            OP_DUP to "DUP",
            OP_NIP to "NIP",
            OP_OVER to "OVER",
            OP_PICK to "PICK",
            OP_ROLL to "ROLL",
            OP_ROT to "ROT",
            OP_SWAP to "SWAP",
            OP_TUCK to "TUCK",
            OP_CAT to "CAT",
            OP_SUBSTR to "SUBSTR",
            OP_LEFT to "LEFT",
            OP_RIGHT to "RIGHT",
            OP_SIZE to "SIZE",
            OP_INVERT to "INVERT",
            OP_AND to "AND",
            OP_OR to "OR",
            OP_XOR to "XOR",
            OP_EQUAL to "EQUAL",
            OP_EQUALVERIFY to "EQUALVERIFY",
            OP_RESERVED1 to "RESERVED1",
            OP_RESERVED2 to "RESERVED2",
            OP_1ADD to "1ADD",
            OP_1SUB to "1SUB",
            OP_2MUL to "2MUL",
            OP_2DIV to "2DIV",
            OP_NEGATE to "NEGATE",
            OP_ABS to "ABS",
            OP_NOT to "NOT",
            OP_0NOTEQUAL to "0NOTEQUAL",
            OP_ADD to "ADD",
            OP_SUB to "SUB",
            OP_MUL to "MUL",
            OP_DIV to "DIV",
            OP_MOD to "MOD",
            OP_LSHIFT to "LSHIFT",
            OP_RSHIFT to "RSHIFT",
            OP_BOOLAND to "BOOLAND",
            OP_BOOLOR to "BOOLOR",
            OP_NUMEQUAL to "NUMEQUAL",
            OP_NUMEQUALVERIFY to "NUMEQUALVERIFY",
            OP_NUMNOTEQUAL to "NUMNOTEQUAL",
            OP_LESSTHAN to "LESSTHAN",
            OP_GREATERTHAN to "GREATERTHAN",
            OP_LESSTHANOREQUAL to "LESSTHANOREQUAL",
            OP_GREATERTHANOREQUAL to "GREATERTHANOREQUAL",
            OP_MIN to "MIN",
            OP_MAX to "MAX",
            OP_WITHIN to "WITHIN",
            OP_RIPEMD160 to "RIPEMD160",
            OP_SHA1 to "SHA1",
            OP_SHA256 to "SHA256",
            OP_HASH160 to "HASH160",
            OP_HASH256 to "HASH256",
            OP_CODESEPARATOR to "CODESEPARATOR",
            OP_CHECKSIG to "CHECKSIG",
            OP_CHECKSIGVERIFY to "CHECKSIGVERIFY",
            OP_CHECKMULTISIG to "CHECKMULTISIG",
            OP_CHECKMULTISIGVERIFY to "CHECKMULTISIGVERIFY",
            OP_NOP1 to "NOP1",
            OP_CHECKLOCKTIMEVERIFY to "CHECKLOCKTIMEVERIFY",
            OP_CHECKSEQUENCEVERIFY to "CHECKSEQUENCEVERIFY",
            OP_NOP4 to "NOP4",
            OP_NOP5 to "NOP5",
            OP_NOP6 to "NOP6",
            OP_NOP7 to "NOP7",
            OP_NOP8 to "NOP8",
            OP_NOP9 to "NOP9",
            OP_NOP10 to "NOP10"
    )

    val p2pkhStart = byteArrayOf(OP_DUP.toByte(), OP_HASH160.toByte())
    val p2pkhEnd = byteArrayOf(OP_EQUALVERIFY.toByte(), OP_CHECKSIG.toByte())
    val p2pshStart = byteArrayOf(OP_HASH160.toByte())
    val p2pshEnd = byteArrayOf(OP_EQUAL.toByte())

    //  Converts the given OpCode into a string (eg "0", "PUSHDATA", or "NON_OP(10)")
    fun getOpCodeName(opcode: Int): String {
        return opCodeMap[opcode] ?: "NON_OP($opcode)"
    }

    //  Converts the given pushdata OpCode into a string (eg "PUSHDATA2", or "PUSHDATA(23)")
    fun getPushDataName(opcode: Int): String {
        return opCodeMap[opcode] ?: "PUSHDATA($opcode)"
    }

    fun push(value: Int) = when (value) {
        0 -> byteArrayOf(0)
        in 1..16 -> byteArrayOf()
        else -> byteArrayOf((value + 0x50).toByte())
    }

    fun push(data: ByteArray): ByteArray {
        val bytes = when (val length = data.size) {
            in 0x00..0x4b -> byteArrayOf(length.toByte())
            in 0x4c..0xff -> byteArrayOf(OP_PUSHDATA1.toByte(), length.toByte())
            in 0x0100..0xffff -> {
                val lengthBytes = Utils.intToByteArray(length)
                val length16InLE = byteArrayOf(lengthBytes[3], lengthBytes[2])
                byteArrayOf(OP_PUSHDATA2.toByte()) + length16InLE
            }
            in 0x10000..0xffffffff -> {
                val lengthBytesInLE = Utils.intToByteArray(length).reversedArray()
                byteArrayOf(OP_PUSHDATA2.toByte()) + lengthBytesInLE
            }
            else -> byteArrayOf()
        }
        return bytes + data
    }

    fun scriptWPKH(data: ByteArray, versionByte: Int = 0): ByteArray {
        return push(versionByte) + push(data)
    }

}

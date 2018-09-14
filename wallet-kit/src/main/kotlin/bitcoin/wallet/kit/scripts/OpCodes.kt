package bitcoin.wallet.kit.scripts

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

    private val opCodeNameMap = hashMapOf(
            "0" to OP_0,
            "PUSHDATA1" to OP_PUSHDATA1,
            "PUSHDATA2" to OP_PUSHDATA2,
            "PUSHDATA4" to OP_PUSHDATA4,
            "1NEGATE" to OP_1NEGATE,
            "RESERVED" to OP_RESERVED,
            "1" to OP_1,
            "2" to OP_2,
            "3" to OP_3,
            "4" to OP_4,
            "5" to OP_5,
            "6" to OP_6,
            "7" to OP_7,
            "8" to OP_8,
            "9" to OP_9,
            "10" to OP_10,
            "11" to OP_11,
            "12" to OP_12,
            "13" to OP_13,
            "14" to OP_14,
            "15" to OP_15,
            "16" to OP_16,
            "NOP" to OP_NOP,
            "VER" to OP_VER,
            "IF" to OP_IF,
            "NOTIF" to OP_NOTIF,
            "VERIF" to OP_VERIF,
            "VERNOTIF" to OP_VERNOTIF,
            "ELSE" to OP_ELSE,
            "ENDIF" to OP_ENDIF,
            "VERIFY" to OP_VERIFY,
            "RETURN" to OP_RETURN,
            "TOALTSTACK" to OP_TOALTSTACK,
            "FROMALTSTACK" to OP_FROMALTSTACK,
            "2DROP" to OP_2DROP,
            "2DUP" to OP_2DUP,
            "3DUP" to OP_3DUP,
            "2OVER" to OP_2OVER,
            "2ROT" to OP_2ROT,
            "2SWAP" to OP_2SWAP,
            "IFDUP" to OP_IFDUP,
            "DEPTH" to OP_DEPTH,
            "DROP" to OP_DROP,
            "DUP" to OP_DUP,
            "NIP" to OP_NIP,
            "OVER" to OP_OVER,
            "PICK" to OP_PICK,
            "ROLL" to OP_ROLL,
            "ROT" to OP_ROT,
            "SWAP" to OP_SWAP,
            "TUCK" to OP_TUCK,
            "CAT" to OP_CAT,
            "SUBSTR" to OP_SUBSTR,
            "LEFT" to OP_LEFT,
            "RIGHT" to OP_RIGHT,
            "SIZE" to OP_SIZE,
            "INVERT" to OP_INVERT,
            "AND" to OP_AND,
            "OR" to OP_OR,
            "XOR" to OP_XOR,
            "EQUAL" to OP_EQUAL,
            "EQUALVERIFY" to OP_EQUALVERIFY,
            "RESERVED1" to OP_RESERVED1,
            "RESERVED2" to OP_RESERVED2,
            "1ADD" to OP_1ADD,
            "1SUB" to OP_1SUB,
            "2MUL" to OP_2MUL,
            "2DIV" to OP_2DIV,
            "NEGATE" to OP_NEGATE,
            "ABS" to OP_ABS,
            "NOT" to OP_NOT,
            "0NOTEQUAL" to OP_0NOTEQUAL,
            "ADD" to OP_ADD,
            "SUB" to OP_SUB,
            "MUL" to OP_MUL,
            "DIV" to OP_DIV,
            "MOD" to OP_MOD,
            "LSHIFT" to OP_LSHIFT,
            "RSHIFT" to OP_RSHIFT,
            "BOOLAND" to OP_BOOLAND,
            "BOOLOR" to OP_BOOLOR,
            "NUMEQUAL" to OP_NUMEQUAL,
            "NUMEQUALVERIFY" to OP_NUMEQUALVERIFY,
            "NUMNOTEQUAL" to OP_NUMNOTEQUAL,
            "LESSTHAN" to OP_LESSTHAN,
            "GREATERTHAN" to OP_GREATERTHAN,
            "LESSTHANOREQUAL" to OP_LESSTHANOREQUAL,
            "GREATERTHANOREQUAL" to OP_GREATERTHANOREQUAL,
            "MIN" to OP_MIN,
            "MAX" to OP_MAX,
            "WITHIN" to OP_WITHIN,
            "RIPEMD160" to OP_RIPEMD160,
            "SHA1" to OP_SHA1,
            "SHA256" to OP_SHA256,
            "HASH160" to OP_HASH160,
            "HASH256" to OP_HASH256,
            "CODESEPARATOR" to OP_CODESEPARATOR,
            "CHECKSIG" to OP_CHECKSIG,
            "CHECKSIGVERIFY" to OP_CHECKSIGVERIFY,
            "CHECKMULTISIG" to OP_CHECKMULTISIG,
            "CHECKMULTISIGVERIFY" to OP_CHECKMULTISIGVERIFY,
            "NOP1" to OP_NOP1,
            "CHECKLOCKTIMEVERIFY" to OP_CHECKLOCKTIMEVERIFY,
            "CHECKSEQUENCEVERIFY" to OP_CHECKSEQUENCEVERIFY,
            "NOP2" to OP_NOP2,
            "NOP3" to OP_NOP3,
            "NOP4" to OP_NOP4,
            "NOP5" to OP_NOP5,
            "NOP6" to OP_NOP6,
            "NOP7" to OP_NOP7,
            "NOP8" to OP_NOP8,
            "NOP9" to OP_NOP9,
            "NOP10" to OP_NOP10
    )

    // Converts the given OpCode into a string (eg "0", "PUSHDATA", or "NON_OP(10)")
    fun getOpCodeName(opcode: Int): String {
        return if (opCodeMap.containsKey(opcode)) opCodeMap[opcode]!! else "NON_OP($opcode)"
    }

    // Converts the given pushdata OpCode into a string (eg "PUSHDATA2", or "PUSHDATA(23)")
    fun getPushDataName(opcode: Int): String {
        return if (opCodeMap.containsKey(opcode)) opCodeMap[opcode]!! else "PUSHDATA($opcode)"
    }

    // Converts the given OpCodeName into an int
    fun getOpCode(opCodeName: String): Int {
        return if (opCodeNameMap.containsKey(opCodeName)) opCodeNameMap[opCodeName]!! else OP_INVALIDOPCODE
    }
}

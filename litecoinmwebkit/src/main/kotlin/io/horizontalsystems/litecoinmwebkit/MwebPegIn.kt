package io.horizontalsystems.litecoinmwebkit

/**
 * The pure parts of the peg-in sequence, kept apart from the kit so they can
 * be unit tested: turning the Litecoin wallet's unsigned transaction into
 * the daemon's template, and putting the change output back afterwards.
 */
object MwebPegIn {

    /** Size of the peg-in output script: OP_9 and a 32-byte kernel hash push. */
    const val PEG_IN_SCRIPT_SIZE = 34

    class Template(val rawTx: ByteArray, val change: TemplateOutput?)

    /**
     * Builds the daemon template from the unsigned Litecoin transaction that
     * pays `value + mwebFee` to the MWEB address at output 0 and keeps its
     * change at [changeOutputIndex].
     *
     * The change output is removed, because the daemon turns every non-MWEB
     * output into a peg-out, and the MWEB output is set to [value]: the
     * daemon adds the MWEB fee on top of it when it sizes the peg-in output,
     * so the peg-in output ends up at `value + mwebFee`, which is what the
     * inputs minus the Litecoin fee and change pay.
     */
    fun template(unsignedRawTx: ByteArray, changeOutputIndex: Int?, destination: ByteArray, value: Long): Template {
        require(MwebAddress.isMwebScript(destination)) { "destination is not an MWEB output script" }
        val tx = MwebTxSerializer.parse(unsignedRawTx)
        require(tx.outputs.isNotEmpty() && MwebAddress.isMwebScript(tx.outputs[0].script)) {
            "unsigned transaction must pay an MWEB output first"
        }
        val change = changeOutputIndex?.let { tx.outputs.removeAt(it) }
        tx.outputs[0] = TemplateOutput(destination, value)
        return Template(MwebTxSerializer.serialize(tx), change)
    }

    /**
     * Checks the daemon's result and appends the change output again.
     * Returns the transaction to hand to the Litecoin wallet for signing.
     */
    fun withChange(createdRawTx: ByteArray, change: TemplateOutput?, expectedPegIn: Long): ByteArray {
        val tx = MwebTxSerializer.parse(createdRawTx)
        if (tx.mweb == null || tx.outputs.size != 1) {
            throw IllegalStateException("daemon returned no peg-in transaction")
        }
        val pegIn = tx.outputs[0]
        if (pegIn.script.size != PEG_IN_SCRIPT_SIZE || pegIn.script[0].toInt() != OP_9) {
            throw IllegalStateException("daemon returned no peg-in output")
        }
        if (pegIn.value != expectedPegIn) {
            throw IllegalStateException("peg-in output ${pegIn.value} differs from $expectedPegIn")
        }
        if (change != null) tx.outputs.add(change)
        return MwebTxSerializer.serialize(tx)
    }

    private const val OP_9 = 0x59
}

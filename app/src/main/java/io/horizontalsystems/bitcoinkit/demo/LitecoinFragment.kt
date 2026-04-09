package io.horizontalsystems.bitcoinkit.demo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bitcoincore.BitcoinCore
import java.text.SimpleDateFormat
import java.util.*

class LitecoinFragment : Fragment() {

    private lateinit var viewModel: LitecoinViewModel

    private lateinit var syncStateValue: TextView
    private lateinit var balanceValue: TextView
    private lateinit var mwebBalanceValue: TextView
    private lateinit var lastBlockValue: TextView
    private lateinit var receiveAddressText: TextView
    private lateinit var mwebAddressText: TextView
    private lateinit var pegOutAddressEdit: EditText
    private lateinit var pegOutAmountEdit: EditText
    private lateinit var pegOutFeeRateEdit: EditText
    private lateinit var pegOutFeeEstimateText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var clearButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_litecoin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        syncStateValue = view.findViewById(R.id.ltcSyncState)
        balanceValue = view.findViewById(R.id.ltcBalance)
        mwebBalanceValue = view.findViewById(R.id.ltcMwebBalance)
        lastBlockValue = view.findViewById(R.id.ltcLastBlock)
        receiveAddressText = view.findViewById(R.id.ltcReceiveAddress)
        mwebAddressText = view.findViewById(R.id.ltcMwebAddress)
        pegOutAddressEdit = view.findViewById(R.id.ltcPegOutAddress)
        pegOutAmountEdit = view.findViewById(R.id.ltcPegOutAmount)
        pegOutFeeRateEdit = view.findViewById(R.id.ltcPegOutFeeRate)
        pegOutFeeEstimateText = view.findViewById(R.id.ltcPegOutFeeEstimate)
        startButton = view.findViewById(R.id.ltcButtonStart)
        stopButton = view.findViewById(R.id.ltcButtonStop)
        clearButton = view.findViewById(R.id.ltcButtonClear)

        view.findViewById<Button>(R.id.ltcCopyReceive).setOnClickListener {
            copyToClipboard("LTC address", receiveAddressText.text.toString())
        }
        view.findViewById<Button>(R.id.ltcCopyMweb).setOnClickListener {
            copyToClipboard("MWEB address", mwebAddressText.text.toString())
        }

        view.findViewById<Button>(R.id.ltcButtonEstimateFee).setOnClickListener {
            val amount = pegOutAmountEdit.text.toString().toLongOrNull()
            if (amount == null || amount <= 0) {
                toast("Enter a valid amount first")
                return@setOnClickListener
            }
            val feeRate = pegOutFeeRateEdit.text.toString().toIntOrNull() ?: 10
            viewModel.estimatePegOutFee(amount, feeRate)
        }

        view.findViewById<Button>(R.id.ltcButtonPegOut).setOnClickListener {
            val address = pegOutAddressEdit.text.toString().trim()
            val amount = pegOutAmountEdit.text.toString().toLongOrNull()
            val feeEstimate = viewModel.pegOutFeeEstimate.value

            when {
                address.isEmpty() -> toast("Enter destination address")
                amount == null || amount <= 0 -> toast("Enter a valid amount")
                feeEstimate == null -> toast("Tap 'Estimate Fee' first")
                else -> viewModel.pegOut(address, amount, feeEstimate)
            }
        }

        startButton.setOnClickListener { viewModel.start() }
        stopButton.setOnClickListener { viewModel.stop() }
        clearButton.setOnClickListener { viewModel.clear() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(LitecoinViewModel::class.java)
        viewModel.init()

        val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.US)

        viewModel.syncState.observe(this, Observer { state ->
            syncStateValue.text = when (state) {
                is BitcoinCore.KitState.Synced -> "Synced"
                is BitcoinCore.KitState.ApiSyncing -> "API syncing (${state.transactions} txs)"
                is BitcoinCore.KitState.Syncing -> "Syncing ${"%.1f".format(state.progress * 100)}%"
                is BitcoinCore.KitState.NotSynced -> "Not synced: ${state.exception.javaClass.simpleName}"
                else -> state.toString()
            }
        })

        viewModel.balance.observe(this, Observer { balance ->
            balanceValue.text = if (balance == null) "—"
            else "LTC: ${"%.8f".format(balance.spendable / 1e8)}"
        })

        viewModel.mwebBalance.observe(this, Observer { sat ->
            mwebBalanceValue.text = if (sat == null || sat == 0L) "0 sat"
            else "$sat sat (${"%.8f".format(sat / 1e8)} LTC)"
        })

        viewModel.lastBlock.observe(this, Observer { block ->
            lastBlockValue.text = block?.let {
                "#${it.height}  ${dateFormat.format(Date(it.timestamp * 1000))}"
            } ?: "—"
        })

        viewModel.status.observe(this, Observer { s ->
            startButton.isEnabled = s == LitecoinViewModel.Status.STOPPED
            stopButton.isEnabled = s == LitecoinViewModel.Status.STARTED
        })

        viewModel.receiveAddress.observe(this, Observer { addr ->
            receiveAddressText.text = addr ?: "—"
        })

        viewModel.mwebAddress.observe(this, Observer { addr ->
            mwebAddressText.text = addr ?: "—"
        })

        viewModel.pegOutFeeEstimate.observe(this, Observer { fee ->
            pegOutFeeEstimateText.text = if (fee == null) "—" else "$fee sat"
        })

        viewModel.pegOutResult.observe(this, Observer { msg ->
            if (!msg.isNullOrEmpty()) toast(msg)
        })
    }

    private fun copyToClipboard(label: String, text: String) {
        if (text == "—" || text == "(not available)") return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        toast("$label copied")
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
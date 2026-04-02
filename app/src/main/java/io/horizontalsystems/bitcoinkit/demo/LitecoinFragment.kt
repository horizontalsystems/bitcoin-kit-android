package io.horizontalsystems.bitcoinkit.demo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
    private lateinit var lastBlockValue: TextView
    private lateinit var receiveAddressText: TextView
    private lateinit var mwebAddressText: TextView
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
        lastBlockValue = view.findViewById(R.id.ltcLastBlock)
        receiveAddressText = view.findViewById(R.id.ltcReceiveAddress)
        mwebAddressText = view.findViewById(R.id.ltcMwebAddress)
        startButton = view.findViewById(R.id.ltcButtonStart)
        stopButton = view.findViewById(R.id.ltcButtonStop)
        clearButton = view.findViewById(R.id.ltcButtonClear)

        view.findViewById<Button>(R.id.ltcCopyReceive).setOnClickListener {
            copyToClipboard("LTC address", receiveAddressText.text.toString())
        }
        view.findViewById<Button>(R.id.ltcCopyMweb).setOnClickListener {
            copyToClipboard("MWEB address", mwebAddressText.text.toString())
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
            balanceValue.text = if (balance == null) {
                "—"
            } else {
                val spendable = "%.8f".format(balance.spendable / 1e8)
                "LTC: $spendable"
            }
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
    }

    private fun copyToClipboard(label: String, text: String) {
        if (text == "—" || text == "(not available)") return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(requireContext(), "$label copied", Toast.LENGTH_SHORT).show()
    }
}
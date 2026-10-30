package io.horizontalsystems.bitcoinkit.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class MwebFragment : Fragment() {

    private lateinit var viewModel: MwebViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_mweb, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(MwebViewModel::class.java)

        val syncState = view.findViewById<TextView>(R.id.mwebSyncState)
        val balance = view.findViewById<TextView>(R.id.mwebBalance)
        val litecoinState = view.findViewById<TextView>(R.id.ltcSyncState)
        val litecoinBalance = view.findViewById<TextView>(R.id.ltcBalance)
        val address = view.findViewById<TextView>(R.id.mwebAddress)
        val transactions = view.findViewById<TextView>(R.id.mwebTransactions)
        val message = view.findViewById<TextView>(R.id.mwebMessage)
        val sendAddress = view.findViewById<EditText>(R.id.mwebSendAddress)
        val sendAmount = view.findViewById<EditText>(R.id.mwebSendAmount)
        val pegInAmount = view.findViewById<EditText>(R.id.mwebPegInAmount)
        val startButton = view.findViewById<Button>(R.id.mwebStart)

        view.findViewById<TextView>(R.id.mwebNetwork).text = viewModel.networkType.name

        viewModel.syncState.observe(viewLifecycleOwner) { syncState.text = it }
        viewModel.balance.observe(viewLifecycleOwner) { balance.text = it }
        viewModel.litecoinState.observe(viewLifecycleOwner) { litecoinState.text = it }
        viewModel.litecoinBalance.observe(viewLifecycleOwner) { litecoinBalance.text = it }
        viewModel.receiveAddress.observe(viewLifecycleOwner) { address.text = it }
        viewModel.transactions.observe(viewLifecycleOwner) { transactions.text = it }
        viewModel.message.observe(viewLifecycleOwner) { message.text = it }
        viewModel.started.observe(viewLifecycleOwner) { startButton.isEnabled = it != true }

        startButton.setOnClickListener { viewModel.start() }
        view.findViewById<Button>(R.id.mwebRefresh).setOnClickListener { viewModel.refresh() }
        view.findViewById<Button>(R.id.mwebClear).setOnClickListener { viewModel.clear() }
        view.findViewById<Button>(R.id.mwebReceive).setOnClickListener { viewModel.onReceiveClick() }
        view.findViewById<Button>(R.id.ltcReceive).setOnClickListener { viewModel.onLitecoinReceiveClick() }
        view.findViewById<Button>(R.id.mwebSend).setOnClickListener {
            viewModel.onSendClick(sendAddress.text.toString(), sendAmount.text.toString())
        }
        view.findViewById<Button>(R.id.mwebPegIn).setOnClickListener {
            viewModel.onPegInClick(pegInAmount.text.toString())
        }
    }
}

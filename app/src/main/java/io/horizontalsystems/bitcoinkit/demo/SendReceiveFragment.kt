package io.horizontalsystems.bitcoinkit.demo

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import io.horizontalsystems.bitcoinkit.managers.UnspentOutputSelector

class SendReceiveFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var receiveAddressButton: Button
    private lateinit var receiveAddressText: TextView

    private lateinit var sendButton: Button
    private lateinit var sendAmount: EditText
    private lateinit var sendAddress: EditText
    private lateinit var txFeeValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(MainViewModel::class.java)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_send_receive, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        receiveAddressText = view.findViewById(R.id.receiveAddressText)
        receiveAddressButton = view.findViewById(R.id.receiveAddressButton)
        receiveAddressButton.setOnClickListener {
            receiveAddressText.text = viewModel.receiveAddress()
        }

        txFeeValue = view.findViewById(R.id.txFeeValue)
        sendAddress = view.findViewById(R.id.sendAddress)
        sendAmount = view.findViewById(R.id.sendAmount)
        sendButton = view.findViewById(R.id.sendButton)
        sendButton.setOnClickListener {
            if (sendAddress.text.isEmpty()) {
                sendAddress.error = "Send address cannot be blank"
            } else if (sendAmount.text.isEmpty()) {
                sendAmount.error = "Send amount cannot be blank"
            } else {
                send()
            }
        }

        sendAmount.addTextChangedListener(textChangeListener)
    }

    private fun send() {
        var message: String
        try {
            viewModel.send(sendAddress.text.toString(), sendAmount.text.toString().toLong())
            sendAmount.text = null
            txFeeValue.text = null
            sendAddress.text = null
            message = "Transaction sent"
        } catch (e: Exception) {
            message = when (e) {
                is UnspentOutputSelector.Error.InsufficientUnspentOutputs,
                is UnspentOutputSelector.Error.EmptyUnspentOutputs -> "Insufficient balance"
                else -> e.message ?: "Failed to send transaction"
            }
        }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private val textChangeListener = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if (sendAddress.text.isEmpty() || sendAmount.text.isEmpty()) {
                return
            }

            try {
                txFeeValue.text = viewModel.fee(
                        value = sendAmount.text.toString().toLong(),
                        address = sendAddress.text.toString()
                ).toString()
            } catch (e: Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
}

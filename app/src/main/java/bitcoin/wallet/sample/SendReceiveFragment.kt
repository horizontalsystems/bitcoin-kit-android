package bitcoin.wallet.sample

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
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
    }

    private fun send() {
        try {
            viewModel.send(sendAddress.text.toString(), sendAmount.text.toString().toInt())
        } catch (e: Exception) {
            val message = when (e) {
                is UnspentOutputSelector.InsufficientUnspentOutputs,
                is UnspentOutputSelector.EmptyUnspentOutputs -> "Insufficient balance"
                else -> e.message
            }

            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}

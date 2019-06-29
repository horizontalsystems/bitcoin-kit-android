package io.horizontalsystems.bitcoinkit.demo

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.horizontalsystems.bitcoincore.managers.UnspentOutputSelectorError
import io.horizontalsystems.bitcoincore.models.FeePriority

class SendReceiveFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var receiveAddressButton: Button
    private lateinit var receiveAddressText: TextView

    private lateinit var sendButton: Button
    private lateinit var maxButton: Button
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
        maxButton = view.findViewById(R.id.maxButton)
        sendButton.setOnClickListener {
            if (sendAddress.text.isEmpty()) {
                sendAddress.error = "Send address cannot be blank"
            } else if (sendAmount.text.isEmpty()) {
                sendAmount.error = "Send amount cannot be blank"
            } else {
                send()
            }
        }
        maxButton.setOnClickListener {
            viewModel.balance.value?.let { balance ->
                val fee = try {
                    viewModel.fee(balance)
                } catch (e: UnspentOutputSelectorError.InsufficientUnspentOutputs) {
                    e.fee
                } catch (e: Exception) {
                    0L
                }

                sendAmount.setText("${balance - fee}")
            }
        }

        sendAmount.addTextChangedListener(textChangeListener)

        val customFeePriority = view.findViewById<EditText>(R.id.customFeePriority)
        customFeePriority.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val feePriority = s.toString().toIntOrNull()
                feePriority?.let {
                    viewModel.feePriority = FeePriority.Custom(it)
                    updateFee()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroup)
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val feePriority = when (checkedId) {
                R.id.radioLowest ->  FeePriority.Lowest
                R.id.radioLow ->  FeePriority.Low
                R.id.radioMedium ->  FeePriority.Medium
                R.id.radioHigh ->  FeePriority.High
                else ->  FeePriority.Highest
            }
            customFeePriority.setText("")
            viewModel.feePriority = feePriority
            updateFee()
        }
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
                is UnspentOutputSelectorError.InsufficientUnspentOutputs,
                is UnspentOutputSelectorError.EmptyUnspentOutputs -> "Insufficient balance"
                else -> e.message ?: "Failed to send transaction (${e.javaClass.name})"
            }
        }

        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private val textChangeListener = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            updateFee()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private fun updateFee() {
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
}

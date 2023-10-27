package io.horizontalsystems.bitcoinkit.demo

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.hodler.LockTimeInterval

class SendReceiveFragment : Fragment() {
    private lateinit var viewModel: MainViewModel

    lateinit var receiveAddressText: TextView
    lateinit var receiveAddressButton: Button
    lateinit var sendAmount: EditText
    lateinit var sendAddress: EditText
    lateinit var txFeeValue: TextView
    lateinit var sendButton: Button
    lateinit var maxButton: Button
    lateinit var radioGroup: RadioGroup
    lateinit var lockTimePeriodValue: Spinner

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_send_receive, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.let { ViewModelProvider(it).get(MainViewModel::class.java) } ?: return

        viewModel.receiveAddressLiveData.observe(viewLifecycleOwner, Observer {
            receiveAddressText.text = it
        })

        viewModel.amountLiveData.observe(viewLifecycleOwner, Observer {
            sendAmount.setText(it?.toString())
        })

        viewModel.addressLiveData.observe(viewLifecycleOwner, Observer {
            sendAddress.setText(it)
        })

        viewModel.feeLiveData.observe(viewLifecycleOwner, Observer {
            txFeeValue.text = it?.toString()
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            //TODO TOASTS NO PREVIOUS OUTPUT SCRIPT CHANGE THE SEED?
            val toast = Toast.makeText(context, "$it", Toast.LENGTH_LONG)

            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        })

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        receiveAddressText = view.findViewById(R.id.receiveAddressText)
        receiveAddressButton = view.findViewById(R.id.receiveAddressButton)
        sendAmount = view.findViewById(R.id.sendAmount)
        sendAddress = view.findViewById(R.id.sendAddress)
        txFeeValue = view.findViewById(R.id.txFeeValue)
        sendButton = view.findViewById(R.id.sendButton)
        maxButton = view.findViewById(R.id.maxButton)
        radioGroup = view.findViewById(R.id.radioGroup)
        lockTimePeriodValue = view.findViewById(R.id.lockTimePeriodValue)

        receiveAddressButton.setOnClickListener {
            viewModel.onReceiveClick()
        }

        sendButton.setOnClickListener {
            viewModel.onSendClick()
        }

        maxButton.setOnClickListener {


            viewModel.onMaxClick()

        }

        sendAmount.addTextChangedListener(SimpleTextWatcher {
            viewModel.amount = try {
                sendAmount.text?.toString()?.toLong()
            } catch (e: NumberFormatException) {
                null
            }
        })

        sendAddress.addTextChangedListener(SimpleTextWatcher {
            viewModel.address = sendAddress.text.toString()
        })

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val feePriority = when (checkedId) {
                R.id.radioLow -> FeePriority.Low
                R.id.radioMedium -> FeePriority.Medium
                R.id.radioHigh -> FeePriority.High
                else -> throw Exception("Undefined priority")
            }
            viewModel.feePriority = feePriority
        }


        lockTimePeriodValue.adapter = ArrayAdapter(view.context, android.R.layout.simple_spinner_dropdown_item, LockTimeIntervalOption.getIntervals())
        lockTimePeriodValue.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.timeLockInterval = (parent?.getItemAtPosition(position) as LockTimeIntervalOption).interval
            }
        }
    }
}

class LockTimeIntervalOption(val interval: LockTimeInterval? = null) {
    override fun toString(): String {
        return interval?.name ?: "OFF"
    }

    companion object {
        fun getIntervals(): Array<LockTimeIntervalOption> {
            return arrayOf(LockTimeIntervalOption()) + LockTimeInterval.values().map { LockTimeIntervalOption(it) }
        }
    }
}

class SimpleTextWatcher(private val callback: (s: Editable?) -> Unit) : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
        callback.invoke(s)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
}

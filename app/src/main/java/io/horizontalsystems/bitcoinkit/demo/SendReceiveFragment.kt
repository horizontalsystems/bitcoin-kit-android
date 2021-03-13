package io.horizontalsystems.bitcoinkit.demo

import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException
import io.horizontalsystems.bitcoincore.managers.SendValueErrors
import io.horizontalsystems.hodler.LockTimeInterval
import kotlinx.android.synthetic.main.fragment_send_receive.*

class SendReceiveFragment : Fragment() {
    private lateinit var viewModel: MainViewModel

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

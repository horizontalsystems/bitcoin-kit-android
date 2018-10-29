package io.horizontalsystems.bitcoinkit.demo

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

class BalanceFragment : Fragment() {

    lateinit var viewModel: MainViewModel
    lateinit var networkName: TextView
    lateinit var balanceValue: TextView
    lateinit var lastBlockValue: TextView
    lateinit var progressValue: TextView
    lateinit var startButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.let {
            viewModel = ViewModelProviders.of(it).get(MainViewModel::class.java)

            viewModel.balance.observe(this, Observer { balance ->
                balanceValue.text = when(balance) {
                    null -> ""
                    else -> NumberFormatHelper.cryptoAmountFormat.format(balance / 100_000_000.0)
                }
            })

            viewModel.lastBlockHeight.observe(this, Observer {
                lastBlockValue.text = (it ?: 0).toString()
            })

            viewModel.progress.observe(this, Observer {
                val percentage = (it ?: 0.0) * 100
                progressValue.text = "${percentage}%"
            })

            viewModel.status.observe(this, Observer {
                when (it) {
                    MainViewModel.State.STARTED -> {
                        startButton.isEnabled = false
                    }
                    else -> {
                        startButton.isEnabled = true
                    }
                }
            })

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_balance, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        networkName = view.findViewById(R.id.networkName)
        networkName.text = viewModel.networkName

        balanceValue = view.findViewById(R.id.balanceValue)
        lastBlockValue = view.findViewById(R.id.lastBlockValue)
        progressValue = view.findViewById(R.id.progressValue)
        startButton = view.findViewById(R.id.buttonStart)

        startButton.setOnClickListener {
            viewModel.start()
        }
    }
}

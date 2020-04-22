package io.horizontalsystems.bitcoinkit.demo

import android.annotation.SuppressLint
import androidx.lifecycle.Observer
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.models.TransactionInputInfo
import io.horizontalsystems.bitcoincore.models.TransactionOutputInfo
import io.horizontalsystems.dashkit.models.DashTransactionInfo
import io.horizontalsystems.hodler.HodlerOutputData
import io.horizontalsystems.hodler.HodlerPlugin
import java.text.DateFormat
import java.util.*

class TransactionsFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var transactionsRecyclerView: RecyclerView
    private val transactionsAdapter = TransactionsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        viewModel.transactions.observe(this, Observer {
            it?.let { transactions ->
                transactionsAdapter.items = transactions
                transactionsAdapter.notifyDataSetChanged()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionsRecyclerView = view.findViewById(R.id.transactions)
        transactionsRecyclerView.adapter = transactionsAdapter
        transactionsRecyclerView.layoutManager = LinearLayoutManager(context)
    }
}

class TransactionsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var items = listOf<TransactionInfo>()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolderTransaction(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_transaction, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolderTransaction -> holder.bind(items[position], itemCount - position)
        }
    }
}

class ViewHolderTransaction(val containerView: View) : RecyclerView.ViewHolder(containerView) {
    private val summary = containerView.findViewById<TextView>(R.id.summary)!!

    @SuppressLint("SetTextI18n")
    fun bind(transactionInfo: TransactionInfo, index: Int) {
        containerView.setBackgroundColor(if (index % 2 == 0) Color.parseColor("#dddddd") else Color.TRANSPARENT)

        val txAmount = calculateAmount(transactionInfo)
        val amount = NumberFormatHelper.cryptoAmountFormat.format(txAmount / 100_000_000.0)
        val fee = transactionInfo.fee?.let {
            NumberFormatHelper.cryptoAmountFormat.format(it / 100_000_000.0)
        } ?: "n/a"

        var text = "#$index"
        text += "\nStatus: ${transactionInfo.status.name}"
        if (transactionInfo is DashTransactionInfo) {
            text += "\nInstant: ${transactionInfo.instantTx.toString().toUpperCase(Locale.getDefault())}"
        }

        text += "\nInputs: ${mapInputs(transactionInfo.inputs)}" +
                "\nOutputs: ${mapOutputs(transactionInfo.outputs)}" +
                "\nAmount: $amount" +
                "\nFee: $fee" +
                "\nTx hash: ${transactionInfo.transactionHash}" +
                "\nTx index: ${transactionInfo.transactionIndex}" +
                "\nBlock: ${transactionInfo.blockHeight}" +
                "\nTimestamp: ${transactionInfo.timestamp}" +
                "\nDate: ${formatDate(transactionInfo.timestamp)}" +
                "\nConflicting tx hash: ${transactionInfo.conflictingTxHash}"

        summary.text = text
        summary.setOnClickListener {
            println(transactionInfo)
            println(text)
        }
    }

    private fun calculateAmount(transactionInfo: TransactionInfo): Long {
        var myInputsTotalValue = 0L

        transactionInfo.inputs.forEach { input ->
            if (input.mine) {
                myInputsTotalValue += input.value ?: 0
            }
        }

        var myOutputsTotalValue = 0L

        transactionInfo.outputs.forEach {
            myOutputsTotalValue += if (it.mine && it.address != null) it.value else 0
        }

        return myOutputsTotalValue - myInputsTotalValue + (transactionInfo.fee ?: 0)
    }

    private fun mapOutputs(list: List<TransactionOutputInfo>): String {
        return list.joinToString("") {
            val sb = StringBuilder()
            sb.append("\n- address: ${it.address}")
            sb.append("\n  value: ${it.value}")
            sb.append("\n  mine: ${it.mine}")
            sb.append("\n  change: ${it.changeOutput}")

            if (it.pluginId == HodlerPlugin.id && it.pluginData != null) {
                (it.pluginData as? HodlerOutputData)?.let { hodlerData ->
                    val lockTimeInterval = hodlerData.lockTimeInterval

                    hodlerData.approxUnlockTime?.let { lockedUntilApprox ->
                        sb.append("\n  * Locked: ${lockTimeInterval.name}, approx until ${formatDate(lockedUntilApprox)}")
                    }

                    sb.append("\n  * Address: ${hodlerData.addressString}")
                    sb.append("\n  * Value: ${it.value}")
                }
            }
            sb.toString()
        }
    }

    private fun mapInputs(list: List<TransactionInputInfo>): String {
        return list.joinToString("") {
            val sb = StringBuilder()
            sb.append("\n- address: ${it.address}")
            sb.append("\n  value: ${it.value}")
            sb.append("\n  mine: ${it.mine}")

            sb.toString()
        }
    }

    private fun formatDate(timestamp: Long): String {
        return DateFormat.getInstance().format(Date(timestamp * 1000))
    }
}

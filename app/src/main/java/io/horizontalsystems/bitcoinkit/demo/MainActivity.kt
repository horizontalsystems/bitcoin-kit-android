package io.horizontalsystems.bitcoinkit.demo

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private val balanceFragment = BalanceFragment()
    private val transactionsFragment = TransactionsFragment()
    private val sendReceiveFragment = SendReceiveFragment()
    private val fm = supportFragmentManager
    private var active: Fragment = balanceFragment

    lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navigation = findViewById<BottomNavigationView>(R.id.navigation)
        navigation.setOnNavigationItemSelectedListener(this)

        fm.beginTransaction().add(R.id.fragment_container, sendReceiveFragment, "3").hide(sendReceiveFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, transactionsFragment, "2").hide(transactionsFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, balanceFragment, "1").commit()

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        viewModel.init()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val fragment = when (item.itemId) {
            R.id.navigation_home -> balanceFragment
            R.id.navigation_transactions -> transactionsFragment
            R.id.navigation_send_receive -> sendReceiveFragment
            else -> null
        }

        if (fragment != null) {
            supportFragmentManager
                    .beginTransaction()
                    .hide(active)
                    .show(fragment)
                    .commit()

            active = fragment

            return true
        }

        return false
    }

}

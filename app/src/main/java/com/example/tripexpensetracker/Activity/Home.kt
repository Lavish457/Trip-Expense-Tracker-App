package com.example.expensetrackerapp.Activity

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.expensetrackerapp.Fragement.CreditorFragement
import com.example.expensetrackerapp.Fragement.DebtorFragement
import com.example.expensetrackerapp.Fragement.FragementGraph
import com.example.expensetrackerapp.Fragement.HomeFragement
import com.example.tripexpensetracker.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class Home : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var myTrip: TextView
    private lateinit var groupExpenses: TextView
    private var isProgrammaticSelection = false
    private fun replaceFragment(fragment: Fragment, tag: String) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.midContainer, fragment, tag)
        if (tag != "trips") {
            transaction.addToBackStack(tag)
        }

        transaction.commit()
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        hideNavigationBar()
        bottomNavigationView = findViewById(R.id.bottomNavigation)

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView) { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }
        myTrip = findViewById(R.id.myTrip)
        groupExpenses = findViewById(R.id.groupExpenses)
        if(savedInstanceState == null)
        {
            replaceFragment(HomeFragement(), "trips")
            bottomNavigationView.selectedItemId = R.id.home
        }
        bottomNavigationView.setOnItemSelectedListener { item ->
            if (isProgrammaticSelection) return@setOnItemSelectedListener true
            when (item.itemId)
            {
                R.id.home ->
                {
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    replaceFragment(HomeFragement(),"trips")
                    myTrip.text = "My Trips"
                    groupExpenses.text = "Manage your group expenses"
                    true
                }
                R.id.creditor ->
                {
                    replaceFragment(CreditorFragement(),"creditors")
                    myTrip.text = "Creditors"
                    groupExpenses.text = "People who will receive money"
                    true
                }
                R.id.debtor ->
                {
                    replaceFragment(DebtorFragement(),"debtors")
                    myTrip.text = "Debtors"
                    groupExpenses.text = "People who give money"
                    true
                }
                R.id.graph ->
                {
                    replaceFragment(FragementGraph(),"graphs")
                    myTrip.text = "Analytics"
                    groupExpenses.text = "Track your spending patterns"
                    true
                }
                else -> false
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.midContainer)
            isProgrammaticSelection = true
            when (currentFragment) {
                is HomeFragement -> bottomNavigationView.selectedItemId = R.id.home
                is CreditorFragement -> bottomNavigationView.selectedItemId = R.id.creditor
                is DebtorFragement -> bottomNavigationView.selectedItemId = R.id.debtor
                is FragementGraph -> bottomNavigationView.selectedItemId = R.id.graph
            }
            isProgrammaticSelection = false
        }
    }
    private fun hideNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

}
package com.example.expensetrackerapp.Fragement

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.Adapter.CreditorAdapter
import com.example.expensetrackerapp.KT_DataClass.Balance
import com.example.tripexpensetracker.R
import com.example.expensetrackerapp.RetrofitClient
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class CreditorFragement : Fragment() {

    private val Users_BIN_ID = "691c63b7d0ea881f40f00aea"
    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CreditorAdapter
    private val creditorList = mutableListOf<Balance>()

    private var createrId: Long = -1
    private var isTestUser: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_creditor_fragement, container, false)

        // Check if current user is test account
        val prefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE)
        isTestUser = prefs.getBoolean("isTempUser", false)
        createrId = requireActivity()
            .getSharedPreferences("memberId", Context.MODE_PRIVATE)
            .getLong("memberId", -1L)

        Log.d("CreditorFragment", "createrId: $createrId | isTestUser: $isTestUser")

        recyclerView = view.findViewById(R.id.recycleCreditors)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = CreditorAdapter(requireContext(), creditorList)
        recyclerView.adapter = adapter

        fetchCreditors()

        return view
    }

    private fun fetchCreditors() {
        if (isTestUser) {
            fetchCreditorsFromFirebase()
        } else {
            fetchCreditorsFromApi()
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Fetch creditors from Firebase Realtime Database (for test users)
    // ───────────────────────────────────────────────────────────────
    private fun fetchCreditorsFromFirebase() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val database = FirebaseDatabase.getInstance()
                val balancesRef = database.getReference("userBalance")

                // Get all balances under userBalance
                val snapshot = balancesRef.get().await()

                val allBalances = mutableListOf<Balance>()

                for (tripSnapshot in snapshot.children) {
                    for (balanceSnapshot in tripSnapshot.children) {
                        val balance = balanceSnapshot.getValue(Balance::class.java)
                        if (balance != null) {
                            allBalances.add(balance)
                        }
                    }
                }

                val creditors = allBalances.filter {
                    it.balance < 0 && it.createrId == createrId
                }

                val totalPaiseOwedToYou = creditors.sumOf { kotlin.math.abs(it.balance).toLong() }
                val totalRupeesOwedToYou = totalPaiseOwedToYou

                val uniqueTrips = creditors.mapNotNull { it.tripId }.toSet().size

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    creditorList.clear()
                    creditorList.addAll(creditors)
                    adapter.notifyDataSetChanged()

                    view?.findViewById<TextView>(R.id.amountToPay)?.text = when {
                        creditors.isEmpty() -> "0"
                        else -> "₹ $totalRupeesOwedToYou"
                    }

                    view?.findViewById<TextView>(R.id.totalCreTrip)?.text = when {
                        creditors.isEmpty() -> "No one owes you money!"
                        uniqueTrips == 1 -> "Across 1 Trip"
                        else -> "Across $uniqueTrips Trips"
                    }

                    if (creditors.isEmpty()) {
                        Toast.makeText(requireContext(), "No money owed to you in shared data!", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    Log.e("CreditorFragment", "Firebase fetch error", e)
                    Toast.makeText(requireContext(), "Failed to load shared creditors", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Original API fetch (used only for normal users)
    // ───────────────────────────────────────────────────────────────
    private fun fetchCreditorsFromApi() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getBalance(Users_BIN_ID, API_KEY)
                val allBalances = response.record.userBalance ?: emptyList()

                val creditors = allBalances.filter {
                    it.balance < 0 && it.createrId == createrId
                }

                val totalPaiseOwedToYou = creditors.sumOf { kotlin.math.abs(it.balance).toLong() }
                val totalRupeesOwedToYou = totalPaiseOwedToYou

                val uniqueTrips = creditors.mapNotNull { it.tripId }.toSet().size

                creditorList.clear()
                creditorList.addAll(creditors)
                adapter.notifyDataSetChanged()

                view?.findViewById<TextView>(R.id.amountToPay)?.text = when {
                    creditors.isEmpty() -> "0"
                    else -> "₹ $totalRupeesOwedToYou"
                }

                view?.findViewById<TextView>(R.id.totalCreTrip)?.text = when {
                    creditors.isEmpty() -> "No one owes you money!"
                    uniqueTrips == 1 -> "Across 1 Trip"
                    else -> "Across $uniqueTrips Trips"
                }

                if (creditors.isEmpty()) {
                    Toast.makeText(requireContext(), "No money owes by you!", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.d("Error", "Error: ${e.message}")
                // Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
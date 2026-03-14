package com.example.expensetrackerapp.Fragement

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.Adapter.DebtorAdapter
import com.example.expensetrackerapp.KT_DataClass.Balance
import com.example.tripexpensetracker.R
import com.example.expensetrackerapp.RetrofitClient
import kotlinx.coroutines.launch

class DebtorFragement : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DebtorAdapter
    private val debtorList = mutableListOf<Balance>()
    private val Users_BIN_ID = "691c63b7d0ea881f40f00aea"
    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2
    private var createrId : Long = -1

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_debtor_fragement, container, false)
        createrId = requireActivity()
            .getSharedPreferences("memberId", MODE_PRIVATE)
            .getLong("memberId", -1)
        Log.d("IID", createrId.toString())
        recyclerView = view.findViewById(R.id.recycleDebtors)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = DebtorAdapter(requireContext(), debtorList)
        recyclerView.adapter = adapter

        fetchDebtors()

        return view
    }

    private fun fetchDebtors() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getBalance(Users_BIN_ID, API_KEY)
                val allBalances = response.record.userBalance

                val debtors = allBalances.filter { it.balance > 0 && it.createrId == createrId}

                val totalPaise = debtors.sumOf { it.balance.toLong() }
                val totalRupees = totalPaise

                val uniqueTripCount = debtors.mapNotNull { it.tripId }.toSet().size

                debtorList.clear()
                debtorList.addAll(debtors)
                adapter.notifyDataSetChanged()

                view?.findViewById<TextView>(R.id.amountReceive)?.text =
                    if (totalRupees > 0) {
                        "₹ ${totalRupees}"
                    } else {
                        "0"
                    }

                view?.findViewById<TextView>(R.id.totalTrip)?.text = when {
                    debtors.isEmpty() -> "No one owes you money!"
                    uniqueTripCount == 1 -> "Across 1 Trip"
                    else -> "Across $uniqueTripCount Trips"
                }

                if (debtors.isEmpty()) {
                    Toast.makeText(requireContext(), "No one owes you money!", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("Error", "Error: ${e.message}")
//                Toast.makeText(requireContext(), "Failed to load data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
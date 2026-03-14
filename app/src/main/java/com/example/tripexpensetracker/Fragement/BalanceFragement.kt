package com.example.expensetrackerapp.Fragement

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.Adapter.BalanceAdapter
import com.example.tripexpensetracker.R
import com.example.expensetrackerapp.RetrofitClient
import com.example.tripexpensetracker.TripBalanceViewModel
import kotlinx.coroutines.*

class BalanceFragement : Fragment() {

    private val Users_BIN_ID = "691c63b7d0ea881f40f00aea"
    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2

    private var tripID: Long = 0L
    private lateinit var userList: RecyclerView

    // ────────────────────────────────────────────────
    // ADDED: Observe the shared ViewModel from Activity
    // ────────────────────────────────────────────────
    private val viewModel: TripBalanceViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_balance_fragement, container, false)
        userList = view.findViewById(R.id.recycleBalance)
        userList.layoutManager = LinearLayoutManager(requireContext())

        tripID = arguments?.getLong("ID") ?: 0L

        if (tripID == 0L) {
            Toast.makeText(requireContext(), "Trip ID missing", Toast.LENGTH_SHORT).show()
        } else {
            // Initial load
            fetchUsers(tripID)

            // ────────────────────────────────────────────────
            // ADDED: Observe live updates from ViewModel
            // ────────────────────────────────────────────────
            viewModel.balances.observe(viewLifecycleOwner) { updatedBalances ->
                val filtered = updatedBalances.filter { it.tripId == tripID }
                (userList.adapter as? BalanceAdapter)?.submitList(filtered)

                if (filtered.isEmpty()) {
                    Toast.makeText(requireContext(), "No members yet", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    private fun fetchUsers(tripId: Long) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getBalance(Users_BIN_ID, API_KEY)
                val allUsers = response.record.userBalance ?: emptyList()
                val filteredUsers = allUsers.filter { it.tripId == tripId }

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    var adapter = BalanceAdapter()
                    adapter.submitList(filteredUsers)
                    userList.adapter = adapter

                    // Optional: also push to ViewModel so it's in sync
                    viewModel.setBalancesManually(filteredUsers)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    // Toast.makeText(requireContext(), "Error loading balances: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.d("Error", "Error : ${e.message}")
                }
            }
        }
    }

    companion object {
        fun newInstance(tripID: Long): BalanceFragement {
            val fragment = BalanceFragement()
            val bundle = Bundle().apply {
                putLong("ID", tripID)
            }
            fragment.arguments = bundle
            return fragment
        }
    }
}
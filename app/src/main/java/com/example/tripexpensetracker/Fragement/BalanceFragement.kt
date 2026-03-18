package com.example.expensetrackerapp.Fragement

import android.content.Context.MODE_PRIVATE
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
import com.example.expensetrackerapp.KT_DataClass.Balance
import com.example.tripexpensetracker.R
import com.example.expensetrackerapp.RetrofitClient
import com.example.tripexpensetracker.TripBalanceViewModel
import com.google.firebase.database.*
import kotlinx.coroutines.*

class BalanceFragement : Fragment() {

    private val Users_BIN_ID = "691c63b7d0ea881f40f00aea"
    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2

    private var tripID: Long = 0L
    private lateinit var userList: RecyclerView

    private val viewModel: TripBalanceViewModel by activityViewModels()

    // Flag to know if current user is test account
    private var isTestUser: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_balance_fragement, container, false)
        userList = view.findViewById(R.id.recycleBalance)
        userList.layoutManager = LinearLayoutManager(requireContext())

        tripID = arguments?.getLong("ID") ?: 0L

        // Check if current user is test account
        val prefs = requireContext().getSharedPreferences("auth", MODE_PRIVATE)
        isTestUser = prefs.getBoolean("isTempUser", false)
        Log.d("BalanceFragment", "tripID: $tripID | isTestUser: $isTestUser")

        if (tripID == 0L) {
            Toast.makeText(requireContext(), "Trip ID missing", Toast.LENGTH_SHORT).show()
        } else {
            // Load data – from Firebase if test user, else from API
            loadBalances(tripID)

            // Observe ViewModel updates (still works for both cases)
            viewModel.balances.observe(viewLifecycleOwner) { updatedBalances ->
                val filtered = updatedBalances.filter { it.tripId == tripID }
                (userList.adapter as? BalanceAdapter)?.submitList(filtered)

                if (filtered.isEmpty() && !isTestUser) {
                    Toast.makeText(requireContext(), "No members yet", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    private fun loadBalances(tripId: Long) {
        if (isTestUser) {
            fetchBalancesFromFirebase(tripId)
        } else {
            fetchBalancesFromApi(tripId)
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Fetch balances from Firebase Realtime Database for test users
    // ───────────────────────────────────────────────────────────────
    private fun fetchBalancesFromFirebase(tripId: Long) {
        val database = FirebaseDatabase.getInstance()
        val balanceRef = database.getReference("userBalance").child(tripId.toString())

        balanceRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val balances = mutableListOf<Balance>()

                for (child in snapshot.children) {
                    val balance = child.getValue(Balance::class.java)
                    if (balance != null) {
                        balances.add(balance)
                    }
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    if (!isAdded) return@launch

                    val adapter = BalanceAdapter()
                    adapter.submitList(balances)
                    userList.adapter = adapter

                    // Push to ViewModel for consistency
                    viewModel.setBalancesManually(balances)

                    if (balances.isEmpty()) {
                        Toast.makeText(requireContext(), "No shared balances yet", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                lifecycleScope.launch(Dispatchers.Main) {
                    if (!isAdded) return@launch
                    Toast.makeText(requireContext(), "Failed to load shared balances", Toast.LENGTH_LONG).show()
                }
                Log.e("BalanceFragment", "Firebase error", error.toException())
            }
        })
    }

    // ───────────────────────────────────────────────────────────────
    // Original API fetch (used only for normal users)
    // ───────────────────────────────────────────────────────────────
    private fun fetchBalancesFromApi(tripId: Long) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getBalance(Users_BIN_ID, API_KEY)
                val allUsers = response.record.userBalance ?: emptyList()
                val filteredUsers = allUsers.filter { it.tripId == tripId }

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    val adapter = BalanceAdapter()
                    adapter.submitList(filteredUsers)
                    userList.adapter = adapter

                    // Also push to ViewModel
                    viewModel.setBalancesManually(filteredUsers)

                    if (filteredUsers.isEmpty()) {
                        Toast.makeText(requireContext(), "No members yet", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    Log.e("BalanceFragment", "API error", e)
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
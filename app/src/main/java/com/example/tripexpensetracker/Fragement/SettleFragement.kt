package com.example.expensetrackerapp.Fragement

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetrackerapp.Adapter.SettleAdapter
import com.example.expensetrackerapp.KT_DataClass.Balance
import com.example.expensetrackerapp.RetrofitClient
import com.example.tripexpensetracker.databinding.FragmentSettleFragementBinding
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class SettleFragement : Fragment() {

    private var _binding: FragmentSettleFragementBinding? = null
    private val binding get() = _binding!!

    private val BIN_ID = "691c63b7d0ea881f40f00aea"
    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2

    private var tripId: Long = 0L
    private lateinit var adapter: SettleAdapter

    // Flag to know if current user is test account
    private var isTestUser: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tripId = arguments?.getLong("ID") ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettleFragementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if current user is test account
        val prefs = requireContext().getSharedPreferences("auth", MODE_PRIVATE)
        isTestUser = prefs.getBoolean("isTempUser", false)
        Log.d("SettleFragment", "tripId: $tripId | isTestUser: $isTestUser")

        setupRecyclerView()

        if (tripId == 0L) {
            Toast.makeText(requireContext(), "Trip ID missing", Toast.LENGTH_SHORT).show()
            return
        }

        fetchSettlements(tripId)
    }

    private fun setupRecyclerView() {
        adapter = SettleAdapter()
        binding.recycleBalance.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SettleFragement.adapter
        }
    }

    private fun fetchSettlements(tripId: Long) {
        if (isTestUser) {
            fetchSettlementsFromFirebase(tripId)
        } else {
            fetchSettlementsFromApi(tripId)
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Fetch settlements from Firebase Realtime Database (for test users)
    // ───────────────────────────────────────────────────────────────
    private fun fetchSettlementsFromFirebase(tripId: Long) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val database = FirebaseDatabase.getInstance()
                val balanceRef = database.getReference("userBalance").child(tripId.toString())

                val snapshot = balanceRef.get().await()

                val settlements = mutableListOf<Balance>()

                for (child in snapshot.children) {
                    val balance = child.getValue(Balance::class.java)
                    if (balance != null && balance.balance != 0) {
                        settlements.add(balance)
                    }
                }

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    if (settlements.isEmpty()) {
                        Toast.makeText(requireContext(), "No settlements for this trip (shared data)", Toast.LENGTH_LONG).show()
                    }

                    adapter.updateList(settlements)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    Log.e("SettleFragment", "Firebase fetch error", e)
                    Toast.makeText(requireContext(), "Failed to load shared settlements", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Original API fetch (used only for normal users)
    // ───────────────────────────────────────────────────────────────
    private fun fetchSettlementsFromApi(tripId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getBalance(BIN_ID, API_KEY)

                Log.d("SettleDebug", "Full Response: $response")
                Log.d("SettleDebug", "userBalance field: ${response.record.userBalance}")

                val allBalances = response.record.userBalance ?: emptyList()
                Log.d("SettleDebug", "Total items: ${allBalances.size}")

                val filtered = allBalances.filter { it.tripId == tripId && it.balance != 0 }
                Log.d("SettleDebug", "Filtered for trip $tripId: ${filtered.size} items")

                if (filtered.isEmpty()) {
                    Toast.makeText(requireContext(), "No data for tripId: $tripId", Toast.LENGTH_LONG).show()
                }

                adapter.updateList(filtered)

            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("Error", "Error: ${e.message}")
                // Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(tripID: Long): SettleFragement {
            return SettleFragement().apply {
                arguments = Bundle().apply {
                    putLong("ID", tripID)
                }
            }
        }
    }
}
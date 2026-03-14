package com.example.expensetrackerapp.Fragement

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
import kotlinx.coroutines.launch

class SettleFragement : Fragment() {

    private var _binding: FragmentSettleFragementBinding? = null
    private val binding get() = _binding!!

    private val BIN_ID = "691c63b7d0ea881f40f00aea"
    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2

    private var tripId: Long = 0L
    private lateinit var adapter: SettleAdapter

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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getBalance(BIN_ID, API_KEY)

                android.util.Log.d("SettleDebug", "Full Response: $response")
                android.util.Log.d("SettleDebug", "userBalance field: ${response.record.userBalance}")

                val allBalances = response.record.userBalance ?: emptyList()
                android.util.Log.d("SettleDebug", "Total items: ${allBalances.size}")

                val filtered = allBalances.filter { it.tripId == tripId && it.balance != 0 }
                android.util.Log.d("SettleDebug", "Filtered for trip $tripId: ${filtered.size} items")

                if (filtered.isEmpty()) {
                    Toast.makeText(requireContext(), "No data for tripId: $tripId", Toast.LENGTH_LONG).show()
                }

                adapter.updateList(filtered)

            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("Error","Error : ${e.message}")
//                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
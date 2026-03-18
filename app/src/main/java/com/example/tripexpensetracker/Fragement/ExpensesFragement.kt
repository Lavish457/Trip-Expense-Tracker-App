package com.example.expensetrackerapp.Fragement

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
import com.example.expensetrackerapp.Adapter.ExpenseAdapter
import com.example.expensetrackerapp.KT_DataClass.Expense
import com.example.expensetrackerapp.RetrofitClient
import com.example.tripexpensetracker.databinding.FragmentExpensesFragementBinding
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class ExpensesFragement : Fragment() {

    private var _binding: FragmentExpensesFragementBinding? = null
    private val binding get() = _binding!!

    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2
    private val EXPENSE_BIN_ID = "691c6bf643b1c97be9b51a62"

    private var tripID: Long = 0L
    private lateinit var adapter: ExpenseAdapter

    // Flag to know if current user is test account
    private var isTestUser: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tripID = arguments?.getLong("ID") ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpensesFragementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if current user is test account
        val prefs = requireContext().getSharedPreferences("auth", MODE_PRIVATE)
        isTestUser = prefs.getBoolean("isTempUser", false)
        Log.d("ExpensesFragment", "tripID: $tripID | isTestUser: $isTestUser")

        setupRecyclerView()

        if (tripID == 0L) {
            Toast.makeText(requireContext(), "Invalid Trip ID", Toast.LENGTH_SHORT).show()
            return
        }

        fetchExpenses(tripID)
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter()
        binding.recycleBalance.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ExpensesFragement.adapter
        }
    }

    private fun fetchExpenses(tripId: Long) {
        if (isTestUser) {
            fetchExpensesFromFirebase(tripId)
        } else {
            fetchExpensesFromApi(tripId)
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Fetch expenses from Firebase Realtime Database (for test users)
    // ───────────────────────────────────────────────────────────────
    private fun fetchExpensesFromFirebase(tripId: Long) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val database = FirebaseDatabase.getInstance()
                val expenseRef = database.getReference("userExpense").child(tripId.toString())

                val snapshot = expenseRef.get().await()

                val expenses = mutableListOf<Expense>()

                for (child in snapshot.children) {
                    val expense = child.getValue(Expense::class.java)
                    if (expense != null) {
                        expenses.add(expense)
                    }
                }

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    if (expenses.isEmpty()) {
                        Toast.makeText(requireContext(), "No expenses for this trip (shared data)", Toast.LENGTH_SHORT).show()
                    }

                    adapter.updateExpenses(expenses)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    Log.e("ExpensesFragment", "Firebase fetch error", e)
                    Toast.makeText(requireContext(), "Failed to load shared expenses", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Original API fetch (used only for normal users)
    // ───────────────────────────────────────────────────────────────
    private fun fetchExpensesFromApi(tripId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getExpense(EXPENSE_BIN_ID, API_KEY)
                val allExpenses = response.record.expenseBalance ?: emptyList()

                val filteredExpenses = allExpenses.filter { it.tripId == tripId }

                if (filteredExpenses.isEmpty()) {
                    Toast.makeText(requireContext(), "No expenses for this trip", Toast.LENGTH_SHORT).show()
                }

                adapter.updateExpenses(filteredExpenses)

            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("Error", "Error: ${e.message}")
                // Toast.makeText(requireContext(), "Failed to load expenses: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(tripID: Long): ExpensesFragement {
            return ExpensesFragement().apply {
                arguments = Bundle().apply {
                    putLong("ID", tripID)
                }
            }
        }
    }
}
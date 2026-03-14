// File: Adapter/ExpenseAdapter.kt
package com.example.expensetrackerapp.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.KT_DataClass.Expense
import com.example.tripexpensetracker.databinding.ExpenseListBinding
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAdapter(
    private var expenseList: List<Expense> = emptyList()
) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

    fun updateExpenses(newList: List<Expense>) {
        expenseList = newList
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ExpenseListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ExpenseListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val expense = expenseList[position]
        with(holder.binding) {
            expenseName.text = expense.expenseName
            expenseTotalAmount.text = "₹ ${expense.expenseAmount}"
            payeeName.text = expense.payeeName

            val total = expense.expenseAmount.replace(",", "").toDoubleOrNull() ?: 0.0
            val count = expense.members?.size ?: 1
            val avg = total / count
            expenseAvgAmount.text = "₹ ${String.format("%.2f", avg)} each"

            totalMember.text = "Split among $count member${if (count != 1) "s" else ""}"

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dateTXT.text = sdf.format(Date(expense.expenseDate))
        }
    }

    override fun getItemCount() = expenseList.size
}
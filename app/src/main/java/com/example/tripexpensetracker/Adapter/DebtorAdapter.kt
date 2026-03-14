package com.example.expensetrackerapp.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.KT_DataClass.Balance
import com.example.tripexpensetracker.R

class DebtorAdapter(
    private val context: Context,
    private val debtors: List<Balance>
) : RecyclerView.Adapter<DebtorAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val debtorName: TextView = itemView.findViewById(R.id.debtorName)
        val debtorTrip: TextView = itemView.findViewById(R.id.debtorTrip)
        val debtorTripName: TextView = itemView.findViewById(R.id.debtorTripName)
        val tripExpense: TextView = itemView.findViewById(R.id.tripExpense)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.all_debtors, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val debtor = debtors[position]

        holder.debtorName.text = debtor.memberName
        holder.debtorTrip.text = "From ${debtor.tripId}"
        holder.debtorTripName.text = (debtor.tripId ?: "Unknown Trip").toString()

        holder.tripExpense.text = "${String.format("%,d", debtor.balance)}"
    }

    override fun getItemCount() = debtors.size
}
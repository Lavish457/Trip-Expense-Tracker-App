package com.example.expensetrackerapp.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.KT_DataClass.Balance
import com.example.tripexpensetracker.R

class CreditorAdapter(
    private val context: Context,
    private val creditors: MutableList<Balance>
) : RecyclerView.Adapter<CreditorAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val creditorName: TextView = itemView.findViewById(R.id.creditorName)
        val creditorTrip: TextView = itemView.findViewById(R.id.creditorTrip)
        val creditorTripName: TextView = itemView.findViewById(R.id.creditorTripName)
        val tripExpense: TextView = itemView.findViewById(R.id.tripExpense)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.all_creditors, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val creditor = creditors[position]

        holder.creditorName.text = creditor.memberName ?: "Unknown"

        holder.creditorTrip.text = "From ${creditor.tripId}"

        holder.creditorTripName.text = (creditor.tripId ?: "Unknown Trip").toString()

        val amountInPaise = kotlin.math.abs(creditor.balance) // Remove negative sign
        val amountInRupees = amountInPaise

        holder.tripExpense.text = amountInRupees.toString()
    }

    override fun getItemCount() = creditors.size
}
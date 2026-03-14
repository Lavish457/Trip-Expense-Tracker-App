package com.example.expensetrackerapp.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.KT_DataClass.Trips
import com.example.tripexpensetracker.R
import com.example.expensetrackerapp.Activity.TripDetail
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripAdapter(private val context: Context,private val trips: List<Trips>) :
    RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name = itemView.findViewById<TextView>(R.id.tripName)
        val detail = itemView.findViewById<TextView>(R.id.tripDetail)
        val expense = itemView.findViewById<TextView>(R.id.tripExpense)
        val members = itemView.findViewById<TextView>(R.id.members)
        val date = itemView.findViewById<TextView>(R.id.date)
        val totalExpense = itemView.findViewById<TextView>(R.id.totalExpense)
        val tripCard = itemView.findViewById<MaterialCardView>(R.id.tripCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.all_trips, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = trips[position]

        holder.name.text = trip.tripName ?: ""
        holder.detail.text = trip.tripDescription ?: ""
        holder.expense.text = trip.totalAmount?.toString() ?: "0"
        holder.totalExpense.text = "${trip.expenseCount ?: 0} expenses"
        holder.members.findViewById<TextView>(R.id.members).text =
            "${trip.totalMembers ?: 0} members"
        holder.tripCard.setOnClickListener()
        {
            var intent = Intent(context, TripDetail::class.java)
            intent.putExtra("tripId", trip.tripID)
            context.startActivity(intent)
        }
        holder.date.findViewById<TextView>(R.id.date).text =
            convertStamp(trip.tripDatetimeStamp ?: 0L)
    }

    override fun getItemCount() = trips.size

    private fun convertStamp(timestamp: Long): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
}

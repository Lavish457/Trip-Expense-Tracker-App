package com.example.expensetrackerapp.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.KT_DataClass.Balance
import com.example.tripexpensetracker.R

class BalanceAdapter : ListAdapter<Balance, BalanceAdapter.ViewHolder>(DiffCallback()) {


    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.balance_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userTripName = itemView.findViewById<TextView>(R.id.userTripName)
        private val paidAmount = itemView.findViewById<TextView>(R.id.paidAmount)
        private val creditAmount = itemView.findViewById<TextView>(R.id.creditAmount)
        private val balanceAmount = itemView.findViewById<TextView>(R.id.balanceAmount)

        fun bind(user: Balance) {
            userTripName.text = user.memberName
            paidAmount.text = "₹ ${user.paidAmount}"
            creditAmount.text = "₹ ${user.creditAmount}"

            val amount = user.balance
            val displayAmount = kotlin.math.abs(amount)
            balanceAmount.text = "₹ $displayAmount"

            balanceAmount.setTextColor(if (amount < 0) Color.RED else Color.GREEN)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Balance>() {
        override fun areItemsTheSame(oldItem: Balance, newItem: Balance): Boolean =
            oldItem.memberName == newItem.memberName

        override fun areContentsTheSame(oldItem: Balance, newItem: Balance): Boolean =
            oldItem == newItem
    }
}
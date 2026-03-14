// File: Adapter/SettleAdapter.kt
package com.example.expensetrackerapp.Adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.KT_DataClass.Balance
import com.example.expensetrackerapp.KT_DataClass.BalanceListResponse
import com.example.expensetrackerapp.KT_DataClass.BalanceListWrapper
import com.example.tripexpensetracker.R
import com.example.expensetrackerapp.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettleAdapter : RecyclerView.Adapter<SettleAdapter.ViewHolder>() {

    private var balanceList: List<Balance> = emptyList()

    private val BIN_ID = "691c63b7d0ea881f40f00aea"
    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2

    fun updateList(newList: List<Balance>) {
        balanceList = newList
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.settlementUserName)
        val userAmount: TextView = itemView.findViewById(R.id.settlementUserAmount)
        val markAsPaidButton: LinearLayout = itemView.findViewById(R.id.markAsPaidLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.settlement_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val balance = balanceList[position]

        holder.userName.text = balance.memberName
        val absAmount = kotlin.math.abs(balance.balance.toDouble())
        holder.userAmount.text = "₹ ${String.format("%.2f", absAmount)}"

        if (balance.balance > 0) {
            holder.userName.setTextColor(Color.parseColor("#2e8b57"))
            holder.userAmount.setTextColor(Color.parseColor("#2e8b57"))
        } else if (balance.balance < 0) {
            holder.userName.setTextColor(Color.parseColor("#e0442d"))
            holder.userAmount.setTextColor(Color.parseColor("#e0442d"))
        } else {
            holder.userName.setTextColor(Color.parseColor("#000000"))
            holder.userAmount.setTextColor(Color.parseColor("#7c3bed"))
        }

        holder.markAsPaidButton.visibility = if (balance.balance == 0) GONE else VISIBLE

        holder.markAsPaidButton.setOnClickListener {
            val lifecycleOwner = holder.itemView.findViewTreeLifecycleOwner() ?: return@setOnClickListener

            lifecycleOwner.lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.getBalance(BIN_ID, API_KEY)
                    val currentList = response.record.userBalance?.toMutableList() ?: mutableListOf()

                    val index = currentList.indexOfFirst {
                        it.memberId == balance.memberId && it.tripId == balance.tripId
                    }

                    if (index != -1) {
                        val user = currentList[index].copy()

                        if (user.balance > 0) {
                            user.paidAmount -= user.balance
                        } else if (user.balance < 0) {
                            user.paidAmount += kotlin.math.abs(user.balance)
                        }
                        user.balance = 0

                        currentList[index] = user
                    }

                    val wrapper = BalanceListWrapper(currentList)
                    RetrofitClient.instance.updateBalance(
                        binId = BIN_ID,
                        apiKey = API_KEY,
                        wrapper = wrapper
                    )

                    withContext(Dispatchers.Main) {
                        Toast.makeText(holder.itemView.context, "${balance.memberName} marked as paid!", Toast.LENGTH_SHORT).show()
                        balanceList = balanceList.map {
                            if (it.memberId == balance.memberId && it.tripId == balance.tripId) {
                                it.copy(balance = 0)
                            } else it
                        }
                        notifyDataSetChanged()
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(holder.itemView.context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun getItemCount() = balanceList.size
}
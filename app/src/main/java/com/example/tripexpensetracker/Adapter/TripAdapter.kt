package com.example.expensetrackerapp.Adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.KT_DataClass.Balance
import com.example.expensetrackerapp.KT_DataClass.Expense
import com.example.expensetrackerapp.KT_DataClass.Trips
import com.example.expensetrackerapp.KT_DataClass.User
import com.example.expensetrackerapp.KT_DataClass.UserListWrapper
import com.example.tripexpensetracker.R
import com.example.expensetrackerapp.Activity.TripDetail
import com.example.expensetrackerapp.RetrofitClient
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripAdapter(
    private val context: Context,
    private val trips: List<Trips>
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    constructor(
        context: Context,
        trips: List<Trips>,
        cachedExpenses: List<Expense>?,
        cachedBalances: List<Balance>?
    ) : this(context, trips)

    private val adapterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2

    private val EXPENSE_BIN_ID   = "691c6bf643b1c97be9b51a62"
    private val BALANCE_BIN_ID   = "691c63b7d0ea881f40f00aea"
    private val TEMP_USER_BIN_ID = "691c6c2643b1c97be9b51ab3"

    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name         = itemView.findViewById<TextView>(R.id.tripName)
        val detail       = itemView.findViewById<TextView>(R.id.tripDetail)
        val expense      = itemView.findViewById<TextView>(R.id.tripExpense)
        val members      = itemView.findViewById<TextView>(R.id.members)
        val date         = itemView.findViewById<TextView>(R.id.date)
        val totalExpense = itemView.findViewById<TextView>(R.id.totalExpense)
        val tripCard     = itemView.findViewById<MaterialCardView>(R.id.tripCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.all_trips, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = trips[position]

        holder.name.text         = trip.tripName        ?: ""
        holder.detail.text       = trip.tripDescription ?: ""
        holder.expense.text      = trip.totalAmount?.toString() ?: "0"
        holder.totalExpense.text = "${trip.expenseCount  ?: 0} expenses"
        holder.members.text      = "${trip.totalMembers  ?: 0} members"
        holder.date.text         = convertStamp(trip.tripDatetimeStamp ?: 0L)

        holder.tripCard.setOnClickListener {
            context.startActivity(
                Intent(context, TripDetail::class.java)
                    .putExtra("tripId", trip.tripID)
            )
        }

        holder.tripCard.setOnLongClickListener { view ->
            AlertDialog.Builder(view.context)
                .setTitle("Share Trip")
                .setMessage("Are you sure you want to share this trip?")
                .setPositiveButton("Yes") { _, _ ->
                    adapterScope.launch {
                        shareTripWithFreshData(trips[holder.adapterPosition])
                    }
                }
                .setNegativeButton("No", null)
                .show()
            true
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Fetch fresh data from API, then push to Firebase
    // ─────────────────────────────────────────────────────────────────────────
    private suspend fun shareTripWithFreshData(trip: Trips) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Preparing to share trip…", Toast.LENGTH_SHORT).show()
        }

        try {
            val freshExpenses: List<Expense> = withContext(Dispatchers.IO) {
                try {
                    RetrofitClient.instance.getExpense(EXPENSE_BIN_ID, API_KEY)
                        .record.expenseBalance ?: emptyList()
                } catch (e: Exception) {
                    Log.e("ShareTrip", "Failed to fetch expenses", e)
                    emptyList()
                }
            }

            val freshBalances: List<Balance> = withContext(Dispatchers.IO) {
                try {
                    RetrofitClient.instance.getBalance(BALANCE_BIN_ID, API_KEY)
                        .record.userBalance ?: emptyList()
                } catch (e: Exception) {
                    Log.e("ShareTrip", "Failed to fetch balances", e)
                    emptyList()
                }
            }

            Log.d("ShareTrip", "Fresh: ${freshExpenses.size} expenses, ${freshBalances.size} balances")

            saveTripToFirebase(trip, freshExpenses, freshBalances)

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context,
                    "Failed to share: ${e.localizedMessage ?: "Unknown error"}",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Write trip / expenses / balances to Firebase
    // ─────────────────────────────────────────────────────────────────────────
    private suspend fun saveTripToFirebase(
        trip: Trips,
        allExpenses: List<Expense>,
        allBalances: List<Balance>
    ) {
        val db = FirebaseDatabase.getInstance()

        try {
            // Trip
            db.getReference("tripData")
                .child(trip.tripID.toString())
                .setValue(trip).await()

            // Expenses – wipe stale then re-write
            val tripExpenses = allExpenses.filter { it.tripId == trip.tripID }
            val expRef = db.getReference("userExpense").child(trip.tripID.toString())
            expRef.removeValue().await()
            tripExpenses.forEach { expRef.push().setValue(it).await() }
            Log.d("ShareTrip", "Saved ${tripExpenses.size} expenses")

            // Balances – wipe stale then re-write
            val tripBalances = allBalances.filter { it.tripId == trip.tripID }
            val balRef = db.getReference("userBalance").child(trip.tripID.toString())
            balRef.removeValue().await()
            tripBalances.forEach { balRef.push().setValue(it).await() }
            Log.d("ShareTrip", "Saved ${tripBalances.size} balances")

            withContext(Dispatchers.Main) {
                val msg = buildString {
                    append("Trip shared successfully!")
                    if (tripExpenses.isEmpty()) append("\n(No expenses yet)")
                    if (tripBalances.isEmpty()) append("\n(No balances yet)")
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }

            // ── 3. Upsert test account ─────────────────────────────────────────
            // KEY RULE:
            //   test account memberID  ==  tripData.memberID  ==  userBalance.createrId
            //
            //   Example from your Firebase / API:
            //     tripData.memberID        = 46282915   ← real owner
            //     userBalance.createrId    = 46282915
            //     test account memberID    = 46282915   ← must match (was 22317395, wrong)
            //
            //   We pass trip.memberID here so the guest's saved memberId equals
            //   the owner's id and every fragment filter finds the correct records.
            upsertTestAccount(
                tripName      = trip.tripName,
                tripEmail     = "${trip.tripName}_test@gmail.com",
                ownerMemberId = trip.memberID          // ← always the real owner's ID
            )

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context,
                    "Failed to share trip: ${e.localizedMessage ?: "Unknown error"}",
                    Toast.LENGTH_LONG).show()
            }
            Log.e("ShareTrip", "Error saving trip", e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Create OR force-update the test account.
    //
    //    Whether the account is new or already exists, we ALWAYS set its
    //    memberID = ownerMemberId (= trip.memberID).
    //
    //    This fixes accounts that were previously created with a random id
    //    (e.g. 22317395) so they now carry the correct id (e.g. 46282915).
    // ─────────────────────────────────────────────────────────────────────────
    private suspend fun upsertTestAccount(
        tripName: String,
        tripEmail: String,
        ownerMemberId: Long
    ) {
        withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getTempUser(TEMP_USER_BIN_ID, API_KEY)
                val users = response.record.users.toMutableList()

                val existingIndex = users.indexOfFirst {
                    it.email.equals(tripEmail, ignoreCase = true)
                }

                if (existingIndex != -1) {
                    // Account exists — ALWAYS overwrite memberID with ownerMemberId.
                    // This corrects any account that was created with a wrong random id.
                    users[existingIndex] = users[existingIndex].copy(memberID = ownerMemberId)
                    Log.d("ShareTrip",
                        "Updated existing test account memberID → $ownerMemberId")
                } else {
                    // New account — set memberID = ownerMemberId from the start.
                    users.add(User(tripName, tripEmail, "123456", ownerMemberId))
                    Log.d("ShareTrip",
                        "Created test account: $tripEmail | memberID=$ownerMemberId")
                }

                // Push updated list back to the API bin
                RetrofitClient.instance.updateTempUser(
                    TEMP_USER_BIN_ID, API_KEY,
                    wrapper = UserListWrapper(users)
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Test account ready!\nEmail: $tripEmail\nPassword: 123456",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("ShareTrip", "Failed to upsert test account", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context,
                        "Test account update failed: ${e.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun getItemCount() = trips.size

    private fun convertStamp(timestamp: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
}
package com.example.expensetrackerapp.Activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.expensetrackerapp.BillDetail
import com.example.expensetrackerapp.Fragement.BalanceFragement
import com.example.expensetrackerapp.Fragement.ExpensesFragement
import com.example.expensetrackerapp.Fragement.SettleFragement

import com.example.expensetrackerapp.KT_DataClass.*
import com.example.tripexpensetracker.R
import com.example.expensetrackerapp.RetrofitClient
import com.example.tripexpensetracker.TripBalanceViewModel
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.*

class TripDetail : AppCompatActivity() {

    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2
    private val Trips_BIN_ID = "691875ae43b1c97be9af0b54"
    private val Users_BIN_ID = "691c63b7d0ea881f40f00aea"
    private val Expense_BIN_ID = "691c6bf643b1c97be9b51a62"

    private lateinit var tripName: TextView
    private lateinit var expenseAmount: TextView
    private lateinit var expenseCount: TextView
    private lateinit var memberCount: TextView
    private lateinit var addNewMember: LinearLayout
    private lateinit var addNewExpense: LinearLayout
    private lateinit var tabBalances: MaterialCardView
    private lateinit var tabExpenses: MaterialCardView
    private lateinit var tabSettle: MaterialCardView
    private var payeeList = ArrayList<String>()
    private var tripID: Long = 0
    private var createrId: Long = -1

    private val balanceViewModel: TripBalanceViewModel by lazy {
        ViewModelProvider(this)[TripBalanceViewModel::class.java]
    }

    // Flag to know if current user is test account
    private var isTestUser: Boolean = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_trip_detail)
        hideNavigationBar()

        // ────────────────────────────────────────────────────────────────────
        // FIX: isTestUser must be read from the "auth" prefs (where SignIn
        //      saves it), NOT from "memberId" prefs which only stores the
        //      numeric member ID.
        // ────────────────────────────────────────────────────────────────────
        val authPrefs = getSharedPreferences("auth", MODE_PRIVATE)
        isTestUser = authPrefs.getBoolean("isTempUser", false)

        val memberPrefs = getSharedPreferences("memberId", MODE_PRIVATE)
        createrId = memberPrefs.getLong("memberId", -1L)

        Log.d("TripDetail", "createrId=$createrId | isTestUser=$isTestUser")

        tripID = intent.getLongExtra("tripId", 0L)
        Log.d("TripDetail", "Trip ID: $tripID")

        initViews()
        setupClickListeners()
        supportFragmentManager.beginTransaction()
            .replace(R.id.tabFragmentContainer, BalanceFragement.newInstance(tripID))
            .commit()

        fetchTrip(tripID)

        val generateBill: LinearLayout = findViewById(R.id.generateBill)
        generateBill.setOnClickListener {
            val intent = Intent(this, BillDetail::class.java)
            intent.putExtra("TripID", tripID)
            startActivity(intent)
        }
    }

    private fun initViews() {
        findViewById<ImageView>(R.id.back).setOnClickListener { finish() }

        tabBalances = findViewById(R.id.tabBalances)
        tabExpenses = findViewById(R.id.tabExpenses)
        tabSettle = findViewById(R.id.tabSettle)
        addNewExpense = findViewById(R.id.addNewExpense)
        addNewMember = findViewById(R.id.addNewMember)
        tripName = findViewById(R.id.tripName)
        expenseAmount = findViewById(R.id.expenseAmount)
        expenseCount = findViewById(R.id.expenseCount)
        memberCount = findViewById(R.id.memberCount)
    }

    private fun setupClickListeners() {
        tabBalances.setOnClickListener {
            replaceFragment(BalanceFragement.newInstance(tripID))
            highlightTab(tabBalances)
        }
        tabExpenses.setOnClickListener {
            replaceFragment(ExpensesFragement.newInstance(tripID))
            highlightTab(tabExpenses)
        }
        tabSettle.setOnClickListener {
            replaceFragment(SettleFragement.newInstance(tripID))
            highlightTab(tabSettle)
        }

        addNewExpense.setOnClickListener {
            if (isTestUser) {
                Toast.makeText(this, "You are using a test account. Cannot update data.", Toast.LENGTH_LONG).show()
            } else {
                showAddExpenseDialog()
            }
        }

        addNewMember.setOnClickListener {
            if (isTestUser) {
                Toast.makeText(this, "You are using a test account. Cannot update data.", Toast.LENGTH_LONG).show()
            } else {
                showAddMemberDialog()
            }
        }
    }

    private suspend fun incrementMemberCount(tripId: Long) {
        try {
            val response = RetrofitClient.instance.getTrips(Trips_BIN_ID, API_KEY)
            val tripList = response.record.trips.toMutableList()

            val tripIndex = tripList.indexOfFirst { it.tripID == tripId }
            if (tripIndex != -1) {
                tripList[tripIndex].totalMembers += 1

                RetrofitClient.instance.updateTrips(
                    binId = Trips_BIN_ID,
                    apiKey = API_KEY,
                    wrapper = TripListWrapper(tripList)
                )
            }
        } catch (e: Exception) {
            Log.e("TripDetail", "Failed to update member count", e)
        }
    }

    private suspend fun incrementExpenseCountAndAmount(tripId: Long, amount: Int) {
        try {
            val response = RetrofitClient.instance.getTrips(Trips_BIN_ID, API_KEY)
            val tripList = response.record.trips.toMutableList()

            val tripIndex = tripList.indexOfFirst { it.tripID == tripId }
            if (tripIndex != -1) {
                val trip = tripList[tripIndex]
                trip.expenseCount += 1
                trip.totalAmount += amount

                RetrofitClient.instance.updateTrips(
                    binId = Trips_BIN_ID,
                    apiKey = API_KEY,
                    wrapper = TripListWrapper(tripList)
                )
            }
        } catch (e: Exception) {
            Log.e("TripDetail", "Failed to update expense stats", e)
        }
    }

    private fun showAddMemberDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.new_member_dialog)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val edtName = dialog.findViewById<EditText>(R.id.edtNewMemberName)
        val createBtn = dialog.findViewById<AppCompatButton>(R.id.createMember)
        val closeBtn = dialog.findViewById<ImageView>(R.id.closeDialog)

        closeBtn.setOnClickListener { dialog.dismiss() }

        createBtn.setOnClickListener {
            val name = edtName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Enter member name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val progressDialog = showProgressDialog("Adding member...")

            CoroutineScope(Dispatchers.IO).launch {
                val startTime = System.currentTimeMillis()

                try {
                    val response = RetrofitClient.instance.getBalance(Users_BIN_ID, API_KEY)
                    val list = (response.record.userBalance ?: mutableListOf()).toMutableList()

                    list.add(Balance(name, System.currentTimeMillis(), 0, 0, 0, tripID, createrId))

                    RetrofitClient.instance.updateBalance(
                        Users_BIN_ID,
                        API_KEY,
                        wrapper = BalanceListWrapper(list)
                    )

                    incrementMemberCount(tripID)

                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed < 2000) delay(2000 - elapsed)

                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(this@TripDetail, "$name added!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        fetchTrip(tripID)
                        balanceViewModel.refreshBalances(tripID, createrId)
                    }
                } catch (e: Exception) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed < 2000) delay(2000 - elapsed)

                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(this@TripDetail, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showAddExpenseDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.new_expense_dialog)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val closeDialog = dialog.findViewById<ImageView>(R.id.closeDialog)
        val edtNewName = dialog.findViewById<EditText>(R.id.edtNewName)
        val edtAmount = dialog.findViewById<EditText>(R.id.edtAmount)
        val paidByUser = dialog.findViewById<Spinner>(R.id.paidByUser)
        val categoryValue = dialog.findViewById<Spinner>(R.id.categoryValue)
        val createExpense = dialog.findViewById<AppCompatButton>(R.id.createExpense)

        val categories = listOf("Food", "Transport", "Activities", "Shopping", "Other", "Accommodation")
        categoryValue.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, categories)

        closeDialog.setOnClickListener { dialog.dismiss() }
        loadPayeesAndSetupSpinner(paidByUser)

        createExpense.setOnClickListener {
            val expenseName = edtNewName.text.toString().trim()
            val amountStr = edtAmount.text.toString().trim()
            val paidByName = paidByUser.selectedItem?.toString() ?: ""

            if (expenseName.isEmpty() || amountStr.isEmpty() ||
                paidByName.isEmpty() || paidByName == "No members yet"
            ) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toIntOrNull() ?: run {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val progressDialog = showProgressDialog("Adding expense...")

            CoroutineScope(Dispatchers.IO).launch {
                val startTime = System.currentTimeMillis()

                try {
                    val expenseResponse = RetrofitClient.instance.getExpense(Expense_BIN_ID, API_KEY)
                    val expenseList = (expenseResponse.record.expenseBalance ?: mutableListOf())
                        .filterNotNull()
                        .toMutableList()

                    val membersWithInfo = getMembersWithNameAndId(tripID)
                    if (membersWithInfo.isEmpty()) {
                        val elapsed = System.currentTimeMillis() - startTime
                        if (elapsed < 2000) delay(2000 - elapsed)

                        withContext(Dispatchers.Main) {
                            progressDialog.dismiss()
                            Toast.makeText(
                                this@TripDetail,
                                "Add at least one member first!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@launch
                    }

                    val nameToIdMap = membersWithInfo.associate { it.name to it.id }
                    val paidById = nameToIdMap[paidByName] ?: return@launch

                    val memberCount = membersWithInfo.size
                    val baseAmount = amount / memberCount
                    val remainder = amount % memberCount

                    val membersList = membersWithInfo.mapIndexed { index, member ->
                        val payable = if (index < remainder) baseAmount + 1 else baseAmount
                        expenseMembers(member.id, member.name, payable)
                    }.toMutableList()

                    val newExpense = Expense(
                        tripId = tripID,
                        expenseId = System.currentTimeMillis(),
                        expenseName = expenseName,
                        payeeName = paidByName,
                        expenseDate = System.currentTimeMillis(),
                        expenseAmount = amountStr,
                        category = categoryValue.selectedItem.toString(),
                        members = membersList
                    )

                    expenseList.add(newExpense)
                    RetrofitClient.instance.updateExpense(
                        Expense_BIN_ID,
                        API_KEY,
                        wrapper = ExpenseListWrapper(expenseList)
                    )

                    val balanceResponse = RetrofitClient.instance.getBalance(Users_BIN_ID, API_KEY)
                    val currentBalances =
                        (balanceResponse.record.userBalance ?: mutableListOf()).toMutableList()
                    val balanceMap = currentBalances.associateBy { it.memberId }.toMutableMap()

                    membersWithInfo.forEach { member ->
                        if (!balanceMap.containsKey(member.id)) {
                            balanceMap[member.id] =
                                Balance(member.name, member.id, 0, 0, 0, tripID, createrId)
                        }
                    }

                    val payerBalance = balanceMap.getValue(paidById)
                    payerBalance.paidAmount += amount
                    membersList.forEach { expMember ->
                        balanceMap.getValue(expMember.memberId).apply {
                            creditAmount += expMember.payableAmount
                            balance = paidAmount - creditAmount
                        }
                    }
                    payerBalance.balance = payerBalance.paidAmount - payerBalance.creditAmount

                    RetrofitClient.instance.updateBalance(
                        Users_BIN_ID,
                        API_KEY,
                        wrapper = BalanceListWrapper(balanceMap.values.toMutableList())
                    )

                    incrementExpenseCountAndAmount(tripID, amount)

                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed < 2000) delay(2000 - elapsed)

                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@TripDetail,
                            "Expense added & balances updated!",
                            Toast.LENGTH_LONG
                        ).show()
                        dialog.dismiss()
                        fetchTrip(tripID)
                        balanceViewModel.refreshBalances(tripID, createrId)
                    }
                } catch (e: Exception) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed < 2000) delay(2000 - elapsed)

                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(this@TripDetail, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("TripDetail", "Add expense failed", e)
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showProgressDialog(message: String = "Please wait..."): AlertDialog {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(64, 48, 64, 48)
            setBackgroundColor(Color.WHITE)
        }

        val progress = ProgressBar(this).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 32
            }
        }

        val text = TextView(this).apply {
            text = message
            textSize = 18f
            setTextColor(Color.BLACK)
        }

        container.addView(progress)
        container.addView(text)

        return AlertDialog.Builder(this)
            .setCancelable(false)
            .setView(container)
            .create()
            .apply { show() }
    }

    data class MemberInfo(val id: Long, val name: String)

    private suspend fun getMembersWithNameAndId(tripId: Long): List<MemberInfo> =
        withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getBalance(Users_BIN_ID, API_KEY)
                response.record.userBalance
                    ?.filter { it.tripId == tripId }
                    ?.map { MemberInfo(it.memberId, it.memberName) }
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

    private fun loadPayeesAndSetupSpinner(spinner: Spinner) {
        payeeList.clear()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getBalance(Users_BIN_ID, API_KEY)
                val users =
                    response.record.userBalance?.filter { it.tripId == tripID } ?: emptyList()

                withContext(Dispatchers.Main) {
                    if (users.isEmpty()) {
                        payeeList.add("No members yet")
                        spinner.isEnabled = false
                    } else {
                        payeeList.addAll(users.map { it.memberName })
                        spinner.isEnabled = true
                    }
                    spinner.adapter = ArrayAdapter(
                        this@TripDetail,
                        android.R.layout.simple_list_item_1,
                        payeeList
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    payeeList.add("Failed to load")
                    spinner.adapter = ArrayAdapter(
                        this@TripDetail,
                        android.R.layout.simple_list_item_1,
                        payeeList
                    )
                }
            }
        }
    }

    private fun fetchTrip(tripId: Long) {
        if (isTestUser) {
            fetchTripFromFirebase(tripId)
        } else {
            fetchTripFromApi(tripId)
        }
    }

    private fun fetchTripFromFirebase(tripId: Long) {
        val database = FirebaseDatabase.getInstance()
        val tripRef = database.getReference("tripData").child(tripId.toString())

        tripRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val trip = snapshot.getValue(Trips::class.java)
                runOnUiThread {
                    if (trip != null) {
                        tripName.text = trip.tripName.ifEmpty { "Unnamed Trip" }
                        expenseAmount.text = "₹ ${trip.totalAmount}"
                        expenseCount.text = "${trip.expenseCount} expenses"
                        memberCount.text = "${trip.totalMembers} members"
                    } else {
                        Toast.makeText(
                            this@TripDetail,
                            "Trip not found in shared data",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                runOnUiThread {
                    Toast.makeText(
                        this@TripDetail,
                        "Failed to load shared trip: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                Log.e("TripDetail", "Firebase fetch error", error.toException())
            }
        })
    }

    private fun fetchTripFromApi(tripId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getTrips(Trips_BIN_ID, API_KEY)
                val trip = response.record.trips.find { it.tripID == tripId }
                withContext(Dispatchers.Main) {
                    if (trip != null) {
                        tripName.text = trip.tripName
                        expenseAmount.text = "₹ ${trip.totalAmount}"
                        expenseCount.text = "${trip.expenseCount} expenses"
                        memberCount.text = "${trip.totalMembers} members"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TripDetail, "Network error", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.tabFragmentContainer, fragment)
            .commit()
    }

    private fun highlightTab(selected: MaterialCardView) {
        val tabs = listOf(
            tabBalances to R.id.tvBalance,
            tabExpenses to R.id.tvExpense,
            tabSettle to R.id.tvSettle
        )
        tabs.forEach { (card, textId) ->
            val textView = findViewById<TextView>(textId)
            if (card == selected) {
                card.setCardBackgroundColor(Color.WHITE)
                textView.setTextColor(Color.BLACK)
            } else {
                card.setCardBackgroundColor(Color.parseColor("#f3f4f6"))
                textView.setTextColor(Color.parseColor("#6b7280"))
            }
        }
    }

    private fun hideNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                hide(WindowInsets.Type.systemBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }
}
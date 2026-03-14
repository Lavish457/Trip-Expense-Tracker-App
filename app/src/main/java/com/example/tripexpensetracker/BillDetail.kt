package com.example.expensetrackerapp

import android.content.ContentValues
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.Adapter.BalanceAdapter
import com.example.expensetrackerapp.Adapter.ExpenseAdapter
import com.example.tripexpensetracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillDetail : AppCompatActivity() {

    private var tripId: Long = 0
    private lateinit var download: LinearLayout
    private lateinit var rootLayout: ConstraintLayout

    companion object {
        private val API_KEY1 = "\$2a\$10$"
        private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
        private val API_KEY = API_KEY1 + API_KEY2
        private const val Users_BIN_ID = "691c63b7d0ea881f40f00aea"
        private const val Trips_BIN_ID = "691875ae43b1c97be9af0b54"
        private const val EXPENSE_BIN_ID = "691c6bf643b1c97be9b51a62"
    }

    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var balanceAdapter: BalanceAdapter

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bill_detail)
        hideNavigationBar()
        showDateTime()
        tripId = intent.getLongExtra("TripID", 0)
        Log.d("tripID", tripId.toString())
        val recycleExpenses = findViewById<RecyclerView>(R.id.recycleExpenses)
        recycleExpenses.layoutManager = LinearLayoutManager(this)
        expenseAdapter = ExpenseAdapter(emptyList())
        recycleExpenses.adapter = expenseAdapter

        val recycleMembers = findViewById<RecyclerView>(R.id.recycleMembers)
        recycleMembers.layoutManager = LinearLayoutManager(this)
        balanceAdapter = BalanceAdapter()
        recycleMembers.adapter = balanceAdapter

        var tripID : TextView = findViewById(R.id.tripID)
        tripID.text = "Trip ID : ${tripId}"
        fetchTrip(tripId)
        fetchExpenses(tripId)
        fetchUsers(tripId)
        fetchTotal(tripId)
        findViewById<ImageView>(R.id.close).setOnClickListener { finish() }
        rootLayout = findViewById(R.id.main)
        download  = findViewById(R.id.download)
        download.setOnClickListener()
        {
            findViewById<ImageView>(R.id.close).visibility = View.GONE
            findViewById<LinearLayout>(R.id.download).visibility = View.GONE
            savePdfToDownloads(findViewById(R.id.main))
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun savePdfToDownloads(view: View) {
        val pdfDocument = PdfDocument()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val pageInfo = PdfDocument.PageInfo.Builder(view.measuredWidth, view.measuredHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        view.draw(page.canvas)
        pdfDocument.finishPage(page)
        val fileName = "ExpenseTrackerBill_${System.currentTimeMillis()}.pdf"
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            Toast.makeText(this, "PDF saved to Downloads", Toast.LENGTH_LONG).show()
            finish()
        } else {
            Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_LONG).show()
        }
        pdfDocument.close()
    }


    private fun showDateTime() {
        val currentTime = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd/MM/yyyy 'at' hh:mm:ss a", Locale.getDefault())
        val formattedDateTime = sdf.format(Date(currentTime))

        val dateTimeTextView = findViewById<TextView>(R.id.dateTime)
        dateTimeTextView.text = "Generated on $formattedDateTime"
    }

    private fun fetchTotal(tripId: Long) {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getTrips(Trips_BIN_ID, API_KEY)
                val tripList = response.record.trips.find { it.tripID == tripId }
                withContext(Dispatchers.Main) {
                    if (tripList != null) {
                        var TotalAmount = findViewById<TextView>(R.id.TotalAmount)
                        TotalAmount.text = "₹ ${tripList.totalAmount}"
                    }
                }

            } catch (e: Exception) {
                Log.e("TripDetail", "Failed to update member count", e)
            }
        }

    }

    private fun fetchUsers(tripId: Long) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getBalance(Users_BIN_ID, API_KEY)
                }

                val filteredUsers = (response.record.userBalance ?: emptyList())
                    .filter { it.tripId == tripId }

                withContext(Dispatchers.Main) {
                    if (filteredUsers.isEmpty()) {
                        Toast.makeText(this@BillDetail, "No participants yet", Toast.LENGTH_SHORT).show()
                    }

                    balanceAdapter.submitList(filteredUsers)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BillDetail, "Failed to load participants", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun fetchExpenses(tripId: Long) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getExpense(EXPENSE_BIN_ID, API_KEY)
                }

                val filteredExpenses = (response.record.expenseBalance ?: emptyList())
                    .filter { it.tripId == tripId }

                withContext(Dispatchers.Main) {
                    if (filteredExpenses.isEmpty()) {
                        Toast.makeText(this@BillDetail, "No expenses added yet", Toast.LENGTH_SHORT).show()
                    }
                    expenseAdapter.updateExpenses(filteredExpenses)
                }
            } catch (e: Exception) {
                Toast.makeText(this@BillDetail, "Failed to load expenses", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchTrip(id: Long) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getTrips(Trips_BIN_ID, API_KEY)
                }

                val trip = response.record.trips.find { it.tripID == id }

                withContext(Dispatchers.Main) {
                    if (trip != null) {
                        findViewById<TextView>(R.id.TName).text = trip.tripName
                        findViewById<TextView>(R.id.TDescription).text = trip.tripDescription
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        findViewById<TextView>(R.id.TDate).text = "Date: ${sdf.format(Date(trip.tripDatetimeStamp))}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BillDetail, "Failed to load trip", Toast.LENGTH_LONG).show()
                }
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
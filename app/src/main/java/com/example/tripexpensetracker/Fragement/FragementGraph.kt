package com.example.expensetrackerapp.Fragement

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.expensetrackerapp.KT_DataClass.Trips
import com.example.tripexpensetracker.R
import com.example.expensetrackerapp.RetrofitClient
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class FragementGraph : Fragment() {

    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2
    private val Trips_BIN_ID = "691875ae43b1c97be9af0b54"

    private var createrId: Long = -1L
    private lateinit var totalSpent: TextView
    private lateinit var totalAvg: TextView

    // Flag to know if current user is test account
    private var isTestUser: Boolean = false

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fragement_graph, container, false)

        // Check if current user is test account
        val prefs = requireActivity().getSharedPreferences("auth", Context.MODE_PRIVATE)
        isTestUser = prefs.getBoolean("isTempUser", false)

        createrId = requireActivity()
            .getSharedPreferences("memberId", Context.MODE_PRIVATE)
            .getLong("memberId", -1L)

        Log.d("GraphFragment", "createrId: $createrId | isTestUser: $isTestUser")

        totalSpent = view.findViewById(R.id.spentTotal)
        totalAvg   = view.findViewById(R.id.spentAvg)

        // Use viewLifecycleOwner.lifecycleScope – automatically cancels on onDestroyView()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val filteredTrips = if (isTestUser) {
                    fetchTripsFromFirebase()
                } else {
                    fetchTripsFromApi()
                }

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    val totalSum = filteredTrips.sumOf { it.totalAmount }
                    val userTripCount = filteredTrips.size.toDouble()

                    totalSpent.text = totalSum.toString()
                    totalAvg.text = if (userTripCount > 0) {
                        "%.2f".format(totalSum / userTripCount)
                    } else "0"

                    Log.d("SUM", "Total: $totalSum | Avg: ${totalAvg.text}")

                    val barChart = view.findViewById<BarChart>(R.id.barChart)

                    if (filteredTrips.isEmpty()) {
                        barChart.setNoDataText("No trips created yet")
                        barChart.invalidate()
                        return@withContext
                    }

                    val labels = filteredTrips.map { it.tripName }
                    val entries = filteredTrips.mapIndexed { index, trip ->
                        BarEntry(index.toFloat(), trip.totalAmount.toFloat())
                    }

                    val dataSet = BarDataSet(entries, "Trip Expenses")
                    dataSet.color = Color.parseColor("#7C3BED")
                    dataSet.valueTextSize = 12f

                    val barData = BarData(dataSet)
                    barChart.data = barData

                    barChart.xAxis.apply {
                        valueFormatter = IndexAxisValueFormatter(labels)
                        granularity = 1f
                        position = XAxis.XAxisPosition.BOTTOM
                        labelRotationAngle = -45f
                        textSize = 12f
                        setAvoidFirstLastClipping(true)
                    }

                    barChart.apply {
                        setExtraBottomOffset(20f)
                        setExtraLeftOffset(10f)
                        setExtraRightOffset(10f)
                        axisLeft.textSize = 12f
                        axisRight.isEnabled = false
                        description.isEnabled = false
                        legend.isEnabled = false
                        setFitBars(true)
                        invalidate()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Log.e("GraphFragment", "Failed to load trips", e)
                        // Toast.makeText(requireContext(), "Error loading data", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        return view
    }

    // ───────────────────────────────────────────────────────────────
    // Fetch trips from Firebase Realtime Database (for test users)
    // ───────────────────────────────────────────────────────────────
    private suspend fun fetchTripsFromFirebase(): List<Trips> = withContext(Dispatchers.IO) {
        try {
            val database = FirebaseDatabase.getInstance()
            val tripsRef = database.getReference("tripData")

            val snapshot = tripsRef.get().await()

            val trips = mutableListOf<Trips>()

            for (child in snapshot.children) {
                val trip = child.getValue(Trips::class.java)
                if (trip != null && trip.memberID == createrId) {
                    trips.add(trip)
                }
            }

            trips
        } catch (e: Exception) {
            Log.e("GraphFragment", "Firebase fetch error", e)
            emptyList()
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Original API fetch (used only for normal users)
    // ───────────────────────────────────────────────────────────────
    private suspend fun fetchTripsFromApi(): List<Trips> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.instance.getTrips(Trips_BIN_ID, API_KEY)
            response.record.trips.filter { it.memberID == createrId }
        } catch (e: Exception) {
            Log.e("GraphFragment", "API fetch error", e)
            emptyList()
        }
    }
}
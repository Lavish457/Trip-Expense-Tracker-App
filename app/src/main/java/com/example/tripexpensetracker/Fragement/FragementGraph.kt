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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope   // ← Add this import
import com.example.tripexpensetracker.R
import com.example.expensetrackerapp.RetrofitClient
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragementGraph : Fragment() {

    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2
    private val Trips_BIN_ID = "691875ae43b1c97be9af0b54"

    private var createrId: Long = -1L
    private lateinit var totalSpent: TextView
    private lateinit var totalAvg: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fragement_graph, container, false)

        createrId = requireActivity()
            .getSharedPreferences("memberId", Context.MODE_PRIVATE)
            .getLong("memberId", -1L)

        totalSpent = view.findViewById(R.id.spentTotal)   // note: id was spentTotal, not spentAvg
        totalAvg   = view.findViewById(R.id.spentAvg)

        // Use viewLifecycleOwner.lifecycleScope – automatically cancels on onDestroyView()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getTrips(Trips_BIN_ID, API_KEY)
                val tripList = response.record.trips.toMutableList()
                val filtered = tripList.filter { it.memberID == createrId }

                val totalSum = filtered.sumOf { it.totalAmount }
                val userTripCount = filtered.size.toDouble()   // ← use filtered.size, not tripList.size!

                // Switch to main only once for all UI updates
                withContext(Dispatchers.Main) {
                    totalSpent.text = totalSum.toString()
                    totalAvg.text = if (userTripCount > 0) {
                        "%.2f".format(totalSum / userTripCount)   // better formatting
                    } else "0"

                    Log.d("SUM", "Total: $totalSum | Avg: ${totalAvg.text}")

                    val barChart = view.findViewById<BarChart>(R.id.barChart)

                    if (filtered.isEmpty()) {
                        // Optional: show "No trips" message or empty chart
                        barChart.setNoDataText("No trips created yet")
                        barChart.invalidate()
                        return@withContext
                    }

                    val labels = filtered.map { it.tripName }
                    val entries = filtered.mapIndexed { index, trip ->
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
                    // Safe now – coroutine is cancelled if view is gone
                    if (isAdded) {  // extra safety check (good habit)
                        Log.d("Error", "Error: ${e.message}")
//                        Toast.makeText(requireContext(), "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                Log.e("GraphFragment", "Failed to load trips", e)
            }
        }

        return view
    }
}
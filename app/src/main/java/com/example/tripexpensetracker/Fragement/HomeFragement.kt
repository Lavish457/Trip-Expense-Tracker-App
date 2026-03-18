package com.example.expensetrackerapp.Fragement

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.Activity.SignIn
import com.example.expensetrackerapp.Adapter.TripAdapter
import com.example.expensetrackerapp.KT_DataClass.TripListWrapper
import com.example.expensetrackerapp.KT_DataClass.Trips
import com.example.tripexpensetracker.R
import com.example.expensetrackerapp.RetrofitClient
import com.google.firebase.database.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.random.Random

class HomeFragement : Fragment() {

    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2
    private val BIN_ID = "691875ae43b1c97be9af0b54"

    private lateinit var tripList: RecyclerView
    private var memberId: Long = 0

    // Flag to know if current user is test account
    private var isTestUser: Boolean = false

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_fragement, container, false)
        tripList = view.findViewById(R.id.tripList)

        // Check if current user is test account
        val prefs = requireContext().getSharedPreferences("auth", MODE_PRIVATE)
        isTestUser = prefs.getBoolean("isTempUser", false)

        memberId = requireContext().getSharedPreferences("memberId", MODE_PRIVATE)
            .getLong("memberId", -1L)

        tripList.layoutManager = LinearLayoutManager(requireContext())

        fetchTrips()

        val createNewTrip: LinearLayout = view.findViewById(R.id.createNewTrip)

        createNewTrip.setOnClickListener {
            if (isTestUser) {
                Toast.makeText(
                    requireContext(),
                    "You are using a test account. Cannot create new trips.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                showCreateTripDialog()
            }
        }

        return view
    }

    private fun showCreateTripDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.new_trip_dialog)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val closeDialog: ImageView = dialog.findViewById(R.id.closeDialog)
        val btnCreateTrip: AppCompatButton = dialog.findViewById(R.id.createTrip)
        val edtTripName: EditText = dialog.findViewById(R.id.edtNewTripName)
        val edtTripDescription: EditText = dialog.findViewById(R.id.edtNewTripDescription)

        closeDialog.setOnClickListener { dialog.dismiss() }

        btnCreateTrip.setOnClickListener {
            val name = edtTripName.text.toString().trim()
            val description = edtTripDescription.text.toString().trim()

            if (name.isNotEmpty() && description.isNotEmpty()) {
                dialog.dismiss()
                createTrip(name, description)
            } else {
                Toast.makeText(requireContext(), "Both fields can't be empty", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun createTrip(tripName: String, tripDescription: String) {
        val timeStamp = System.currentTimeMillis()
        val totalAmount = 0
        val expenseCount = 0
        val memberCount = 0

        val loadingDialog = AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setView(ProgressBar(requireContext()).apply {
                isIndeterminate = true
                setPadding(80, 80, 80, 80)
            })
            .setMessage("Creating trip...")
            .create()
            .apply { show() }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            try {
                val response = RetrofitClient.instance.getTrips(BIN_ID, API_KEY)
                val currentTrips = response.record.trips.toMutableList()

                if (currentTrips.any {
                        it.tripName.equals(tripName, ignoreCase = true) &&
                                it.memberID == memberId
                    }) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed < 2000) delay(2000 - elapsed)

                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        Toast.makeText(
                            requireContext(),
                            "You already have a trip named '$tripName'!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val tripId = Random.nextLong(1000000000L, 9999999999L)

                currentTrips.add(
                    Trips(
                        tripName = tripName,
                        tripDescription = tripDescription,
                        totalAmount = totalAmount,
                        expenseCount = expenseCount,
                        totalMembers = memberCount,
                        tripDatetimeStamp = timeStamp,
                        tripID = tripId,
                        memberID = memberId
                    )
                )

                val wrapper = TripListWrapper(currentTrips)
                RetrofitClient.instance.updateTrips(BIN_ID, API_KEY, wrapper = wrapper)

                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 2000) delay(2000 - elapsed)

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(requireContext(), "Trip added successfully!", Toast.LENGTH_SHORT).show()
                    fetchTrips()   // refresh list
                }

            } catch (e: Exception) {
                e.printStackTrace()
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 2000) delay(2000 - elapsed)

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun fetchTrips() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val myTrips = if (isTestUser) {
                    fetchTripsFromFirebase()
                } else {
                    fetchTripsFromApi()
                }

                Log.d("DataTrips", "Found ${myTrips.size} trips for member $memberId")
                Log.d("DataTrips", Gson().toJson(myTrips))

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    // TripAdapter no longer needs cached data – it fetches fresh on share
                    tripList.adapter = TripAdapter(
                        context = requireContext(),
                        trips = myTrips
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "Failed to load trips", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
                if (trip != null && trip.memberID == memberId) {
                    trips.add(trip)
                }
            }

            trips
        } catch (e: Exception) {
            Log.e("HomeFragment", "Firebase fetch error", e)
            emptyList()
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Original API fetch (used only for normal users)
    // ───────────────────────────────────────────────────────────────
    private suspend fun fetchTripsFromApi(): List<Trips> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.instance.getTrips(BIN_ID, API_KEY)
            response.record.trips.filter { it.memberID == memberId }
        } catch (e: Exception) {
            Log.e("HomeFragment", "API fetch error", e)
            emptyList()
        }
    }
}
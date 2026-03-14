package com.example.expensetrackerapp.Fragement

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
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
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random


class HomeFragement : Fragment() {
    private val API_KEY1 = "\$2a\$10$"
    private val API_KEY2 = "hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
    private val API_KEY = API_KEY1 + API_KEY2
    private val BIN_ID = "691875ae43b1c97be9af0b54"
    private lateinit var tripList : RecyclerView
    private var memberId: Long = 0
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        var view = inflater.inflate(R.layout.fragment_home_fragement, container, false)
        tripList = view.findViewById(R.id.tripList)
        memberId = requireContext().getSharedPreferences("memberId", MODE_PRIVATE)
            .getLong("memberId", -1)
        tripList.layoutManager = LinearLayoutManager(requireContext())
        fetchTrips()
        var createNewTrip : LinearLayout = view.findViewById(R.id.createNewTrip)

        createNewTrip.setOnClickListener()
        {
            var dialog = Dialog(requireContext())
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

            closeDialog.setOnClickListener()
            {
                dialog.dismiss()
            }

                btnCreateTrip.setOnClickListener()
                {
                    val name = edtTripName.text.toString().trim()
                    val description = edtTripDescription.text.toString().trim()
                    if(edtTripName.text.toString().trim().isNotEmpty() &&
                        edtTripDescription.text.toString().trim().isNotEmpty()) {
//                        Toast.makeText(requireContext(), "Trip Created: $name", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        createTrip(edtTripName.text.toString(),edtTripDescription.text.toString())
                    }
                    else
                    {
                        Toast.makeText(requireContext(), "Both fields can't be null or empty", Toast.LENGTH_SHORT).show()
                    }

                }


            dialog.show()
        }
        return view
    }


    private fun createTrip(edtTripName: String, edtTripDescription: String) {
        var tripName = edtTripName
        var description = edtTripDescription
        var timeStamp = System.currentTimeMillis()
        var toatlAmount = 0
        var expenseCount = 0
        var memberCount = 0  // Show loading dialog
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setView(ProgressBar(requireContext()).apply {
                isIndeterminate = true
                setPadding(80, 80, 80, 80)
            })
            .setMessage("Creating trip...")
            .create()
            .apply { show() }

        CoroutineScope(Dispatchers.IO).launch {
            val startTime = System.currentTimeMillis()

            try {
                val response = RetrofitClient.instance.getTrips(BIN_ID, API_KEY)
                var currentTrip = response.record.trips.toMutableList()

                if (currentTrip.any {
                        it.tripName.lowercase().equals(edtTripName, ignoreCase = true) &&
                                it.memberID == memberId
                    }) {
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed < 2000) delay(2000 - elapsed)

                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        Toast.makeText(requireContext(), "You already have a trip named '$edtTripName'!", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                Log.d("memberID", "$memberId")
                var tripId = Random.nextLong(1000000000L, 9999999999L)
                Log.d("tripID", "$tripId")

                currentTrip.add(Trips(tripName,description,toatlAmount,expenseCount,memberCount,timeStamp,tripId,memberId))


                val wrapper = TripListWrapper(currentTrip)
                RetrofitClient.instance.updateTrips(
                    BIN_ID, API_KEY, wrapper = wrapper
                )

                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 2000) delay(2000 - elapsed)

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(requireContext(), "Trip added Successful!", Toast.LENGTH_SHORT).show()

                    // IMPORTANT: You probably DON'T want to go to SignIn here
                    // This logs the user out after creating a trip!
                    // startActivity(Intent(requireContext(), SignIn::class.java))
                    // requireActivity().finish()

                    fetchTrips()  // ← better: just refresh the list
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
                val response = RetrofitClient.instance.getTrips(BIN_ID, API_KEY)

                val trips = response.record.trips.filter {
                    Log.d("DataTrips1", "${it.memberID}")
                    Log.d("DataTrips1", "$memberId")
                    it.memberID == memberId
                }

                Log.d("DataTrips", Gson().toJson(trips))
                Log.d("DataTrips", Gson().toJson(response))

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    tripList.adapter = TripAdapter(requireContext(), trips)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    context?.let {
//                        Toast.makeText(it, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.d("Error", "Error: ${e.message}")
                    }
                }
            }
        }
    }

}
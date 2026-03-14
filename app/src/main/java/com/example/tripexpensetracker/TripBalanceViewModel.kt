package com.example.tripexpensetracker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerapp.KT_DataClass.Balance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.expensetrackerapp.RetrofitClient

class TripBalanceViewModel : ViewModel() {

    private val _balances = MutableLiveData<List<Balance>>(emptyList())
    val balances: LiveData<List<Balance>> = _balances

    fun refreshBalances(tripId: Long, createrId: Long) {
        viewModelScope.launch {
            try {
                val list = fetchBalances(tripId)
                _balances.postValue(list)
            } catch (e: Exception) {
                _balances.postValue(emptyList())
            }
        }
    }

    // Optional: allow fragment to push initial data
    fun setBalancesManually(newList: List<Balance>) {
        _balances.value = newList
    }

    private suspend fun fetchBalances(tripId: Long): List<Balance> =
        withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getBalance(
                    "691c63b7d0ea881f40f00aea",
                    "\$2a\$10\$hPDzuJOstFCGQJp/WyXF/OCUkVjzUbrXHE1W6CMVm4jMb.MXdAz92"
                )
                response.record.userBalance
                    ?.filter { it.tripId == tripId }
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
}
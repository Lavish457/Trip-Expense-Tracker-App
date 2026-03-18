package com.example.expensetrackerapp.KT_DataClass

data class Trips(
    val tripName: String = "",
    val tripDescription: String = "",
    var totalAmount: Int = 0,
    var expenseCount: Int = 0,
    var totalMembers: Int = 0,
    val tripDatetimeStamp: Long = 0L,
    val tripID: Long = 0L,
    val memberID: Long = 0L
)

data class TripListWrapper(
    val trips: MutableList<Trips> = mutableListOf()
)

data class TripListResponse(
    val record: TripListWrapper = TripListWrapper()
)

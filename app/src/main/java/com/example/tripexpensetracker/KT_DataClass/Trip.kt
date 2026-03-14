package com.example.expensetrackerapp.KT_DataClass

data class Trips(
    val tripName : String,
    val tripDescription : String,
    var totalAmount : Int,
    var expenseCount : Int,
    var totalMembers : Int,
    val tripDatetimeStamp : Long,
    val tripID : Long,
    val memberID : Long
)


data class TripListWrapper(
    val trips: MutableList<Trips>
)

data class TripListResponse(
    val record: TripListWrapper
)
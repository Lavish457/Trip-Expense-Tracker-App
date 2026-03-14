package com.example.expensetrackerapp.KT_DataClass

data class Balance(
    val memberName : String,
    val memberId : Long,
    var paidAmount : Int,
    var creditAmount : Int,
    var balance : Int,
    val tripId : Long,
    val createrId : Long
)

data class BalanceListWrapper(
    val userBalance : MutableList<Balance>
)

data class BalanceListResponse(
    val record: BalanceListWrapper
)


package com.example.expensetrackerapp.KT_DataClass

data class Balance(
    val memberName: String = "",
    val memberId: Long = 0L,
    var paidAmount: Int = 0,
    var creditAmount: Int = 0,
    var balance: Int = 0,
    val tripId: Long = 0L,
    val createrId: Long = 0L
)

data class BalanceListWrapper(
    val userBalance: MutableList<Balance> = mutableListOf()
)

data class BalanceListResponse(
    val record: BalanceListWrapper = BalanceListWrapper()
)
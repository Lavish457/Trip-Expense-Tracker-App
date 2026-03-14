package com.example.expensetrackerapp.KT_DataClass

data class SettleBalance(
    val memberId : Long,
    val amount : Int
)
data class SettleListWrapper(
    val settleBalance : MutableList<SettleBalance>
)
data class SettleListResponse(
    val record : SettleListWrapper
)

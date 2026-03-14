package com.example.expensetrackerapp.KT_DataClass

data class expenseMembers(
    val memberId: Long,
    val memberName : String,
    val payableAmount: Int
)

data class Expense(
    val tripId: Long,
    val expenseId: Long,
    val expenseName: String,
    val payeeName: String,
    val expenseDate: Long,
    val expenseAmount: String,
    val category: String,
    val members: List<expenseMembers>? = null
)

data class ExpenseListWrapper(
    val expenseBalance: MutableList<Expense>? = null
)

data class ExpenseListResponse(
    val record: ExpenseListWrapper
)
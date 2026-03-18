package com.example.expensetrackerapp.KT_DataClass

data class expenseMembers(
    val memberId: Long = 0L,
    val memberName: String = "",
    val payableAmount: Int = 0
)

data class Expense(
    val tripId: Long = 0L,
    val expenseId: Long = 0L,
    val expenseName: String = "",
    val payeeName: String = "",
    val expenseDate: Long = 0L,
    val expenseAmount: String = "0",
    val category: String = "",
    val members: List<expenseMembers>? = null
)

data class ExpenseListWrapper(
    val expenseBalance: MutableList<Expense>? = null
)

data class ExpenseListResponse(
    val record: ExpenseListWrapper = ExpenseListWrapper()
)

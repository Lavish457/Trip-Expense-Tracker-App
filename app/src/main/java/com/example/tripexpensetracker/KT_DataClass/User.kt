package com.example.expensetrackerapp.KT_DataClass

data class User(
    val name: String,
    val email: String,
    val password: String,
    var memberID : Long
)

data class UserListWrapper(
    val users: MutableList<User>
)

data class UserListResponse(
    val record: UserListWrapper
)



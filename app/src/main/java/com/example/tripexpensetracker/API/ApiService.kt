package com.example.expensetrackerapp.API

import com.example.expensetrackerapp.KT_DataClass.BalanceListResponse
import com.example.expensetrackerapp.KT_DataClass.BalanceListWrapper
import com.example.expensetrackerapp.KT_DataClass.ExpenseListResponse
import com.example.expensetrackerapp.KT_DataClass.ExpenseListWrapper
import com.example.expensetrackerapp.KT_DataClass.SettleListResponse
import com.example.expensetrackerapp.KT_DataClass.SettleListWrapper
import com.example.expensetrackerapp.KT_DataClass.TripListResponse
import com.example.expensetrackerapp.KT_DataClass.TripListWrapper
import com.example.expensetrackerapp.KT_DataClass.UserListResponse
import com.example.expensetrackerapp.KT_DataClass.UserListWrapper
import retrofit2.http.*

interface ApiService {

    // users api
    @GET("b/{bin_id}/latest")
    suspend fun getUsers(
        @Path("bin_id") binId: String = "69171b69d0ea881f40e7f4cd",
        @Header("X-Master-Key") apiKey: String
    ): UserListResponse

    @PUT("b/{bin_id}")
    suspend fun updateUsers(
        @Path("bin_id") binId: String = "69171b69d0ea881f40e7f4cd",
        @Header("X-Master-Key") apiKey: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body wrapper: UserListWrapper
    ): UserListResponse

    // trips api
    @GET("b/{bin_id}/latest")
    suspend fun getTrips(
        @Path("bin_id") binId: String = "691875ae43b1c97be9af0b54",
        @Header("X-Master-Key") apiKey: String
    ) : TripListResponse

    @PUT("b/{bin_id}")
    suspend fun updateTrips(
        @Path("bin_id") binId: String = "691875ae43b1c97be9af0b54",
        @Header("X-Master-Key") apiKey: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body wrapper: TripListWrapper
    ) : TripListResponse

    // balance api
    @GET("b/{bin_id}/latest")
    suspend fun getBalance(
        @Path("bin_id") binId: String = "691c63b7d0ea881f40f00aea",
        @Header("X-Master-Key") apiKey: String
    ) : BalanceListResponse

    @PUT("b/{bin_id}")
    suspend fun updateBalance(
        @Path("bin_id") binId: String = "691c63b7d0ea881f40f00aea",
        @Header("X-Master-Key") apiKey: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body wrapper: BalanceListWrapper
    ) : BalanceListResponse

    // expense api
    @GET("b/{bin_id}/latest")
    suspend fun getExpense(
        @Path("bin_id") binId: String = "691c6bf643b1c97be9b51a62",
        @Header("X-Master-Key") apiKey: String
    ) : ExpenseListResponse

    @PUT("b/{bin_id}")
    suspend fun updateExpense(
        @Path("bin_id") binId: String = "691c6bf643b1c97be9b51a62",
        @Header("X-Master-Key") apiKey: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body wrapper: ExpenseListWrapper
    ) : ExpenseListResponse

    // settle balance api
    @GET("b/{bin_id}/latest")
    suspend fun getSettleBalance(
        @Path("bin_id") binId: String = "691c6c2643b1c97be9b51ab3",
        @Header("X-Master-Key") apiKey: String
    ) : SettleListResponse

    @PUT("b/{bin_id}")
    suspend fun updateSettleBalance(
        @Path("bin_id") binId: String = "691c6c2643b1c97be9b51ab3",
        @Header("X-Master-Key") apiKey: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body wrapper: SettleListWrapper
    ) : SettleListResponse
}

//{
//    "record":
//    {
//        "trip detail" :
//        {
//            {
//                "tripId" : ,
//                "expenseCount" : ,
//                "memberCount" : ,
//                "tripMembers" : [
//                {
//                    "memberId" : ,
//                    "memberName" : ,
//                    "paidAmount" : ,
//                    "creditAmount" : ,
//                    "BalanceAmount" :
//                }
//                ]
//                "expenses" : [
//                {
//                    "amount" : ,
//                    "expenseId" : ,
//                    "expenseName" : ,
//                    "date" : ,
//                    "members" : [
//                    {
//                        "memberId" : ,
//                        "tripId" : ,
//                        "payableAmount" : ,
//                    }
//                    ]
//                    "payeeId"("memberId") : ,
//                }
//                ],
//                "pureBalance" :
//                [
//                    "memberId" : ,
//                    "amount" : ,
//                ]
//            }
//        }
//    }
//}
package com.themathguild.mytuitionmanager

data class DailySpent(
    val id: Long,
    val date: String,
    val description: String,
    val amount: Int
)

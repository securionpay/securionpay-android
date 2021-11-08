package com.securionpay.data.model.token

data class ThreeDInfo(
    val amount: Int,
    val currency: String,
    val enrolled: Boolean,
    val liabilityShift: String?,
    val version: String
)
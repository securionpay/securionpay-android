package com.securionpay.data.model.pay

import com.google.gson.annotations.SerializedName

data class ChargeResult(
    val customer: Customer,
    @SerializedName("chargeId")
    val id: String?,
    val subscriptionId: String?
)
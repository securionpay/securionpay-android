package com.securionpay.data.model.threeD

import com.securionpay.utils.UserAgentGenerator

internal data class ThreeDCheckRequest(
    val amount: Int,
    val currency: String,
    val card: String,
    val paymentUserAgent: String,
    val platform: String = "android"
)
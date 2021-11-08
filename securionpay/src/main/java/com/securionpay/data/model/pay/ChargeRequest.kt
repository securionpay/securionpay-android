package com.securionpay.data.model.pay

internal data class ChargeRequest(
    val key: String,
    val tokenId: String,
    val sessionId: String,
    val checkoutRequest: String,
    val email: String,
    val rememberMe: Boolean,
    val cvc: String?,
    val verificationSmsId: String?,
    val customAmount: Int?
)
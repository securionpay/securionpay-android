package com.securionpay.data.model.token

internal data class SavedTokenRequest(
    private val key: String,
    private val email: String,
    private val paymentUserAgent: String
)
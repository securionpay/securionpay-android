package com.securionpay.data.model.lookup

internal data class LookupRequest(
    private val key: String,
    private val email: String
)
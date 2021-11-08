package com.securionpay.data.model.sms

import com.securionpay.utils.empty

internal data class VerifySMSRequest(
    private val code: String = String.empty
)
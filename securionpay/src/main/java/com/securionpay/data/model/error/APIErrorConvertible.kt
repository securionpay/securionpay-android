package com.securionpay.data.model.error

internal interface APIErrorConvertible {
    fun toAPIError(): APIError
}
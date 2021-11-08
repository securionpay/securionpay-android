package com.securionpay.data.api

import com.securionpay.data.model.error.APIError

data class Result<out T>(val status: Status, val data: T?, val error: APIError?) {
    companion object {
        fun <T> success(data: T?): Result<T> {
            return Result(Status.SUCCESS, data, null)
        }

        fun <T> error(error: APIError, data: T? = null): Result<T> {
            return Result(Status.ERROR, data, error)
        }

        fun <T> cancelled(): Result<T> {
            return Result(Status.SUCCESS, null, null)
        }
    }
}
package com.securionpay.data.model.error

internal class APIErrorResponse(private val error: Error) : APIErrorConvertible {
    class Error(type: APIError.Type = APIError.Type.Unknown, code: APIError.Code = APIError.Code.Unknown, message: String = "") {
        val type: APIError.Type? = APIError.Type.Unknown
        val code: APIError.Code? = APIError.Code.Unknown
        val message: String = ""
    }

    override fun toAPIError(): APIError {
        return if (error.message.startsWith("email:")) {
            APIError(APIError.Type.InvalidRequest, APIError.Code.InvalidEmail, error.message.replace("email: ", ""))
        } else {
            APIError(error.type, error.code ?: APIError.Code.Unknown, error.message)
        }
    }
}
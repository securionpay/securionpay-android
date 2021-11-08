package com.securionpay.data.model.error

import android.content.Context
import com.google.gson.annotations.SerializedName
import com.nsoftware.ipworks3ds.sdk.Warning
import com.securionpay.R

data class APIError(
    val type: Type? = Type.Unknown,
    val code: Code? = Code.Unknown,
    private val message: String? = null
) {
    companion object {
        val unknown: APIError = APIError(
            Type.Unknown,
            Code.Unknown,
            "Unknown error. Try again or contact support."
        )

        val unknownThreeD: APIError = APIError(
            Type.ThreeDSecure,
            Code.Unknown,
            "Unknown 3D Secure Error. Check your SDK integration."
        )

        val invalidCheckoutRequest: APIError = APIError(
            Type.InvalidRequest,
            Code.InvalidCheckoutRequest,
            "Invalid checkout request."
        )

        fun unsupportedValue(value: String): APIError = APIError(
            Type.InvalidRequest,
            Code.UnsupportedValue,
            "Unsupported value: $value"
        )

        fun threeDError(warning: Warning): APIError {
            return when (warning.id) {
                "SW01" -> APIError(Type.ThreeDSecure, Code.DeviceJailbroken, warning.message)
                "SW02" -> APIError(Type.ThreeDSecure, Code.IntegrityTampered, warning.message)
                "SW03" -> APIError(Type.ThreeDSecure, Code.Simulator, warning.message)
                "SW05" -> APIError(Type.ThreeDSecure, Code.OSNotSupported, warning.message)
                else -> APIError.unknownThreeD
            }
        }

        val enrolledCardIsRequired: APIError = APIError(
            Type.InvalidRequest,
            Code.EnrolledCardIsRequired,
            "The charge requires cardholder authentication."
        )

        val successfulLiabilityShiftIsRequired: APIError = APIError(
            Type.InvalidRequest,
            Code.SuccessfulLiabilityShiftIsRequired,
            "The charge requires cardholder authentication."
        )

        val busy: APIError = APIError(
            Type.SDK,
            Code.AnotherOperation,
            "Another task is in progress."
        )
    }

    enum class Type(val value: String) {
        @SerializedName("card_error")
        CardError("card_error"),

        @SerializedName("invalid_request")
        InvalidRequest("invalid_request"),

        @SerializedName("gateway_error")
        GatewayError("gateway_error"),

        @SerializedName("invalid-verification-code")
        InvalidVerificationCode("invalid-verification-code"),

        ThreeDSecure("3d-secure"),
        SDK("sdk"),

        Unknown("unknown")
    }

    enum class Code(val value: String) {
        @SerializedName("invalid_email")
        InvalidEmail("invalid_email"),

        @SerializedName("invalid_number")
        InvalidNumber("invalid_number"),

        @SerializedName("invalid_expiry_month")
        InvalidExpiryMonth("invalid_expiry_month"),

        @SerializedName("invalid_expiry_year")
        InvalidExpiryYear("invalid_expiry_year"),

        @SerializedName("invalid_cvc")
        InvalidCVC("invalid_cvc"),

        @SerializedName("incorrect_cvc")
        IncorrectCVC("incorrect_cvc"),

        @SerializedName("incorrect_zip")
        IncorrectZip("incorrect_zip"),

        @SerializedName("expired_card")
        ExpiredCard("expired_card"),

        @SerializedName("card_declined")
        CardDeclined("card_declined"),

        @SerializedName("insufficient_funds")
        InsufficientFunds("insufficient_funds"),

        @SerializedName("lost_or_stolen")
        LostOrStolen("lost_or_stolen"),

        @SerializedName("suspected_fraud")
        SuspectedFraud("suspected_fraud"),

        @SerializedName("processing_error")
        ProcessingError("processing_error"),

        @SerializedName("blacklisted")
        Blacklisted("blacklisted"),

        @SerializedName("authentication_required")
        AuthenticationRequired("authentication_required"),

        @SerializedName("expired_token")
        ExpiredToken("expired_token"),

        @SerializedName("limit_exceeded")
        LimitExceeded("limit_exceeded"),

        @SerializedName("invalid-verification-code")
        InvalidVerificationCode("invalid-verification-code"),

        @SerializedName("verification-code-required")
        VerificationCodeRequired("verification-code-required"),

        InvalidCheckoutRequest("invalid-checkout-request"),
        UnsupportedValue("unsupported-value"),
        EnrolledCardIsRequired("enrolled-card-is-required"),
        SuccessfulLiabilityShiftIsRequired("successful-liability-shift-is-required"),
        AnotherOperation("another-operation"),

        DeviceJailbroken("device-jailbroken"),
        IntegrityTampered("integrity-tampered"),
        Simulator("simulator"),
        OSNotSupported("os-not-supported"),

        Unknown("unknown");

        companion object {
            operator fun invoke(rawValue: String): Code =
                Code.values()
                    .firstOrNull { it.value == rawValue } ?: Code.Unknown
        }
    }

    fun message(context: Context?): String {
        val context = context ?: return message ?: "Unknown error. Try again or contact support."
        return when (code) {
            Code.InvalidEmail -> context.getString(R.string.invalid_email)
            Code.InvalidNumber -> context.getString(R.string.invalid_number)
            Code.InvalidExpiryMonth -> context.getString(R.string.invalid_expiry_month)
            Code.InvalidExpiryYear -> context.getString(R.string.invalid_expiry_year)
            Code.InvalidCVC -> context.getString(R.string.invalid_cvc)
            Code.IncorrectCVC -> context.getString(R.string.incorrect_cvc)
            Code.IncorrectZip -> context.getString(R.string.incorrect_zip)
            Code.ExpiredCard -> context.getString(R.string.expired_card)
            Code.InsufficientFunds -> context.getString(R.string.insufficient_funds)
            Code.LostOrStolen -> context.getString(R.string.lost_or_stolen)
            Code.SuspectedFraud -> context.getString(R.string.suspected_fraud)
            Code.ProcessingError -> context.getString(R.string.processing_error)
            Code.Blacklisted -> context.getString(R.string.blacklisted)
            Code.ExpiredToken -> context.getString(R.string.expired_token)
            Code.LimitExceeded -> context.getString(R.string.limit_exceeded)
            Code.DeviceJailbroken -> context.getString(R.string.threed_error_jailbroken)
            Code.IntegrityTampered -> context.getString(R.string.threed_error_integrity_tampered)
            Code.Simulator -> context.getString(R.string.threed_error_simulator)
            Code.OSNotSupported -> context.getString(R.string.threed_error_osnotsupported)
            Code.Unknown -> context.getString(R.string.unknown_error)
            null -> message ?: context.getString(R.string.unknown_error)
            else -> message ?: context.getString(R.string.unknown_error)
        }
    }
}
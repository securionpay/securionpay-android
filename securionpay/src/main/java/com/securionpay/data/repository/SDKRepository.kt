package com.securionpay.data.repository

import com.securionpay.BuildConfig
import com.securionpay.SecurionPay
import com.securionpay.data.api.APIService
import com.securionpay.data.api.RequestBuilder
import com.securionpay.data.api.Result
import com.securionpay.data.model.checkoutRequestDetails.CheckoutDetailsRequest
import com.securionpay.data.model.checkoutRequestDetails.CheckoutRequestDetails
import com.securionpay.data.model.lookup.LookupRequest
import com.securionpay.data.model.lookup.LookupResult
import com.securionpay.data.model.pay.ChargeRequest
import com.securionpay.data.model.pay.ChargeResult
import com.securionpay.data.model.pay.CheckoutRequest
import com.securionpay.data.model.sms.SMS
import com.securionpay.data.model.sms.SendSMSRequest
import com.securionpay.data.model.sms.VerifySMSRequest
import com.securionpay.data.model.sms.VerifySMSResponse
import com.securionpay.data.model.threeD.*
import com.securionpay.data.model.token.SavedTokenRequest
import com.securionpay.data.model.token.Token
import com.securionpay.data.model.token.TokenRequest
import com.securionpay.utils.UserAgentGenerator
import com.securionpay.utils.base64

internal class SDKRepository(private val securionPay: SecurionPay) {
    private val responseHandler = ResponseHandler()

    suspend fun createToken(tokenRequest: TokenRequest): Result<Token> {
        val service = service(BuildConfig.API_URL, true)

        return try {
            responseHandler.handleSuccess(service.createToken(tokenRequest))
        } catch (e: Exception) {
            responseHandler.handleException(e)
        }
    }

    suspend fun pay(
        token: Token,
        checkoutRequest: CheckoutRequest,
        email: String,
        remember: Boolean = false,
        cvc: String? = null,
        sms: SMS? = null,
        customAmount: Int? = null
    ): Result<ChargeResult> {
        val service = service(BuildConfig.BACKOFFICE_URL, false)

        return try {
            val details = service.checkoutDetails(
                CheckoutDetailsRequest(
                    securionPay.publicKey,
                    checkoutRequest.content
                )
            )

            val chargeRequest = ChargeRequest(
                key = securionPay.publicKey,
                tokenId = token.id,
                sessionId = details.sessionId,
                checkoutRequest = checkoutRequest.content,
                email = email,
                rememberMe = remember,
                cvc = cvc,
                verificationSmsId = sms?.id,
                customAmount = customAmount
            )
            val result = responseHandler.handleSuccess(service.pay(chargeRequest))
            if (remember) {
                securionPay.emailStorage.addSavedEmail(email)
            }
            result
        } catch (e: Exception) {
            responseHandler.handleException(e)
        }
    }

    suspend fun lookup(email: String): Result<LookupResult> {
        val service = service(BuildConfig.BACKOFFICE_URL, false)
        val lookupRequest = LookupRequest(securionPay.publicKey, email)

        return try {
            responseHandler.handleSuccess(service.lookup(lookupRequest))
        } catch (e: Exception) {
            responseHandler.handleException(e)
        }
    }

    suspend fun savedToken(email: String): Result<Token> {
        val service = service(BuildConfig.BACKOFFICE_URL, false)
        val savedTokenRequest = SavedTokenRequest(securionPay.publicKey, email, "android_sdk")

        return try {
            responseHandler.handleSuccess(service.savedToken(savedTokenRequest))
        } catch (e: Exception) {
            responseHandler.handleException(e)
        }
    }

    suspend fun sendSMS(email: String): Result<SMS> {
        val service = service(BuildConfig.BACKOFFICE_URL, false)
        val sendSMSRequest = SendSMSRequest(securionPay.publicKey, email)

        return try {
            responseHandler.handleSuccess(service.sendSMS(sendSMSRequest))
        } catch (e: Exception) {
            responseHandler.handleException(e)
        }
    }

    suspend fun verifySMS(code: String, sms: SMS): Result<VerifySMSResponse> {
        val service = service(BuildConfig.BACKOFFICE_URL, false)
        val sendSMSRequest = VerifySMSRequest(code)

        return try {
            responseHandler.handleSuccess(service.verifySMS(sms.id, sendSMSRequest))
        } catch (e: Exception) {
            responseHandler.handleException(e)
        }
    }

    suspend fun threeDCheck(
        token: Token,
        amount: Int,
        currency: String
    ): Result<ThreeDCheckResponse> {
        val service = service(BuildConfig.API_URL, true)
        val request = ThreeDCheckRequest(
            amount,
            currency,
            token.id,
            UserAgentGenerator().userAgent()
        )

        return try {
            responseHandler.handleSuccess(service.threeDCheck(request))
        } catch (e: Exception) {
            responseHandler.handleException(e)
        }
    }

    suspend fun threeDAuthorize(
        token: Token,
        authorizationParameters: String
    ): Result<ThreeDAuthResponse> {
        val service = service(BuildConfig.API_URL, true)
        val request = ThreeDAuthRequest(authorizationParameters.base64, token.id)

        return try {
            responseHandler.handleSuccess(service.threeDAuthenticate(request))
        } catch (e: Exception) {
            responseHandler.handleException(e)
        }
    }

    suspend fun threeDChallengeComplete(token: Token): Result<Token> {
        val service = service(BuildConfig.API_URL, true)
        val request = ThreeDChallengeCompleteRequest(token.id)

        return try {
            responseHandler.handleSuccess(service.threeDChallengeComplete(request))
        } catch (e: Exception) {
            responseHandler.handleException(e)
        }
    }

    suspend fun checkoutRequestDetails(checkoutRequest: CheckoutRequest): Result<CheckoutRequestDetails> {
        val service = service(BuildConfig.BACKOFFICE_URL, false)
        val request = CheckoutDetailsRequest(securionPay.publicKey, checkoutRequest.content)

        return try {
            responseHandler.handleSuccess(service.checkoutDetails(request))
        } catch (e: Exception) {
            responseHandler.handleException(e)
        }
    }

    private fun service(url: String, authorize: Boolean): APIService {
        val requestBuilder = RequestBuilder(securionPay.publicKey, url, authorize)
        return requestBuilder.buildService(APIService::class.java)
    }
}
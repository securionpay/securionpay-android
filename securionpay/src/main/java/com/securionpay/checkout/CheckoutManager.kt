package com.securionpay.checkout

import android.app.Activity
import com.securionpay.data.api.Result
import com.securionpay.data.api.Status
import com.securionpay.data.model.error.APIError
import com.securionpay.data.model.lookup.LookupResult
import com.securionpay.data.model.pay.ChargeResult
import com.securionpay.data.model.pay.CheckoutRequest
import com.securionpay.data.model.sms.SMS
import com.securionpay.data.model.sms.VerifySMSResponse
import com.securionpay.data.model.token.Token
import com.securionpay.data.model.token.TokenRequest
import com.securionpay.data.repository.SDKRepository
import com.securionpay.utils.EmailStorage
import com.securionpay.utils.ThreeDManager
import com.securionpay.utils.fromBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class CheckoutManager(
    private val repository: SDKRepository,
    internal val emailStorage: EmailStorage,
    private val signature: String,
    private val trustedAppStores: List<String>?
) {
    private val threeDManager by lazy { ThreeDManager() }

    internal suspend fun pay(
        token: Token,
        checkoutRequest: CheckoutRequest,
        email: String,
        remember: Boolean = false,
        activity: Activity? = null,
        cvc: String? = null,
        sms: SMS? = null,
        customAmount: Int? = null,
        customCurrency: String? = null,
        callback: (Result<ChargeResult>) -> Unit
    ) {
        val checkoutDetailsResult = repository.checkoutRequestDetails(checkoutRequest)
        val checkoutDetails = checkoutDetailsResult.data ?: run {
            callback(Result.error(checkoutDetailsResult.error ?: APIError.unknown, null))
            return@pay
        }

        val computedCustomAmount: Int? = if (checkoutDetails.subscription == null) {
            customAmount
        } else {
            null
        }

        if (!checkoutDetails.threeDSecureRequired) {
            callback(
                repository.pay(
                    token,
                    checkoutRequest,
                    email,
                    remember,
                    cvc,
                    sms,
                    computedCustomAmount
                )
            )
            return
        }

        val threeDCheck =
            repository.threeDCheck(
                token,
                customAmount ?: checkoutRequest.amount,
                customCurrency ?: checkoutRequest.currency
            )
        val threeDCheckData = threeDCheck.data ?: run {
            callback(Result.error(threeDCheck.error ?: APIError.unknown, null))
            return@pay
        }
        val enrolled = threeDCheckData.token?.threeDSecureInfo?.enrolled ?: false

        if (checkoutRequest.requireEnrolledCard && !enrolled) {
            callback(Result.error(APIError.enrolledCardIsRequired, null))
            return
        }

        if (!enrolled) {
            callback(
                repository.pay(
                    token,
                    checkoutRequest,
                    email,
                    remember,
                    cvc,
                    sms,
                    computedCustomAmount
                )
            )
            return
        }

        val threeDActivity = activity ?: run {
            throw Exception("If you want to perform 3DS, activity cannot be null")
            return@pay
        }
        var authorizationParameters: String? = null
        try {
            val warnings = threeDManager.initialize(
                threeDActivity,
                threeDCheckData.token.brand,
                threeDCheckData.directoryServerCertificate,
                threeDCheckData.sdkLicense,
                signature,
                trustedAppStores
            )
            threeDManager.createTransaction(threeDCheckData.version, threeDCheckData.token.brand)
            GlobalScope.launch(Dispatchers.Main) { threeDManager.showProgressDialog() }
            authorizationParameters =
                threeDManager.authenticationRequestParameters()?.authRequest
            if ((authorizationParameters ?: "").isEmpty()) {
                warnings.firstOrNull()?.let {
                    GlobalScope.launch(Dispatchers.Main) {
                        GlobalScope.launch(Dispatchers.Main) { threeDManager.hideProgressDialog() }
                        callback(Result.error(APIError.threeDError(it)))
                    }
                    return@pay
                } ?: run {
                    GlobalScope.launch(Dispatchers.Main) {
                        GlobalScope.launch(Dispatchers.Main) { threeDManager.hideProgressDialog() }
                        callback(Result.error(APIError.unknownThreeD))
                    }
                    return@pay
                }
            }
        } catch (e: Exception) {
            GlobalScope.launch(Dispatchers.Main) { threeDManager.hideProgressDialog() }
            callback(Result.error(APIError.unknownThreeD))
            return@pay
        }

        val threeDAuthentication =
            repository.threeDAuthorize(threeDCheckData.token, authorizationParameters ?: "")
        val ares = threeDAuthentication.data?.ares ?: run {
            GlobalScope.launch(Dispatchers.Main) { threeDManager.hideProgressDialog() }
            callback(Result.error(threeDAuthentication.error ?: APIError.unknown))
            return@pay
        }

        if (ares.transStatus == "C" || ares.transStatus == "D") {
            val response = ares.clientAuthResponse.fromBase64
            threeDManager.startChallenge(response) { success, error ->
                if (error != null) {
                    GlobalScope.launch(Dispatchers.Main) { threeDManager.hideProgressDialog() }
                    callback(Result.error(error, null))
                } else if (!success) {
                    callback(Result.cancelled())
                } else {
                    GlobalScope.launch {
                        val challengeCompleteToken =
                            repository.threeDChallengeComplete(threeDCheckData.token)
                        GlobalScope.launch(Dispatchers.Main) { threeDManager.hideProgressDialog() }
                        when (challengeCompleteToken.status) {
                            Status.SUCCESS -> {
                                if (challengeCompleteToken.data?.threeDSecureInfo?.liabilityShift == "failed" && checkoutRequest.requireSuccessfulLiabilityShiftForEnrolledCard) {
                                    callback(
                                        Result.error(
                                            APIError.successfulLiabilityShiftIsRequired,
                                            null
                                        )
                                    )
                                } else {
                                    val paymentResult = repository.pay(
                                        token,
                                        checkoutRequest,
                                        email,
                                        remember,
                                        cvc,
                                        sms,
                                        computedCustomAmount
                                    )
                                    GlobalScope.launch(Dispatchers.Main) {
                                        callback(
                                            paymentResult
                                        )
                                    }
                                }
                            }
                            Status.ERROR -> {
                                val paymentResult = repository.pay(
                                    token,
                                    checkoutRequest,
                                    email,
                                    remember,
                                    cvc,
                                    sms,
                                    computedCustomAmount
                                )
                                GlobalScope.launch(Dispatchers.Main) { callback(paymentResult) }
                            }
                        }
                    }
                }
            }
        } else if (ares.transStatus == "N" || ares.transStatus == "U" || ares.transStatus == "R") {
            if (checkoutRequest.requireSuccessfulLiabilityShiftForEnrolledCard) {
                callback(Result.error(APIError.successfulLiabilityShiftIsRequired, null))
                GlobalScope.launch(Dispatchers.Main) { threeDManager.hideProgressDialog() }
                return@pay
            } else {
                repository.threeDChallengeComplete(threeDCheckData.token)
                val paymentResult = repository.pay(
                    token,
                    checkoutRequest,
                    email,
                    remember,
                    cvc,
                    sms,
                    computedCustomAmount
                )
                GlobalScope.launch(Dispatchers.Main) { threeDManager.hideProgressDialog() }
                GlobalScope.launch(Dispatchers.Main) { callback(paymentResult) }
            }
        } else {
            repository.threeDChallengeComplete(threeDCheckData.token)
            val paymentResult = repository.pay(
                token,
                checkoutRequest,
                email,
                remember,
                cvc,
                sms,
                computedCustomAmount
            )
            GlobalScope.launch(Dispatchers.Main) { threeDManager.hideProgressDialog() }
            GlobalScope.launch(Dispatchers.Main) { callback(paymentResult) }
        }
    }

    internal suspend fun pay(
        tokenRequest: TokenRequest,
        checkoutRequest: CheckoutRequest,
        email: String,
        remember: Boolean = false,
        activity: Activity? = null,
        cvc: String? = null,
        sms: SMS? = null,
        customAmount: Int? = null,
        customCurrency: String? = null,
        callback: (Result<ChargeResult>) -> Unit
    ) {
        val tokenResult = repository.createToken(tokenRequest)
        val token = tokenResult.data ?: run {
            callback(Result.error(tokenResult.error ?: APIError.unknown, null))
            return@pay
        }
        pay(
            token,
            checkoutRequest,
            email,
            remember,
            activity,
            cvc,
            sms,
            customAmount,
            customCurrency,
            callback
        )
    }

    internal suspend fun lookup(email: String): Result<LookupResult> = repository.lookup(email)

    internal suspend fun checkoutRequestDetails(checkoutRequest: CheckoutRequest) =
        repository.checkoutRequestDetails(checkoutRequest)

    internal suspend fun savedToken(email: String): Result<Token> = repository.savedToken(email)

    internal suspend fun sendSMS(email: String): Result<SMS> = repository.sendSMS(email)

    internal suspend fun verifySMS(code: String, sms: SMS): Result<VerifySMSResponse> =
        repository.verifySMS(code, sms)
}
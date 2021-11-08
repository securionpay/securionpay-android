package com.securionpay.threed

import android.app.Activity
import com.securionpay.data.api.Result
import com.securionpay.data.model.error.APIError
import com.securionpay.data.model.token.Token
import com.securionpay.data.repository.SDKRepository
import com.securionpay.utils.ThreeDManager
import com.securionpay.utils.fromBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

internal class ThreeDAuthenticator(
    private val repository: SDKRepository,
    private val signature: String,
    private val trustedAppStores: List<String>?,
    private val activity: Activity,
    private val callback: (Result<Token>) -> Unit
) {
    private val threeDManager by lazy { ThreeDManager() }

    fun authenticate(
        token: Token,
        amount: Int,
        currency: String
    ) = GlobalScope.launch {
        val threeDCheck = repository.threeDCheck(token, amount, currency)
        val threeDCheckData = threeDCheck.data ?: run {
            executeCallback(Result.error(threeDCheck.error ?: APIError.unknown))
            return@launch
        }
        if (threeDCheckData.token.threeDSecureInfo?.enrolled != true || threeDCheckData.version.first() == '1') {
            executeCallback(Result.success(token))
            return@launch
        }
        progressDialog(true)
        var authorizationParameters: String? = null
        try {
            val warnings = threeDManager.initialize(
                activity,
                threeDCheckData.token.brand,
                threeDCheckData.directoryServerCertificate,
                threeDCheckData.sdkLicense,
                signature,
                trustedAppStores
            )
            threeDManager.createTransaction(
                threeDCheckData.version,
                threeDCheckData.token.brand
            )
            authorizationParameters =
                threeDManager.authenticationRequestParameters()?.authRequest
            if ((authorizationParameters ?: "").isEmpty()) {
                warnings.firstOrNull()?.let {
                    progressDialog(false)
                    executeCallback(Result.error(APIError.threeDError(it)))
                    return@launch
                } ?: run {
                    progressDialog(false)
                    executeCallback(Result.error(APIError.unknownThreeD))
                    return@launch
                }
            }
        } catch (_: Exception) {
            progressDialog(false)
            executeCallback(Result.error(APIError.unknownThreeD))
        }

        val threeDAuthentication =
            repository.threeDAuthorize(threeDCheckData.token, authorizationParameters ?: "")
        val ares = threeDAuthentication.data?.ares ?: run {
            progressDialog(false)
            executeCallback(Result.error(threeDAuthentication.error ?: APIError.unknown))
            return@launch
        }

        if (ares.transStatus == "C" || ares.transStatus == "D") {
            val response = ares.clientAuthResponse.fromBase64
            threeDManager.startChallenge(response) { success, error ->
                if (error != null) {
                    progressDialog(false)
                    executeCallback(Result.error(error))
                } else if (!success) {
                    progressDialog(false)
                    executeCallback(Result.cancelled())
                } else {
                    GlobalScope.launch {
                        repository.threeDChallengeComplete(threeDCheckData.token)
                        progressDialog(false)
                        executeCallback(Result.success(threeDCheckData.token))
                    }
                }
            }
        } else {
            repository.threeDChallengeComplete(threeDCheckData.token)
            progressDialog(false)
            executeCallback(Result.success(threeDCheckData.token))
        }
    }

    private fun executeCallback(result: Result<Token>) {
        GlobalScope.launch(Dispatchers.Main) {
            callback(result)
        }
    }

    private fun progressDialog(show: Boolean) {
        GlobalScope.launch(Dispatchers.Main) {
            if (show) {
                threeDManager.showProgressDialog()
            } else {
                threeDManager.hideProgressDialog()
            }
        }
    }
}
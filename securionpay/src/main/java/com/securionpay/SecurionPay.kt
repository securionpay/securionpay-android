package com.securionpay

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.securionpay.checkout.CheckoutDialogFragment
import com.securionpay.data.api.Result
import com.securionpay.data.model.error.APIError
import com.securionpay.data.model.pay.ChargeResult
import com.securionpay.data.model.pay.CheckoutRequest
import com.securionpay.data.model.token.Token
import com.securionpay.data.model.token.TokenRequest
import com.securionpay.data.repository.SDKRepository
import com.securionpay.threed.ThreeDAuthenticator
import com.securionpay.utils.EmailStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SecurionPay(
    internal val context: Context,
    var publicKey: String,
    var signature: String,
    val trustedAppStores: List<String>? = null
) {
    internal val repository = SDKRepository(this)
    internal val emailStorage = EmailStorage(context)
    private var threeDAuthenticator: ThreeDAuthenticator? = null

    public interface CheckoutDialogFragmentResultListener {
        public fun onCheckoutFinish(result: Result<ChargeResult>?)
    }

    fun <T> showCheckoutDialog(
        activity: T,
        checkoutRequest: CheckoutRequest
    ) where T : AppCompatActivity, T : CheckoutDialogFragmentResultListener {
        if (!checkoutRequest.correct) {
            activity.onCheckoutFinish(Result.error(APIError.invalidCheckoutRequest))
            return
        }
        if (checkoutRequest.customerId != null) {
            activity.onCheckoutFinish(Result.error(APIError.unsupportedValue("customerId")))
            return
        }
        if (checkoutRequest.termsAndConditions != null) {
            activity.onCheckoutFinish(Result.error(APIError.unsupportedValue("termsAndConditions")))
            return
        }
        if (checkoutRequest.crossSaleOfferIds != null) {
            activity.onCheckoutFinish(Result.error(APIError.unsupportedValue("crossSaleOfferIds")))
            return
        }

        val checkoutDialogFragment = CheckoutDialogFragment()
        checkoutDialogFragment.arguments = Bundle().apply {
            putString("checkoutRequest", checkoutRequest.content)
            putString("signature", signature)
            putString("publicKey", publicKey)
            trustedAppStores?.let { putStringArray("trustedAppStores", it.toTypedArray()) }
        }
        checkoutDialogFragment.show(activity.supportFragmentManager, "checkoutDialogFragment")
    }

    fun cleanSavedCards() {
        emailStorage.cleanSavedEmails()
    }

    fun createToken(tokenRequest: TokenRequest, callback: (Result<Token>) -> Unit) {
        GlobalScope.launch {
            val token = repository.createToken(tokenRequest)
            GlobalScope.launch(Dispatchers.Main) {
                callback(token)
            }
        }
    }

    fun authenticate(
        token: Token,
        amount: Int,
        currency: String,
        activity: Activity,
        callback: (Result<Token>) -> Unit
    ) {
        if (threeDAuthenticator != null) {
            callback(Result.error(APIError.busy))
            return
        }
        threeDAuthenticator = ThreeDAuthenticator(repository, signature, trustedAppStores, activity) {
            threeDAuthenticator = null
            callback(it)
        }
        threeDAuthenticator?.authenticate(
            token,
            amount,
            currency
        )
    }
}
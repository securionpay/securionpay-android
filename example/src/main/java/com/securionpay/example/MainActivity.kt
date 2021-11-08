package com.securionpay.example

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.securionpay.SecurionPay
import com.securionpay.data.api.Result
import com.securionpay.data.api.Status
import com.securionpay.data.model.pay.ChargeResult
import com.securionpay.data.model.pay.CheckoutRequest
import com.securionpay.data.model.token.TokenRequest


class MainActivity : AppCompatActivity(), SecurionPay.CheckoutDialogFragmentResultListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val securionPay = SecurionPay(
            applicationContext,
            findViewById<TextInputEditText>(R.id.textEditPublicKeyCheckout).text.toString(),
            "8D:1D:B1:DC:EF:45:8E:16:55:BA:AE:C3:FA:16:CE:B0:8C:DE:85:EA:BE:57:5F:2E:BE:AC:E9:62:97:E0:55:37",
            listOf("com.google.android.packageinstaller")
        )

        findViewById<Button>(R.id.buttonPaymentDialog).setOnClickListener {
            securionPay.publicKey =
                findViewById<TextInputEditText>(R.id.textEditPublicKeyCheckout).text.toString()
            securionPay.showCheckoutDialog(
                this,
                checkoutRequest = CheckoutRequest(findViewById<TextInputEditText>(R.id.textEditCheckoutRequest).text.toString())
            )
        }

        findViewById<Button>(R.id.buttonCleanSavedCards).setOnClickListener {
            securionPay.cleanSavedCards()
            Toast.makeText(applicationContext, "Cards cleaned!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.buttonPay).setOnClickListener {
            securionPay.publicKey =
                findViewById<TextInputEditText>(R.id.textEditPublicKeyCustomForm).text.toString()
            val tokenRequest = TokenRequest(
                findViewById<TextInputEditText>(R.id.textEditCardNumber).text.toString(),
                findViewById<TextInputEditText>(R.id.textEditExpMonth).text.toString(),
                findViewById<TextInputEditText>(R.id.textEditExpYear).text.toString(),
                findViewById<TextInputEditText>(R.id.textEditCVC).text.toString()
            )

            securionPay.createToken(tokenRequest) { token ->
                when (token.status) {
                    Status.SUCCESS -> {
                        securionPay.authenticate(
                            token.data!!,
                            10000,
                            "EUR",
                            this
                        ) { authenticatedToken ->
                            when (authenticatedToken.status) {
                                Status.SUCCESS -> {
                                    Snackbar.make(
                                        findViewById(R.id.mainActivityLayout),
                                        "3ds finished: ${authenticatedToken.data?.id}",
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                    findViewById<TextView>(R.id.lastTokenId).text =
                                        "Last token: ${authenticatedToken.data?.id}"
                                }
                                Status.ERROR -> {
                                    Snackbar.make(
                                        findViewById(R.id.mainActivityLayout),
                                        authenticatedToken.error?.message(this)!!,
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                    findViewById<TextView>(R.id.lastTokenId).text = null
                                    Log.e("ERROR", authenticatedToken.error?.message(this)!!)
                                }
                            }
                        }
                    }
                    Status.ERROR -> {
                        Snackbar.make(
                            findViewById(R.id.mainActivityLayout),
                            token.error?.message(null)!!,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        findViewById<TextView>(R.id.lastTokenId).text = null
                    }
                }
            }
        }
    }

    override fun onCheckoutFinish(result: Result<ChargeResult>?) {
        result?.let {
            when (it.status) {
                Status.SUCCESS -> {
                    Snackbar.make(
                        findViewById(R.id.mainActivityLayout),
                        "Charge succeeded: ${it.data?.id}, Subscription id: ${it.data?.subscriptionId ?: "-"}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    findViewById<TextView>(R.id.lastChargeId).text =
                        it.data?.id?.let { id -> "Last charge: $id" }
                    findViewById<TextView>(R.id.lastSubscriptionId).text =
                        it.data?.subscriptionId?.let { id -> "Last subscription: $id" }
                }
                Status.ERROR -> {
                    Snackbar.make(
                        findViewById(R.id.mainActivityLayout),
                        it.error?.message(this)!!,
                        Snackbar.LENGTH_SHORT
                    ).show()
                    findViewById<TextView>(R.id.lastChargeId).text = null
                }
            }
        } ?: run {
            Snackbar.make(
                findViewById(R.id.mainActivityLayout),
                "Cancelled!",
                Snackbar.LENGTH_SHORT
            ).show()
            findViewById<TextView>(R.id.lastChargeId).text = null
        }
    }
}
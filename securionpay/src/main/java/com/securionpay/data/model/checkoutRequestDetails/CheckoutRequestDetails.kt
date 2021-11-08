package com.securionpay.data.model.checkoutRequestDetails

import com.securionpay.data.model.subscription.Subscription

internal data class CheckoutRequestDetails(
    val sessionId: String,
    val threeDSecureRequired: Boolean,
    val subscription: Subscription?
)
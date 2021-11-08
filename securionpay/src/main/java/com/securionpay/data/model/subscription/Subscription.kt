package com.securionpay.data.model.subscription

import java.text.NumberFormat
import java.util.*

internal data class Subscription(
    val plan: SubscriptionPlan
) {
    fun readable(): String {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        format.currency = Currency.getInstance(plan.currency)
        return format.format(plan.amount.toDouble()/100).replace(" ", " ")
    }
}
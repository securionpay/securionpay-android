package com.securionpay.data.model.pay

import java.text.NumberFormat
import java.util.*

internal data class Donation(val amount: Int, val currency: String) {
    val readable: String get() {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        format.currency = Currency.getInstance(currency)
        return format.format(amount.toDouble()/100).replace(" ", " ")
    }
}
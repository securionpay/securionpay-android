package com.securionpay.data.model.pay

import com.securionpay.utils.CurrencyFormatter
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

internal data class Donation(val amount: Int, val currency: String) {
    val readable: String get() {
        return CurrencyFormatter.format(amount.toBigDecimal(), currency, true)
    }
}
package com.securionpay.utils

import android.content.Context
import android.content.SharedPreferences

internal class EmailStorage(private val context: Context) {
    object Constants {
        const val EMAIL_STORAGE_KEY = "EMAIL_STORAGE_KEY"
        const val SAVED_EMAILS_KEY = "SAVED_EMAILS_KEY"
        const val LAST_KEY = "LAST_KEY"
    }

    private val sharedPreferences: SharedPreferences
        get() = context.getSharedPreferences(
            Constants.EMAIL_STORAGE_KEY,
            0
        )

    private var emails: List<String>
        get() = sharedPreferences.getStringSet(Constants.SAVED_EMAILS_KEY, null)
            ?.toList()
            ?.map { it ?: "" }
            ?.filter { it.isNotEmpty() } ?: listOf()
        set(newValue) {
            with(sharedPreferences.edit()) {
                putStringSet(Constants.SAVED_EMAILS_KEY, newValue.toSet())
                apply()
            }
        }

    var lastEmail: String?
        get() = sharedPreferences.getString(Constants.LAST_KEY, null)
        set(newValue) {
            with(sharedPreferences.edit()) {
                putString(Constants.LAST_KEY, newValue)
                apply()
            }
        }

    fun isEmailSaved(email: String?): Boolean {
        val email: String = email?.lowercase() ?: return false
        return emails.contains(email)
    }

    fun cleanSavedEmails() {
        lastEmail = null
        emails = listOf()
    }

    fun addSavedEmail(email: String?) {
        val email: String = email?.lowercase() ?: return

        val savedEmails = emails.toMutableList()
        savedEmails.add(email)
        emails = savedEmails
        lastEmail = email
    }
}
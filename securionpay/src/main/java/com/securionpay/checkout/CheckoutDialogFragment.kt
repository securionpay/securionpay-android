package com.securionpay.checkout

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.securionpay.R
import com.securionpay.SecurionPay
import com.securionpay.data.api.Result
import com.securionpay.data.api.Status
import com.securionpay.data.model.CreditCard
import com.securionpay.data.model.error.APIError
import com.securionpay.data.model.pay.ChargeResult
import com.securionpay.data.model.pay.CheckoutRequest
import com.securionpay.data.model.pay.Donation
import com.securionpay.data.model.sms.SMS
import com.securionpay.data.model.subscription.Subscription
import com.securionpay.data.model.token.TokenRequest
import com.securionpay.data.repository.SDKRepository
import com.securionpay.utils.*
import kotlinx.android.synthetic.main.payment_activity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


internal class CheckoutDialogFragment : BottomSheetDialogFragment() {
    private lateinit var checkoutManager: CheckoutManager
    private lateinit var checkoutRequest: CheckoutRequest
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        checkoutRequest =
            CheckoutRequest(arguments?.getString("checkoutRequest", null) ?: String.empty)
        checkoutManager = CheckoutManager(
            SDKRepository(
                SecurionPay(
                    requireActivity(),
                    arguments?.getString("publicKey", null) ?: String.empty,
                    arguments?.getString("signature", null) ?: String.empty,
                    arguments?.getStringArray("trustedAppStores")?.toList()
                )
            ),
            EmailStorage(requireActivity()),
            arguments?.getString("signature", null) ?: String.empty,
            arguments?.getStringArray("trustedAppStores")?.toList()
        )

        return inflater.inflate(R.layout.payment_activity, container, false)
    }

    private enum class Mode {
        INITIALIZING,
        LOADING,
        DONATION,
        NEW_CARD,
        SMS
    }

    private var creditCard: CreditCard = CreditCard.empty
    private var savedEmail: String? = null
    private var sms: SMS? = null
    private var subscription: Subscription? = null
    private val cvcInputFilter = CVCInputFilter()

    private var currentMode: Mode = Mode.INITIALIZING
    private var emailFlag = false
    private var numberFlag = false
    private var expirationFlag = false
    private var cvcFlag = false
    private var processing = false
    private var verifiedCard = false
    private var selectedDonation: Donation? = null
    private val modalBottomSheetBehavior: BottomSheetBehavior<FrameLayout> get() = (this.dialog as BottomSheetDialog).behavior

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogThemeNoFloating)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        modalBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        modalBottomSheetBehavior.isDraggable = false

        textInputCardNumber.filters = arrayOf(
            CreditCardInputFilter()
        )

        textInputExpiration.filters = arrayOf(
            ExpirationInputFilter()
        )

        textInputCVC.filters = arrayOf(
            cvcInputFilter
        )

        textInputEmail.addTextChangedListener {
            if (emailFlag) {
                emailFlag = false
            }
            clearTextIfSavedEmail()
            lookup(silent = true)
            updateButtonStatus()
            updateCardBrand()
        }

        textInputCardNumber.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                clearTextIfSavedEmail()
                return@setOnKeyListener false
            }
            return@setOnKeyListener false
        }

        textInputCardNumber.addTextChangedListener {
            if (numberFlag) {
                numberFlag = false
            } else {
                clearTextIfSavedEmail()
                creditCard = CreditCard(textInputCardNumber.text?.toString())
                updateCardBrand()
                if (creditCard.correct) {
                    textInputExpiration.requestFocus()
                }
                cvcInputFilter.card = creditCard
                updateButtonStatus()
            }
        }

        textInputExpiration.addTextChangedListener {
            if (expirationFlag) {
                expirationFlag = false
            } else {
                clearTextIfSavedEmail()
                if (ExpirationDateFormatter().format(
                        textInputExpiration.text.toString(),
                        false
                    ).resignFocus
                ) {
                    textInputCVC.requestFocus()
                }
                updateButtonStatus()
            }
        }

        textInputCVC.addTextChangedListener {
            if (cvcFlag) {
                cvcFlag = false
            } else {
                if (verifiedCard) {
                    clearTextIfSavedEmail()
                }
                if (it.toString().length == creditCard.cvcLength) {
                    hideKeyboard()
                }
                updateButtonStatus()
            }
        }

        textInputCVC.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
            }
            return@setOnEditorActionListener true
        }

        textInputSMSCode.setOnPinEnteredListener {
            if (it.toString().length == 6) {
                verifySMS()
            }
        }

        textInputExpiration.disableCopyPaste()
        textInputCVC.disableCopyPaste()

        buttonPayment.setOnClickListener {
            pay()
        }

        buttonClose.setOnClickListener {
            if (!isCancelable) {
                return@setOnClickListener
            }
            dismiss()
            hideKeyboard()
            callback(null)
        }

        updateError(null)
        updateEmailError(null)
        updateCardError(null)

        when {
            checkoutRequest.donations != null -> {
                val donationsAdapter = DonationsAdapter(checkoutRequest.donations!!.toTypedArray())
                donationsAdapter.onItemClick = {
                    selectedDonation = it
                }
                recyclerViewDonation.adapter = donationsAdapter
                recyclerViewDonation.addItemDecoration(
                    DonationsAdapter.DonationItemDecoration(
                        context
                    )
                )
                switchMode(Mode.DONATION)
            }
            checkoutRequest.subscriptionPlanId != null -> {
                switchMode(Mode.LOADING)
                hideKeyboard()
                getCheckoutDetails()
                switchRememberCard.isChecked = checkoutRequest.rememberMe
            }
            checkoutManager.emailStorage.lastEmail != null -> {
                hideKeyboard()
                switchMode(Mode.LOADING)

                updateButtonStatus()
                switchRememberCard.isChecked = checkoutRequest.rememberMe
                updateAmountOnButton()
                checkoutManager.emailStorage.lastEmail?.also {
                    textInputEmail.setText(it)
                }
            }
            else -> {
                switchMode(Mode.NEW_CARD)
                updateButtonStatus()
                switchRememberCard.isChecked = checkoutRequest.rememberMe
                updateAmountOnButton()
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        hideKeyboard()
        callback(null)
    }

    private fun switchMode(newMode: Mode) {
        if (currentMode == newMode) {
            return
        }
        currentMode = newMode

        when (newMode) {
            Mode.LOADING -> {
                recyclerViewDonation.visibility = View.GONE
                textViewAddPayment.setText(R.string.add_payment)
                progressIndicator.visibility = View.VISIBLE
                textViewAddPayment.visibility = View.GONE
                textViewCardInformation.visibility = View.GONE
                linearLayoutCard.visibility = View.GONE
                linearLayoutSMS.visibility = View.GONE
                constraintLayoutRememberSwitch.visibility = View.GONE
                textViewUserInformation.visibility = View.GONE
                textInputLayoutEmail.visibility = View.GONE
                textViewEmailError.visibility = View.GONE
                textViewAdditionalButtonInfo.visibility = View.GONE
                viewButtonSeparator.visibility = View.GONE
                buttonPayment.visibility = View.GONE
                buttonClose.visibility = View.GONE
            }
            Mode.NEW_CARD -> {
                recyclerViewDonation.visibility = View.GONE
                textViewAddPayment.setText(R.string.add_payment)
                progressIndicator.visibility = View.GONE
                textViewAddPayment.visibility = View.VISIBLE
                textViewCardInformation.visibility = View.VISIBLE
                linearLayoutCard.visibility = View.VISIBLE
                linearLayoutSMS.visibility = View.GONE
                constraintLayoutRememberSwitch.visibility = View.VISIBLE
                textViewUserInformation.visibility = View.VISIBLE
                textInputLayoutEmail.visibility = View.VISIBLE
                textViewEmailError.visibility = View.VISIBLE
                textViewAdditionalButtonInfo.visibility = View.GONE
                viewButtonSeparator.visibility = View.VISIBLE
                buttonPayment.visibility = View.VISIBLE
                buttonClose.visibility = View.VISIBLE
                with(buttonPayment) {
                    setCompoundDrawablesWithIntrinsicBounds(
                        ResourcesCompat.getDrawable(resources, R.drawable.ic_locker, null),
                        null,
                        null,
                        null
                    )
                    setPadding(
                        resources.getDimension(R.dimen.padding_standard).toInt(),
                        0,
                        resources.getDimension(R.dimen.padding_standard).toInt(),
                        0
                    )
                    setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
                }
                updateAmountOnButton()
                updateButtonStatus()
            }
            Mode.SMS -> {
                recyclerViewDonation.visibility = View.GONE
                textViewAddPayment.setText(R.string.add_payment)
                progressIndicator.visibility = View.GONE
                textViewAddPayment.visibility = View.VISIBLE
                textViewCardInformation.visibility = View.GONE
                linearLayoutCard.visibility = View.GONE
                linearLayoutSMS.visibility = View.VISIBLE
                constraintLayoutRememberSwitch.visibility = View.GONE
                textViewUserInformation.visibility = View.GONE
                textInputLayoutEmail.visibility = View.GONE
                textViewEmailError.visibility = View.GONE
                buttonClose.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        textInputSMSCode.requestFocus()
                        showKeyboard()
                    },
                    100,
                )
                textInputSMSCode.text = null
                textViewAdditionalButtonInfo.visibility = View.VISIBLE
                viewButtonSeparator.visibility = View.GONE
                buttonPayment.visibility = View.VISIBLE
                buttonPayment.setCompoundDrawables(null, null, null, null)
                buttonPayment.setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
                buttonPayment.setText(R.string.enter_payment_data)
                buttonPayment.isEnabled = true
            }
            Mode.DONATION -> {
                recyclerViewDonation.visibility = View.VISIBLE
                textViewAddPayment.visibility = View.VISIBLE
                textViewAddPayment.setText(R.string.select_amount)
                progressIndicator.visibility = View.GONE
                textViewCardInformation.visibility = View.GONE
                linearLayoutCard.visibility = View.GONE
                linearLayoutSMS.visibility = View.GONE
                constraintLayoutRememberSwitch.visibility = View.GONE
                textViewUserInformation.visibility = View.GONE
                textInputLayoutEmail.visibility = View.GONE
                textViewEmailError.visibility = View.GONE
                viewButtonSeparator.visibility = View.VISIBLE
                buttonClose.visibility = View.VISIBLE
                textViewAdditionalButtonInfo.visibility = View.GONE

                buttonPayment.visibility = View.VISIBLE
                buttonPayment.setCompoundDrawables(null, null, null, null)
                buttonPayment.setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
                buttonPayment.setText(R.string.confirm)
                buttonPayment.isEnabled = true
            }
            else -> {
            }
        }

        updateSwitchVisibility()
        updateError(null)
        updateEmailError(null)
        updateCardError(null)
    }

    private fun clearTextIfSavedEmail() {
        if (savedEmail == null) {
            return
        }
        savedEmail = null
        emailFlag = true
        numberFlag = true
        expirationFlag = true
        cvcFlag = true
        textInputCardNumber.text = null
        textInputExpiration.text = null
        textInputCVC.text = null
        sms = null
        verifiedCard = false
        creditCard = CreditCard.empty

        updateSwitchVisibility()
        updateError(null)
        updateEmailError(null)
        updateCardError(null)
    }

    private fun verifySMS() {
        if (processing) {
            return
        }
        setProcessing(true)
        updateError(null)
        GlobalScope.launch {
            val verifyResult = checkoutManager.verifySMS(textInputSMSCode.text.toString(), sms!!)
            withContext(Dispatchers.Main) {
                setProcessing(false)
                when (verifyResult.status) {
                    Status.SUCCESS -> verifyResult.data?.also {
                        creditCard = CreditCard(card = it.card)
                        fillCardForm()
                        switchMode(Mode.NEW_CARD)
                    }
                    Status.ERROR -> verifyResult.error?.also { error ->
                        textInputSMSCode.text = null
                        textInputSMSCode.setPinBackground(
                            ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.rounded_edge_with_error,
                                null
                            )
                        )
                        Handler(Looper.getMainLooper()).postDelayed(
                            {
                                textInputSMSCode?.setPinBackground(
                                    ResourcesCompat.getDrawable(
                                        resources,
                                        R.drawable.pin_background,
                                        null
                                    )
                                )
                            },
                            1000,
                        )
                        if (error.code != APIError.Code.InvalidVerificationCode) {
                            updateError(APIError.unknown.message(context))
                        }
                    }
                }
            }
        }
    }

    private fun pay() {
        hideKeyboard()

        if (currentMode == Mode.DONATION) {
            switchMode(Mode.NEW_CARD)
            return
        }

        if (processing) {
            return
        }
        if (currentMode == Mode.SMS) {
            creditCard = CreditCard.empty
            savedEmail = null
            sms = null
            switchMode(Mode.NEW_CARD)
            return
        }
        setProcessing(true)
        buttonPayment.startAnimation()
        val remember = switchRememberCard.isChecked
        val email = textInputEmail.text?.toString()
        val number = textInputCardNumber.text?.toString()
        val expiration = textInputExpiration.text?.toString()
        val cvc = textInputCVC.text?.toString()

        val month = expiration?.split("/")?.first()
        val year = expiration?.split("/")?.last()

        hideKeyboard()
        updateError(null)
        updateEmailError(null)
        updateCardError(null)

        if (savedEmail != null) {
            GlobalScope.launch {
                val token = checkoutManager.savedToken(savedEmail!!)
                GlobalScope.launch(Dispatchers.Main) {
                    when (token.status) {
                        Status.SUCCESS -> checkoutManager.pay(
                            token.data!!,
                            checkoutRequest,
                            email ?: String.empty,
                            remember = remember,
                            activity,
                            sms = sms,
                            cvc = cvc,
                            customAmount = selectedDonation?.amount ?: subscription?.plan?.amount,
                            customCurrency = selectedDonation?.currency
                                ?: subscription?.plan?.currency
                        ) {
                            GlobalScope.launch(Dispatchers.Main) {
                                checkoutManager.emailStorage.lastEmail =
                                    if (it.data != null) savedEmail else null

                                if (it.error != null) {
                                    savedEmail = null
                                    textInputCardNumber.text = null
                                    textInputExpiration.text = null
                                    textInputCVC.text = null
                                    creditCard = CreditCard.empty
                                    updateCardBrand()
                                    updateSwitchVisibility()
                                }

                                processChargeResult(it)
                            }
                        }
                        Status.ERROR -> {
                            setProcessing(false)
                            Handler(Looper.getMainLooper()).postDelayed({
                                buttonPayment.revertAnimation()
                            }, 100)
                            token.error?.let { updateError(it.message(this@CheckoutDialogFragment.requireContext())) }
                        }
                    }
                }
            }
            return
        }

        val tokenRequest = TokenRequest(
            number ?: String.empty,
            month ?: String.empty,
            year ?: String.empty,
            cvc ?: String.empty
        )
        GlobalScope.launch {
            checkoutManager.pay(
                tokenRequest,
                checkoutRequest,
                email!!,
                remember = remember,
                activity,
                customAmount = selectedDonation?.amount ?: subscription?.plan?.amount,
                customCurrency = selectedDonation?.currency ?: subscription?.plan?.currency
            ) {
                GlobalScope.launch(Dispatchers.Main) {
                    processChargeResult(it)
                }
            }
        }
    }

    private fun processChargeResult(charge: Result<ChargeResult>) {
        when (charge.status) {
            Status.SUCCESS -> charge.data?.also {
                ResourcesCompat.getDrawable(resources, R.drawable.ic_check, null)
                    ?.toBitmap(96, 96)
                    ?.also {
                        Handler(Looper.getMainLooper()).postDelayed({
                            buttonPayment?.doneLoadingAnimation(
                                ResourcesCompat.getColor(
                                    resources,
                                    R.color.success,
                                    null
                                ), it
                            )
                        }, 100)
                    }
                    ?: run { buttonPayment.stopAnimation() }
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        callback(charge)
                        dismiss()
                    },
                    500,
                )
            } ?: run {
                Handler(Looper.getMainLooper()).postDelayed({
                    buttonPayment.revertAnimation()
                }, 100)
                setProcessing(false)
            }
            Status.ERROR -> charge.error?.also { error ->
                setProcessing(false)
                hideKeyboard()
                Handler(Looper.getMainLooper()).postDelayed({
                    buttonPayment.revertAnimation()
                }, 100)
                if (error.type == APIError.Type.CardError && error.code != null) {
                    updateCardError(error.message(context))
                } else if (error.code == APIError.Code.InvalidEmail) {
                    updateEmailError(error.message(context))
                } else if (error.code == APIError.Code.VerificationCodeRequired) {
                    lookup()
                } else if (error.type == APIError.Type.ThreeDSecure) {
                    callback(Result.error(error))
                    Handler(Looper.getMainLooper()).postDelayed({
                        dismiss()
                    }, 150)
                } else {
                    updateError(error.message(context))
                }
            }
        }
    }

    private fun updateError(message: String?) {
        textViewError?.text = message
        if (message.isNullOrEmpty()) {
            textViewError?.visibility = View.GONE
            (viewButtonSeparator.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = 64
        } else {
            textViewError?.visibility = View.VISIBLE
            (viewButtonSeparator.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = 0
        }
    }

    private fun updateEmailError(message: String?) {
        if (message == null) {
            textInputEmail.setBackgroundResource(R.drawable.rounded_edge)
        } else {
            textInputEmail.setBackgroundResource(R.drawable.rounded_edge_with_error)
        }
        textViewEmailError.text = message
        if (message.isNullOrEmpty()) {
            textViewEmailError.visibility = View.GONE
        } else {
            textViewEmailError.visibility = View.VISIBLE
        }
    }

    private fun updateCardError(message: String?) {
        if (message == null) {
            linearLayoutCard.setBackgroundResource(R.drawable.rounded_edge)
        } else {
            linearLayoutCard.setBackgroundResource(R.drawable.rounded_edge_with_error)
        }
        textViewCardError.text = message
        if (message.isNullOrEmpty()) {
            textViewCardError.visibility = View.GONE
        } else {
            textViewCardError.visibility = View.VISIBLE
        }
    }

    private fun updateButtonStatus() {
        val email = textInputEmail.text?.toString() ?: String.empty
        val number = textInputCardNumber.text?.toString()
            ?: String.empty
        val expiration =
            textInputExpiration.text?.toString()
                ?: String.empty
        val cvc = textInputCVC.text?.toString()
            ?: String.empty

        val card = CreditCard(number = number)

        val correctEmail = email.isNotEmpty()
        val correctNumber = card.correct
        val correctExpiration = expiration.isNotEmpty()
        val correctCVC = cvc.length == card.cvcLength

        buttonPayment?.isEnabled =
            correctEmail && correctNumber && correctExpiration && correctCVC
    }

    private fun hideKeyboard() {
        view?.also { view ->
            textInputCardNumber.clearFocus()
            textInputExpiration.clearFocus()
            textInputCVC.clearFocus()
            textInputSMSCode.clearFocus()
            textInputEmail.clearFocus()

            (context
                ?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun showKeyboard() {
        (context
            ?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager)
            .toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    private fun lookup(silent: Boolean = false) {
        val email = textInputEmail.text?.toString() ?: String.empty
        if (email.isEmpty()) {
            return
        }
        if (!silent) {
            setProcessing(true)
        }
        GlobalScope.launch {
            val lookup = checkoutManager.lookup(email)
            withContext(Dispatchers.Main) {
                when (lookup.status) {
                    Status.SUCCESS -> lookup.data?.also { data ->
                        savedEmail = email

                        if (data.phone != null) {
                            if (data.phone.verified) {
                                verifiedCard = true
                            }
                            val sendSMSResult = checkoutManager.sendSMS(email)
                            withContext(Dispatchers.Main) {
                                setProcessing(false)
                                when (sendSMSResult.status) {
                                    Status.SUCCESS -> {
                                        sms = sendSMSResult.data
                                        switchMode(Mode.SMS)
                                    }
                                    Status.ERROR -> lookup.error?.also { error ->
                                        updateError(error.message(context))
                                        updateAmountOnButton()
                                    }
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                creditCard = CreditCard(card = data.card)
                                fillCardForm()
                                setProcessing(false)
                                Handler(Looper.getMainLooper()).postDelayed({
                                    switchMode(Mode.NEW_CARD)
                                }, 200)
                            }
                        }
                    }
                    Status.ERROR -> lookup.error?.also { error ->
                        if (!silent) {
                            updateError(error.message(context))
                            updateAmountOnButton()
                            setProcessing(false)
                        }
                    }
                }
            }
        }
    }

    private fun getCheckoutDetails() {
        GlobalScope.launch {
            val details = checkoutManager.checkoutRequestDetails(checkoutRequest)
            GlobalScope.launch(Dispatchers.Main) {
                when (details.status) {
                    Status.SUCCESS -> {
                        subscription = details.data!!.subscription
                        switchMode(Mode.NEW_CARD)
                        checkoutManager.emailStorage.lastEmail?.also {
                            textInputEmail.setText(it)
                        }
                    }
                    Status.ERROR -> {
                        callback(Result.error(APIError.unknown))
                        dismiss()
                    }
                }
            }
        }
    }

    private fun fillCardForm() {
        emailFlag = true
        numberFlag = true
        expirationFlag = true
        cvcFlag = true
        textInputCardNumber.setText(creditCard.readable)
        textInputExpiration.setText("••/••")
        if (verifiedCard) {
            hideKeyboard()
            textInputCVC.setText("•••")
        } else {
            textInputCVC.text = null
            textInputCVC.requestFocus()
        }
        updateCardBrand()
        updateButtonStatus()
        updateSwitchVisibility()
    }

    private fun updateSwitchVisibility() {
        if (currentMode != Mode.NEW_CARD) {
            (viewButtonSeparator.layoutParams as ViewGroup.MarginLayoutParams).topMargin = 64
            constraintLayoutRememberSwitch.visibility = View.GONE
            return
        }
        if (savedEmail == null) {
            (viewButtonSeparator.layoutParams as ViewGroup.MarginLayoutParams).topMargin = 0
            constraintLayoutRememberSwitch.visibility = View.VISIBLE
        } else {
            (viewButtonSeparator.layoutParams as ViewGroup.MarginLayoutParams).topMargin = 64
            constraintLayoutRememberSwitch.visibility = View.GONE
        }
    }

    private fun updateAmountOnButton() {
        buttonPayment.text = when {
            subscription != null -> getString(R.string.pay, subscription?.readable())
            selectedDonation != null -> getString(R.string.pay, selectedDonation?.readable)
            else -> getString(R.string.pay, checkoutRequest.readable)
        }
    }

    private fun setProcessing(processing: Boolean) {
        isCancelable = !processing
        this.processing = processing
        textInputEmail.isEnabled = !processing
        textInputCardNumber.isEnabled = !processing
        textInputExpiration.isEnabled = !processing
        textInputCVC.isEnabled = !processing
        switchRememberCard.isEnabled = !processing
    }

    private fun callback(result: Result<ChargeResult>?) {
        (activity as? SecurionPay.CheckoutDialogFragmentResultListener)?.onCheckoutFinish(result)
    }

    private fun updateCardBrand() {
        imageViewCardBrand.setImageDrawable(creditCard.image(resources))
    }
}
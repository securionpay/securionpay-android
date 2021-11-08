package com.securionpay.threed

import android.content.Context
import android.view.View
import com.nsoftware.ipworks3ds.sdk.customization.UiCustomization
import com.securionpay.R

internal class ThreeDUICustomizationFactory(val context: Context) {
    fun createUICustimization(): UiCustomization {
        val uiCustomization = UiCustomization()

        customizeButtons(uiCustomization, context)

        val labelCustomization = uiCustomization.labelCustomization
        labelCustomization.headingTextColor = context.resources.getString(R.color.black)
        labelCustomization.headingTextFontName = "montserrat_bold.ttf"
        labelCustomization.headingTextAlignment = View.TEXT_ALIGNMENT_TEXT_START
        labelCustomization.headingTextFontSize = 24

        labelCustomization.setTextFontName(UiCustomization.LabelType.INFO_LABEL, "montserrat_bold.ttf")
        labelCustomization.setTextFontSize(UiCustomization.LabelType.INFO_LABEL, 14)
        labelCustomization.setTextColor(UiCustomization.LabelType.INFO_LABEL, context.resources.getString(R.color.black))

        labelCustomization.setTextFontName(UiCustomization.LabelType.INFO_TEXT, "lato_regular.ttf")
        labelCustomization.setTextFontSize(UiCustomization.LabelType.INFO_TEXT, 14)
        labelCustomization.setTextColor(UiCustomization.LabelType.INFO_TEXT, context.resources.getString(R.color.black))

        labelCustomization.setTextFontName(UiCustomization.LabelType.SELECTION_LIST, "lato_regular.ttf")
        labelCustomization.setTextFontSize(UiCustomization.LabelType.SELECTION_LIST, 14)
        labelCustomization.setTextColor(UiCustomization.LabelType.SELECTION_LIST, context.resources.getString(R.color.black))

        labelCustomization.setTextFontName(UiCustomization.LabelType.WHY_INFO, "lato_bold.ttf")
        labelCustomization.setTextFontSize(UiCustomization.LabelType.WHY_INFO, 14)
        labelCustomization.setTextColor(UiCustomization.LabelType.WHY_INFO, context.resources.getString(R.color.black))
        labelCustomization.setBackgroundColor(UiCustomization.LabelType.WHY_INFO, context.resources.getString(R.color.white))

        labelCustomization.setTextFontName(UiCustomization.LabelType.WHY_INFO_TEXT, "lato_regular.ttf")
        labelCustomization.setTextFontSize(UiCustomization.LabelType.WHY_INFO_TEXT, 14)
        labelCustomization.setTextColor(UiCustomization.LabelType.WHY_INFO_TEXT, context.resources.getString(R.color.black))
        labelCustomization.setBackgroundColor(UiCustomization.LabelType.WHY_INFO_TEXT, context.resources.getString(R.color.white))

        labelCustomization.setTextFontName(UiCustomization.LabelType.EXPANDABLE_INFO, "lato_bold.ttf")
        labelCustomization.setTextFontSize(UiCustomization.LabelType.EXPANDABLE_INFO, 14)
        labelCustomization.setTextColor(UiCustomization.LabelType.EXPANDABLE_INFO, context.resources.getString(R.color.black))
        labelCustomization.setBackgroundColor(UiCustomization.LabelType.EXPANDABLE_INFO, context.resources.getString(R.color.white))

        labelCustomization.setTextFontName(UiCustomization.LabelType.EXPANDABLE_INFO_TEXT, "lato_regular.ttf")
        labelCustomization.setTextFontSize(UiCustomization.LabelType.EXPANDABLE_INFO_TEXT, 14)
        labelCustomization.setTextColor(UiCustomization.LabelType.EXPANDABLE_INFO_TEXT, context.resources.getString(R.color.black))
        labelCustomization.setBackgroundColor(UiCustomization.LabelType.EXPANDABLE_INFO_TEXT, context.resources.getString(R.color.white))

        val textBoxCustomization = uiCustomization.textBoxCustomization
        textBoxCustomization.borderWidth = 1
        textBoxCustomization.textFontName = "lato_regular.ttf"
        textBoxCustomization.textFontSize = 14
        textBoxCustomization.cornerRadius = 10
        textBoxCustomization.borderColor = context.resources.getString(R.color.grayLight)

        uiCustomization.toolbarCustomization.backgroundColor =
            context.resources.getString(R.color.white)
        uiCustomization.toolbarCustomization.textColor =
            context.resources.getString(R.color.black)
        uiCustomization.toolbarCustomization.headerText = "3D Secure"
        uiCustomization.toolbarCustomization.textFontSize = 16
        uiCustomization.background = context.resources.getString(R.color.white)
        
        return uiCustomization
    }

    private fun customizeButton(uiCustomization: UiCustomization, type: UiCustomization.ButtonType, backgroundColor: String, textColor: String) {
        val button = uiCustomization.getButtonCustomization(type)
        button.backgroundColor = backgroundColor
        button.height = 48
        button.cornerRadius = 10
        button.textColor = textColor
        button.textFontName = "montserrat_bold.ttf"
        button.textFontSize = 14
    }

    private fun customizeButtons(uiCustomization: UiCustomization, context: Context) {
        customizeButton(uiCustomization, UiCustomization.ButtonType.SUBMIT, context.resources.getString(
            R.color.primary), context.resources.getString(R.color.white))
        customizeButton(uiCustomization, UiCustomization.ButtonType.RESEND, context.resources.getString(
            R.color.white), context.resources.getString(R.color.black))
        customizeButton(uiCustomization, UiCustomization.ButtonType.NEXT, context.resources.getString(
            R.color.primary), context.resources.getString(R.color.white))
        customizeButton(uiCustomization, UiCustomization.ButtonType.CANCEL, context.resources.getString(
            R.color.white), context.resources.getString(R.color.black))
        customizeButton(uiCustomization, UiCustomization.ButtonType.CONTINUE, context.resources.getString(
            R.color.primary), context.resources.getString(R.color.white))
    }
}
package com.securionpay.data.model.threeD

import com.securionpay.data.model.token.Token

internal data class ThreeDCheckResponse(
    val version: String,
    val token: Token,
    val directoryServerCertificate: DirectoryServerCertificate,
    val sdkLicense: String
)
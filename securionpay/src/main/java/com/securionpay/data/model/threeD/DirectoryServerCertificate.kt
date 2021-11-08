package com.securionpay.data.model.threeD

internal data class DirectoryServerCertificate(
    val certificate: String,
    val caCertificates: List<String>
)
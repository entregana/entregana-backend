package com.entregana.webserver.dtos

data class RecipientDto(
    val firstName: String,
    val lastName: String,
    val address: String,
    val email: String,
    val phone: String,
    val additionalDetails: String? = ""
)

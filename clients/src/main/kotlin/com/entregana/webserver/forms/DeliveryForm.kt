package com.entregana.webserver.forms

import com.entregana.webserver.dtos.RecipientDto
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

data class DeliveryForm (
    @NotBlank
    val packageType: String,

    @NotBlank
    val courier: String,

    @NotNull
    val recipient: RecipientDto,

    @NotBlank
    val status: String
)

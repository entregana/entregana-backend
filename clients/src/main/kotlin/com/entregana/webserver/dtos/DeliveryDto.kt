package com.entregana.webserver.dtos

import java.time.Instant
import java.util.*

data class DeliveryDto(
    val id: UUID,
    val packageType: String,
    val sender: String,
    val courier: String,
    val recipient: RecipientDto,
    val status: String,
    val otherDetails: Any? = null,
    val modifiedAt: Instant?,
    val type: RecordType?
)

enum class RecordType {
    HISTORICAL, CURRENT
}

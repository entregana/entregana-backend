package com.entregana.webserver.mappers

import com.entregana.states.Recipient
import com.entregana.webserver.dtos.RecipientDto
import org.springframework.stereotype.Component

@Component
class RecipientDtoMapper {

    fun mapToDto(domain: Recipient): RecipientDto =
        RecipientDto(
            firstName = domain.firstName,
            lastName = domain.lastName,
            address = domain.address,
            email = domain.email,
            phone = domain.phone,
            additionalDetails = domain.additionalDetails
        )

    fun mapToDomain(dto: RecipientDto): Recipient =
        Recipient(
            firstName = dto.firstName,
            lastName = dto.lastName,
            address = dto.address,
            email = dto.email,
            phone = dto.phone,
            additionalDetails = dto.additionalDetails
        )
}

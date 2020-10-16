package com.entregana.webserver.mappers

import com.entregana.states.DeliveryState
import com.entregana.webserver.dtos.DeliveryDto
import com.entregana.webserver.dtos.RecordType
import net.corda.core.node.services.Vault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DeliveryDtoMapper {
    @Autowired
    lateinit var recipientDtoMapper: RecipientDtoMapper

    fun mapToDto(domain: DeliveryState, metadata: Vault.StateMetadata? = null): DeliveryDto =
        DeliveryDto(
            id = domain.linearId.id,
            packageType = domain.packageType,
            sender = domain.sender.name.toString(),
            courier = domain.courier.name.toString(),
            recipient = recipientDtoMapper.mapToDto(domain.recipient),
            status = domain.status,
            otherDetails = domain.otherDetails,
            modifiedAt = metadata?.recordedTime,
            type =
                if (metadata != null) {
                    if (metadata.status == Vault.StateStatus.CONSUMED) {
                        RecordType.HISTORICAL
                    } else {
                        RecordType.CURRENT
                    }
                } else {
                    null
                }
        )
}

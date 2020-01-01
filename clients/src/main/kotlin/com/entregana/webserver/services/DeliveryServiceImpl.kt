package com.entregana.webserver.services

import com.entregana.flows.ChangeDetailsFlowInitiator
import com.entregana.flows.ChangeStatusFlowInitiator
import com.entregana.flows.CompleteFlowInitiator
import com.entregana.flows.CreateFlowInitiator
import com.entregana.states.DeliveryState
import com.entregana.webserver.NodeRPCConnection
import com.entregana.webserver.dtos.DeliveryDto
import com.entregana.webserver.dtos.RecordType
import com.entregana.webserver.forms.DeliveryForm
import com.entregana.webserver.mappers.DeliveryDtoMapper
import com.entregana.webserver.mappers.RecipientDtoMapper
import com.entregana.webserver.repositories.DeliveryStateRepository
import com.entregana.webserver.types.DeliveryType
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault
import net.corda.core.utilities.getOrThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class DeliveryServiceImpl(nodeRPCConnection: NodeRPCConnection): DeliveryService {
    private val proxy = nodeRPCConnection.proxy

    @Autowired
    lateinit var deliveryDtoMapper: DeliveryDtoMapper

    @Autowired
    lateinit var recipientDtoMapper: RecipientDtoMapper

    @Autowired
    lateinit var deliveryStateRepository: DeliveryStateRepository

    override fun retrieveDeliveryStates(type: DeliveryType): List<DeliveryDto> {
        val vaultPage = deliveryStateRepository.getAllDeliveryStates(
            if (type == DeliveryType.COMPLETED || type == DeliveryType.ALL) Vault.StateStatus.ALL else Vault.StateStatus.UNCONSUMED
        )

        val group = vaultPage.states.groupBy { it.state.data.linearId }
        return group.map {
            val stateAndRef = it.value.last()
            val metaData = vaultPage.statesMetadata.last { metadata -> stateAndRef.ref == metadata.ref }
            deliveryDtoMapper.mapToDto(
                domain = stateAndRef.state.data,
                metadata = metaData
            )
        }.filter {
            if (type == DeliveryType.COMPLETED) {
                it.type == RecordType.HISTORICAL
            } else if (type == DeliveryType.ACTIVE) {
                it.type == RecordType.CURRENT
            } else {
                true
            }
        }.sortedByDescending { it.modifiedAt }
    }

    override fun retrieveDeliveryHistory(id: UUID): List<DeliveryDto> {
        val vaultPage = deliveryStateRepository.getAllDeliveryStatesAndRefsById(id)
        return vaultPage.states.map {
            val metaData = vaultPage.statesMetadata.last { metadata -> it.ref == metadata.ref }
            deliveryDtoMapper.mapToDto(domain=it.state.data, metadata = metaData)
        }.sortedByDescending { it.modifiedAt }
    }

    override fun createDeliveryState(form: DeliveryForm): DeliveryDto {
        val notary = proxy.notaryIdentities().first()
        val deliveryState = proxy.startFlowDynamic(
            CreateFlowInitiator::class.java,
            form.packageType,
            proxy.wellKnownPartyFromX500Name(
                CordaX500Name.parse(form.courier)
            ),
            this.recipientDtoMapper.mapToDomain(form.recipient),
            form.status,
            notary
        ).returnValue.getOrThrow()
        return deliveryDtoMapper.mapToDto(deliveryState)
    }

    override fun updateDeliveryState(id: UUID, form: DeliveryForm): DeliveryDto {
        val inputStateAndRef = deliveryStateRepository.getDeliveryStateAndRefById(id)
        val inputState = inputStateAndRef.state.data
        var tempState: DeliveryState = inputState
        if (inputState.status != form.status) {
            tempState = proxy.startFlowDynamic(
                ChangeStatusFlowInitiator::class.java,
                UniqueIdentifier(id = id),
                form.status
            ).returnValue.getOrThrow()
        }

        val resultingState = tempState.copy(
            packageType = form.packageType,
            courier = proxy.wellKnownPartyFromX500Name(
                CordaX500Name.parse(form.courier)
            )!!,
            recipient = recipientDtoMapper.mapToDomain(form.recipient)
        )

        if (tempState != resultingState) {
            tempState = proxy.startFlowDynamic(
                ChangeDetailsFlowInitiator::class.java,
                resultingState
            ).returnValue.getOrThrow()
        }

        return deliveryDtoMapper.mapToDto(tempState)
    }

    override fun completeDelivery(id: UUID): DeliveryDto {
        val deliveryState = proxy.startFlowDynamic(
            CompleteFlowInitiator::class.java,
            UniqueIdentifier(id = id)
        ).returnValue.getOrThrow()
        return deliveryDtoMapper.mapToDto(deliveryState)
    }

}

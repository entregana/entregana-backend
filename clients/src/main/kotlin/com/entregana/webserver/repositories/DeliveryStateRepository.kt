package com.entregana.webserver.repositories

import com.entregana.states.DeliveryState
import net.corda.core.contracts.StateAndRef
import net.corda.core.node.services.Vault
import java.util.*

interface DeliveryStateRepository {
    fun getDeliveryStateAndRefById(id: UUID): StateAndRef<DeliveryState>
    fun getDeliveryStateById(id: UUID): DeliveryState
    fun getAllDeliveryStatesAndRefsById(id: UUID): Vault.Page<DeliveryState>
    fun getAllDeliveryStates(type: Vault.StateStatus): Vault.Page<DeliveryState>
}

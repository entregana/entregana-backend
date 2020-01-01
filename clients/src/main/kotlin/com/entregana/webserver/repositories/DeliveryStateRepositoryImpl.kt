package com.entregana.webserver.repositories

import com.entregana.states.DeliveryState
import com.entregana.webserver.NodeRPCConnection
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class DeliveryStateRepositoryImpl(nodeRPCConnection: NodeRPCConnection): DeliveryStateRepository {
    private val proxy = nodeRPCConnection.proxy

    override fun getDeliveryStateAndRefById(id: UUID): StateAndRef<DeliveryState> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
            linearId = listOf(UniqueIdentifier(id = id)))
        val results = proxy.vaultQueryByCriteria(
            criteria = queryCriteria,
            contractStateType = DeliveryState::class.java
        )

        return results.states.singleOrNull()
            ?: throw FlowException("There is no DeliveryState with linear ID $id")
    }

    override fun getDeliveryStateById(id: UUID): DeliveryState {
        val inputStateAndRef = this.getDeliveryStateAndRefById(id)

        return inputStateAndRef.state.data
    }

    override fun getAllDeliveryStatesAndRefsById(id: UUID): Vault.Page<DeliveryState> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
            linearId = listOf(UniqueIdentifier(id = id)),
            status = Vault.StateStatus.ALL
        )
        return proxy.vaultQueryByCriteria(
            criteria = queryCriteria,
            contractStateType = DeliveryState::class.java
        )
    }

    override fun getAllDeliveryStates(type: Vault.StateStatus): Vault.Page<DeliveryState> {
        val criteria = QueryCriteria.VaultQueryCriteria(type)
        return proxy.vaultQueryByCriteria(
            criteria = criteria,
            contractStateType = DeliveryState::class.java
        )
    }
}

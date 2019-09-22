package com.entregana.states

import com.entregana.contracts.DeliveryContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

// *********
// * State *
// *********
@BelongsToContract(DeliveryContract::class)
data class DeliveryState(val data: String, override val participants: List<AbstractParty> = listOf()) : ContractState

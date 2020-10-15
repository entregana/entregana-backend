package com.entregana.states

import com.entregana.contracts.DeliveryContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

// *********
// * State *
// *********
@BelongsToContract(DeliveryContract::class)
data class DeliveryState(
        val packageType: String,
        val sender: Party,
        val courier: Party,
        val recipient: Recipient,
        val status: String,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<Party> = listOf(sender, courier)
) : LinearState

@CordaSerializable
data class Recipient(
    val firstName: String,
    val lastName: String,
    val address: String,
    val email: String,
    val phone: String,
    val additionalDetails: String = ""
)

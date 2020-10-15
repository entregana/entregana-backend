package com.entregana.contracts

import com.entregana.states.DeliveryState
import com.entregana.states.Recipient
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class CompleteCommandTests {
    private val ledgerServices = MockServices()
    private val sender = TestIdentity(CordaX500Name("John Doe", "City", "BG"))
    private val courier = TestIdentity(CordaX500Name("Richard Roe", "Town", "BG"))
    private val participants = listOf(sender.publicKey, courier.publicKey)
    private val recipient = Recipient(
        firstName = "John",
        lastName = "Doe",
        address = "123 Main St Anytown, Bulgaria",
        email = "john.doe@example.com",
        phone = "+359888888888"
    )
    private val deliveryState = DeliveryState(
        packageType = "PALLET",
        sender = sender.party,
        courier = courier.party,
        recipient = recipient,
        status = "Pending"
    )

    @Test
    fun `Complete command should complete successfully`() {
        ledgerServices.ledger {
            transaction {
                command(participants, DeliveryContract.Commands.Complete())
                input(DeliveryContract.ID, deliveryState)
                verifies()
            }
        }
    }

    @Test
    fun `One input should be consumed`() {
        ledgerServices.ledger {
            transaction {
                command(participants, DeliveryContract.Commands.Complete())
                input(DeliveryContract.ID, deliveryState)
                input(DeliveryContract.ID, deliveryState)
                failsWith("One input should be consumed.")
            }
        }
    }

    @Test
    fun `No outputs should be produced`() {
        ledgerServices.ledger {
            transaction {
                command(participants, DeliveryContract.Commands.Complete())
                input(DeliveryContract.ID, deliveryState)
                output(DeliveryContract.ID, deliveryState)
                failsWith("No outputs should be produced.")
            }
        }
    }

}

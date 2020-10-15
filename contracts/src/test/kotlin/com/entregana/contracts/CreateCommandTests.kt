package com.entregana.contracts

import com.entregana.states.DeliveryState
import com.entregana.states.Recipient
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class CreateCommandTests {
    private val ledgerServices = MockServices()
    private val sender = TestIdentity(CordaX500Name("Acme Corporation", "Anytown", "BG"))
    private val courier = TestIdentity(CordaX500Name("ACE Delivery", "Anytown", "BG"))
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
    fun `Create command should complete successfully`() {
        ledgerServices.ledger {
            transaction {
                command(participants, DeliveryContract.Commands.Create())
                output(DeliveryContract.ID, deliveryState)
                verifies()
            }
        }
    }

    @Test
    fun `No inputs should be consumed`() {
        ledgerServices.ledger {
            transaction {
                command(participants, DeliveryContract.Commands.Create())
                input(DeliveryContract.ID, deliveryState)
                output(DeliveryContract.ID, deliveryState)
                failsWith("No inputs should be consumed.")
            }
        }
    }

    @Test
    fun `One output should be produced`() {
        ledgerServices.ledger {
            transaction {
                command(participants, DeliveryContract.Commands.Create())
                output(DeliveryContract.ID, deliveryState)
                output(DeliveryContract.ID, deliveryState)
                failsWith("One output should be produced.")
            }
        }
    }

    @Test
    fun `The recipient address should not be empty`() {
        ledgerServices.ledger {
            transaction {
                command(participants, DeliveryContract.Commands.Create())
                output(
                    DeliveryContract.ID,
                    deliveryState.copy(
                        recipient = recipient.copy(
                            address = ""
                        )
                    )
                )
                failsWith("The recipient address should not be empty.")
            }
        }
    }

    @Test
    fun `The status should not be empty`() {
        ledgerServices.ledger {
            transaction {
                command(participants, DeliveryContract.Commands.Create())
                output(DeliveryContract.ID, deliveryState.copy(status = ""))
                failsWith("The status should not be empty.")
            }
        }
    }
}

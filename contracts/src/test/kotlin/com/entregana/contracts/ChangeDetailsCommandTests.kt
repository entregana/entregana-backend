package com.entregana.contracts

import com.entregana.states.DeliveryState
import com.entregana.states.Recipient
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class ChangeDetailsCommandTests {
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
        status = "Pending",
        otherDetails = "To be delivered between 9AM and 6PM"
    )
    private val newDeliveryState = deliveryState.copy(
        recipient = recipient.copy(
            email = "john.doe.new@example.com"
        )
    )

    @Test
    fun `ChangeDetails command should complete successfully`() {
        ledgerServices.ledger {
            transaction {
                command(participants, DeliveryContract.Commands.ChangeDetails())
                input(DeliveryContract.ID, deliveryState)
                output(DeliveryContract.ID, newDeliveryState)
                verifies()
            }
        }
    }

    @Test
    fun `One input should be consumed`() {
        ledgerServices.ledger {
            transaction {
                command(participants, DeliveryContract.Commands.ChangeDetails())
                output(DeliveryContract.ID, newDeliveryState)
                failsWith("One input should be consumed.")
            }
            transaction {
                command(participants, DeliveryContract.Commands.ChangeDetails())
                input(DeliveryContract.ID, deliveryState)
                input(DeliveryContract.ID, deliveryState)
                output(DeliveryContract.ID, newDeliveryState)
                failsWith("One input should be consumed.")
            }
        }
    }

    @Test
    fun `One output should be produced`() {
        ledgerServices.ledger {
            transaction {
                command(participants, DeliveryContract.Commands.ChangeDetails())
                input(DeliveryContract.ID, deliveryState)
                failsWith("One output should be produced.")
            }
            transaction {
                command(participants, DeliveryContract.Commands.ChangeDetails())
                input(DeliveryContract.ID, deliveryState)
                output(DeliveryContract.ID, newDeliveryState)
                output(DeliveryContract.ID, newDeliveryState)
                failsWith("One output should be produced.")
            }
        }
    }

    @Test
    fun `The details should change`() {
        ledgerServices.ledger {
            transaction {
                command(participants, DeliveryContract.Commands.ChangeDetails())
                input(DeliveryContract.ID, deliveryState)
                output(DeliveryContract.ID, deliveryState)
                failsWith("The details should change.")
            }
        }
    }

    @Test
    fun `The status should not change`() {
        ledgerServices.ledger {
            transaction {
                command(participants, DeliveryContract.Commands.ChangeDetails())
                input(DeliveryContract.ID, deliveryState)
                output(DeliveryContract.ID, newDeliveryState.copy(status = "Accepted")
                )
                failsWith("The status should not change")
            }
        }
    }

    @Test
    fun `The recipient address should not be empty`() {
        ledgerServices.ledger {
            transaction {
                command(participants, DeliveryContract.Commands.ChangeDetails())
                input(DeliveryContract.ID, deliveryState)
                output(
                    DeliveryContract.ID,
                    newDeliveryState.copy(
                        recipient = recipient.copy(
                            address = ""
                        )
                    )
                )
                failsWith("The recipient address should not be empty.")
            }
        }
    }
}

package com.entregana.contracts

import com.entregana.states.DeliveryState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class DeliveryContract : Contract {
    companion object {
        const val ID = "com.entregana.contracts.DeliveryContract"
    }

    interface Commands : CommandData {
        class Create : Commands
        class ChangeStatus: Commands
        class ChangeDetails: Commands
        class Complete: Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val inputs = tx.inputsOfType<DeliveryState>()
        val outputs = tx.outputsOfType<DeliveryState>()
        val command = tx.commandsOfType<Commands>().single()

        when (command.value) {
            is Commands.Create -> requireThat {
                "No inputs should be consumed." using (inputs.isEmpty())
                "One output should be produced." using (outputs.size == 1)

                val state = outputs.single()
                "The recipient address should not be empty." using (state.recipient.address.isNotEmpty())
                "The status should not be empty." using (state.status.isNotEmpty())
            }
            is Commands.ChangeStatus -> requireThat {
                "One input should be consumed." using (inputs.size == 1)
                "One output should be produced." using (outputs.size == 1)

                val inputState = inputs.single()
                val outputState = outputs.single()
                "The status should change." using (inputState.status != outputState.status)
                "Only the status should change." using (inputState == outputState.copy(status = inputState.status))
                "The status should not be empty." using (outputState.status.isNotEmpty())
            }
            is Commands.ChangeDetails -> requireThat {
                "One input should be consumed." using (inputs.size == 1)
                "One output should be produced." using (outputs.size == 1)

                val inputState = inputs.single()
                val outputState = outputs.single()
                "The details should change." using (inputState != outputState)
                "The status should not change." using (inputState.status == outputState.status)
                "The recipient address should not be empty." using (outputState.recipient.address.isNotEmpty())
            }
            is Commands.Complete -> requireThat {
                "One input should be consumed." using (inputs.size == 1)
                "No outputs should be produced." using (outputs.isEmpty())
            }
        }
    }
}

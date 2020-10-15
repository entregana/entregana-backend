package com.entregana.flows

import co.paralleluniverse.fibers.Suspendable
import com.entregana.contracts.DeliveryContract
import com.entregana.states.DeliveryState
import com.entregana.states.Recipient
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class CreateFlowInitiator(
    val packageType: String,
    val courier: Party,
    val recipient: Recipient,
    val status: String,
    val notaryToUse: Party
) : FlowLogic<DeliveryState>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): DeliveryState {
        val deliveryState = DeliveryState(
            packageType=packageType,
            sender = ourIdentity,
            courier=courier,
            recipient = recipient,
            status = status
        )

        val createCommand = Command(
            DeliveryContract.Commands.Create(),
            listOf(ourIdentity.owningKey, courier.owningKey)
        )

        val transactionBuilder = TransactionBuilder(notaryToUse)
            .addOutputState(deliveryState, DeliveryContract.ID)
            .addCommand(createCommand)

        transactionBuilder.verify(serviceHub)

        val partiallySignedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val counterpartySession = initiateFlow(courier)
        val fullySignedTransaction = subFlow(
            CollectSignaturesFlow(
                partiallySignedTransaction,
                listOf(counterpartySession)
            )
        )

        subFlow(FinalityFlow(fullySignedTransaction, counterpartySession))

        return deliveryState
    }
}

@InitiatedBy(CreateFlowInitiator::class)
class CreateFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow: SignTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val outputState = stx.tx.outputsOfType<DeliveryState>().single()
                "The counterparty should be the courier" using (outputState.courier == ourIdentity)
            }
        }
        val expectedTxId = subFlow(signTransactionFlow).id
        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = expectedTxId))
    }
}

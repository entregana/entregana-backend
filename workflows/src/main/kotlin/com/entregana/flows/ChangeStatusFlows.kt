package com.entregana.flows

import co.paralleluniverse.fibers.Suspendable
import com.entregana.contracts.DeliveryContract
import com.entregana.states.DeliveryState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class ChangeStatusFlowInitiator(
    val linearId: UniqueIdentifier,
    val status: String
) : FlowLogic<DeliveryState>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): DeliveryState {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
            linearId = listOf(linearId))
        val results = serviceHub.vaultService.queryBy<DeliveryState>(queryCriteria)
        val inputStateAndRef = results.states.singleOrNull()
            ?: throw FlowException("There is no DeliveryState with linear ID $linearId")
        val inputState = inputStateAndRef.state.data
        val outputState = inputState.copy(status = status)
        val changeStatusCommand = Command(
            DeliveryContract.Commands.ChangeStatus(),
            inputState.participants.map { it.owningKey }
        )

        val transactionBuilder = TransactionBuilder(inputStateAndRef.state.notary)
            .addInputState(inputStateAndRef)
            .addOutputState(outputState, DeliveryContract.ID)
            .addCommand(changeStatusCommand)

        transactionBuilder.verify(serviceHub)

        val partiallySignedTransaction = serviceHub.signInitialTransaction(transactionBuilder)

        val counterpartySessions = (inputState.participants - ourIdentity).map {initiateFlow(it)}.toSet()
        val fullySignedTransaction = subFlow(
            CollectSignaturesFlow(
                partiallySignedTransaction,
                counterpartySessions
            )
        )

        subFlow(FinalityFlow(fullySignedTransaction, counterpartySessions))

        return outputState
    }
}

@InitiatedBy(ChangeStatusFlowInitiator::class)
class ChangeStatusFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow: SignTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val outputState = stx.tx.outputsOfType<DeliveryState>().single()
                "The counterparty should be either the courier or the sender" using (listOf(outputState.sender, outputState.courier).contains(ourIdentity))
            }
        }
        val expectedTxId = subFlow(signTransactionFlow).id
        subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = expectedTxId))
    }
}

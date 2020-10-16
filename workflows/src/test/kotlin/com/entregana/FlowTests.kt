package com.entregana

import com.entregana.flows.*
import com.entregana.states.DeliveryState
import com.entregana.states.Recipient
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.queryBy
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class FlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
        TestCordapp.findCordapp("com.entregana.contracts"),
        TestCordapp.findCordapp("com.entregana.flows")
    )))
    private val sender = network.createNode()
    private val courier = network.createNode()
    private val recipient = Recipient(
        firstName = "John",
        lastName = "Doe",
        address = "123 Main St Anytown, Bulgaria",
        email = "john.doe@example.com",
        phone = "+359888888888"
    )

    init {
        courier.registerInitiatedFlow(CreateFlowResponder::class.java)
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    private fun createDelivery(): DeliveryState {
        val flow = CreateFlowInitiator(
            packageType = "PALLET",
            courier = courier.info.chooseIdentity(),
            recipient = recipient,
            status = "Pending",
            otherDetails = "To be delivered between 9AM and 6PM",
            notaryToUse = network.defaultNotaryIdentity
        )
        val resultFuture = sender.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }

    private fun changeDeliveryStatus(
        linearId: UniqueIdentifier,
        status: String,
        initiator: StartedMockNode
    ): DeliveryState {
        val flow = ChangeStatusFlowInitiator(
            linearId = linearId,
            status = status
        )
        val resultFuture = initiator.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }

    private fun changeDeliveryDetails(
        deliveryState: DeliveryState,
        initiator: StartedMockNode
    ): DeliveryState {
        val flow = ChangeDetailsFlowInitiator(
            deliveryState = deliveryState
        )
        val resultFuture = initiator.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }

    private fun completeDelivery(
        linearId: UniqueIdentifier,
        initiator: StartedMockNode
    ): DeliveryState {
        val flow = CompleteFlowInitiator(linearId = linearId)
        val resultFuture = initiator.startFlow(flow)
        network.runNetwork()
        return resultFuture.get()
    }

    @Test
    fun `Create flow - Golden path`() {
        createDelivery()

        listOf(sender, courier).forEach { node ->
            node.transaction {
                val deliveryStatesAndRefs = node.services.vaultService.queryBy<DeliveryState>().states
                assertEquals(1, deliveryStatesAndRefs.size)

                val deliveryState = deliveryStatesAndRefs.single().state.data
                assertEquals(sender.info.chooseIdentity(), deliveryState.sender)
                assertEquals(courier.info.chooseIdentity(), deliveryState.courier)
            }
        }
    }

    @Test
    fun `Change status flow - Golden path`() {
        val createdDeliveryState = createDelivery()
        val status = "Accepted"

        changeDeliveryStatus(
            linearId = createdDeliveryState.linearId,
            status = status,
            initiator = sender
        )

        listOf(sender, courier).forEach { node ->
            node.transaction {
                val deliveryStatesAndRefs = node.services.vaultService.queryBy<DeliveryState>().states
                assertEquals(1, deliveryStatesAndRefs.size)

                val deliveryState = deliveryStatesAndRefs.single().state.data
                assertEquals(sender.info.chooseIdentity(), deliveryState.sender)
                assertEquals(courier.info.chooseIdentity(), deliveryState.courier)
                assertEquals(deliveryState.status, status)
            }
        }

        val secondStatus = "Delivered"
        changeDeliveryStatus(
            linearId = createdDeliveryState.linearId,
            status = secondStatus,
            initiator = courier
        )

        listOf(sender, courier).forEach { node ->
            node.transaction {
                val deliveryStatesAndRefs = node.services.vaultService.queryBy<DeliveryState>().states
                assertEquals(1, deliveryStatesAndRefs.size)

                val deliveryState = deliveryStatesAndRefs.single().state.data
                assertEquals(sender.info.chooseIdentity(), deliveryState.sender)
                assertEquals(courier.info.chooseIdentity(), deliveryState.courier)
                assertEquals(deliveryState.status, secondStatus)
            }
        }
    }

    @Test
    fun `Change details flow - Golden path`() {
        val createdDeliveryState = createDelivery()
        val newDeliveryState = createdDeliveryState.copy(
            packageType = "LETTER",
            recipient = createdDeliveryState.recipient.copy(
                email = "john.doe.new@example.com"
            )
        )

        changeDeliveryDetails(
            deliveryState = newDeliveryState,
            initiator = sender
        )

        listOf(sender, courier).forEach { node ->
            node.transaction {
                val deliveryStatesAndRefs = node.services.vaultService.queryBy<DeliveryState>().states
                assertEquals(1, deliveryStatesAndRefs.size)

                val deliveryState = deliveryStatesAndRefs.single().state.data
                assertEquals(sender.info.chooseIdentity(), deliveryState.sender)
                assertEquals(courier.info.chooseIdentity(), deliveryState.courier)
                assertEquals(deliveryState, newDeliveryState)
            }
        }

        changeDeliveryDetails(
            deliveryState = createdDeliveryState,
            initiator = courier
        )

        listOf(sender, courier).forEach { node ->
            node.transaction {
                val deliveryStatesAndRefs = node.services.vaultService.queryBy<DeliveryState>().states
                assertEquals(1, deliveryStatesAndRefs.size)

                val deliveryState = deliveryStatesAndRefs.single().state.data
                assertEquals(sender.info.chooseIdentity(), deliveryState.sender)
                assertEquals(courier.info.chooseIdentity(), deliveryState.courier)
                assertEquals(deliveryState, createdDeliveryState)
            }
        }
    }

    @Test
    fun `Complete flow - Golden path`() {
        val createdDeliveryState = createDelivery()
        changeDeliveryStatus(linearId = createdDeliveryState.linearId, initiator = sender, status = "Delivered")
        completeDelivery(linearId=createdDeliveryState.linearId, initiator = sender)

        listOf(sender, courier).forEach { node ->
            node.transaction {
                val deliveryStatesAndRefs = node.services.vaultService.queryBy<DeliveryState>().states
                assertEquals(0, deliveryStatesAndRefs.size)
            }
        }
    }
}

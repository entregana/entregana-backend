package com.entregana.webserver.services

import com.entregana.webserver.dtos.DeliveryDto
import com.entregana.webserver.forms.DeliveryForm
import com.entregana.webserver.types.DeliveryType
import java.util.*

interface DeliveryService {
    fun retrieveDeliveryStates(type: DeliveryType = DeliveryType.ALL): List<DeliveryDto>
    fun retrieveDeliveryHistory(id: UUID): List<DeliveryDto>
    fun createDeliveryState(form: DeliveryForm): DeliveryDto
    fun updateDeliveryState(id: UUID, form: DeliveryForm): DeliveryDto
    fun completeDelivery(id: UUID): DeliveryDto
}

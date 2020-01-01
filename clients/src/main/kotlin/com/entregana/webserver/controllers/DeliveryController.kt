package com.entregana.webserver.controllers

import com.entregana.webserver.dtos.DeliveryDto
import com.entregana.webserver.forms.DeliveryForm
import com.entregana.webserver.services.DeliveryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.validation.Valid

@RestController
@RequestMapping("/api/deliveries") // The paths for GET and POST requests are relative to this base path.
class DeliveryController {

    @Autowired
    private lateinit var deliveryService: DeliveryService

    @GetMapping(value = [""], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getActiveDeliveries(): ResponseEntity<List<DeliveryDto>> {
        val deliveryDtos = deliveryService.retrieveDeliveryStates()

        return ResponseEntity.ok(deliveryDtos)
    }

    @GetMapping("completed", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getCompletedDeliveries(): ResponseEntity<List<DeliveryDto>> {
        val deliveryDtos = deliveryService.retrieveDeliveryStates(completed=true)
        return ResponseEntity.ok(deliveryDtos)
    }

    @PostMapping(
        value = [""],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    private fun createDelivery(
        @Valid @RequestBody form: DeliveryForm,
        errors: Errors
    ): ResponseEntity<DeliveryDto> {
        if (errors.hasErrors()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }

        val deliveryDto = deliveryService.createDeliveryState(form)

        return ResponseEntity(deliveryDto, HttpStatus.CREATED)
    }

    @GetMapping("{id}/history", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getDeliveryHistory(
        @PathVariable("id") id: UUID
    ): ResponseEntity<List<DeliveryDto>> {
        return ResponseEntity.ok(deliveryService.retrieveDeliveryHistory(id))
    }

    @PatchMapping(
        value = ["{id}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    private fun updateDeliveryState(
        @PathVariable("id") id: UUID,
        @Valid @RequestBody form: DeliveryForm,
        errors: Errors
    ): ResponseEntity<DeliveryDto> {
        if (errors.hasErrors()) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val deliveryDto = deliveryService.updateDeliveryState(
            id = id,
            form = form
        )
        return ResponseEntity.ok(deliveryDto)
    }

    @DeleteMapping(value = ["{id}/complete"])
    private fun completeDeliveryState(
        @PathVariable("id") id: UUID
    ): ResponseEntity<DeliveryDto> {
        val deliveryDto = deliveryService.completeDelivery(id = id)
        return ResponseEntity.ok(deliveryDto)
    }
}

package taxiapp.ride.controller;

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.beans.factory.annotation.Autowired
import taxiapp.ride.service.RideService

@RestController
@RequestMapping("api/ride")
class RideController @Autowired constructor(
    private val rideService: RideService
){    
    @PostMapping("/new")
    fun requestNewRide(@RequestParam originId: String, @RequestParam destinationId: String, @RequestHeader("username") passengerUsername: String): ResponseEntity<Any> {
        val response = rideService.requestRide(originId, destinationId, passengerUsername)
        if (!response.isSuccess()) {
            return ResponseEntity.status(response.httpStatus).body(response.messages)
        }
        return ResponseEntity.ok(response)
    }

    @PostMapping("/accept/passenger")
    fun acceptProposedRide(@RequestParam rideId: Long): ResponseEntity<Any> {
        val response = rideService.acceptProposedRide(rideId)
        return ResponseEntity.status(response.httpStatus).body(response.messages)
    }

    @PostMapping("/reject/passenger")
    fun rejectProposedRide(@RequestParam rideId: Long): ResponseEntity<Any> {
        val response = rideService.acceptProposedRide(rideId)
        return ResponseEntity.status(response.httpStatus).body(response.messages)
    }

    @PostMapping("/arrived")
    fun arrived(@RequestParam rideId: Long, @RequestHeader("username") driverUsername: String): ResponseEntity<Any> {
        val response = rideService.driverArrived(rideId, driverUsername)
        if (!response.isSuccess()) {
            return ResponseEntity.status(response.httpStatus).body(response.messages)
        }
        return ResponseEntity.ok(response)
    }


    @PostMapping("/finish")
    fun finishRide(@RequestParam rideId: Long, @RequestHeader("username") driverUsername: String): ResponseEntity<Any> {
        val response = rideService.finishRide(rideId, driverUsername)
        if (!response.isSuccess()) {
            return ResponseEntity.status(response.httpStatus).body(response.messages)
        }
        return ResponseEntity.ok(response)
    }

    @PostMapping("/cancel")
    fun cancelRide(@RequestParam rideId: Long): ResponseEntity<Any> {
        val response = rideService.cancelRide(rideId, 80)
        if (!response.isSuccess()) {
            return ResponseEntity.status(response.httpStatus).body(response.messages)
        }
        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun getRideInfo(@RequestParam rideId: Long): ResponseEntity<Any> {
        return ResponseEntity.ok(rideService.getRideInfo(rideId))
    }

    @GetMapping("/passenger")
    fun getPassengerRide(@RequestHeader("username") passengerUsername: String): ResponseEntity<Any> {
        val response = rideService.getCurrentPassengerRide(passengerUsername)
        if (!response.isSuccess()) {
            return ResponseEntity.status(response.httpStatus).body(response.messages)
        }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/driver")
    fun getDriverRide(@RequestHeader("username") driverUsername: String): ResponseEntity<Any> {
        val response = rideService.getCurrentDriverRide(driverUsername)
        if (!response.isSuccess()) {
            return ResponseEntity.status(response.httpStatus).body(response.messages)
        }
        return ResponseEntity.ok(response)
    }

}

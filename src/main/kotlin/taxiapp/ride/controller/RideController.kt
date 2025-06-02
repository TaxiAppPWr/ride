package taxiapp.ride.controller;

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.beans.factory.annotation.Autowired
import taxiapp.ride.service.RideService

@RestController
@RequestMapping("api/ride")
class RideController @Autowired constructor(
    private val driversService: RideService
){    
    @PostMapping("/new")
    fun requestNewRide(@RequestParam originId: String, @RequestParam destinationId: String, @RequestParam passengerUsername: String): ResponseEntity<Any> {
        val response = driversService.requestRide(originId, destinationId, passengerUsername)
        if (!response.isSuccess()) {
            return ResponseEntity.status(response.httpStatus).body(response.messages)
        }
        return ResponseEntity.ok(response)
    }

    @PostMapping("/accept/passenger")
    fun acceptProposedRide(@RequestParam rideId: Long): ResponseEntity<Any> {
        val response = driversService.acceptProposedRide(rideId)
        return ResponseEntity.status(response.httpStatus).body(response.messages)
    }
}

package taxiapp.ride.service

import taxiapp.ride.dto.response.ResponseInterface
import taxiapp.ride.model.Ride

interface RideService {
    fun requestRide(originId: String, destinationId: String, passengerUsername: String): ResponseInterface
    fun acceptProposedRide(rideId: Long): ResponseInterface
    fun cancelRide(rideId: Long, refundPercentage: Int): ResponseInterface
    fun paymentStatusUpdated(rideId: Long, status: String): ResponseInterface
    fun getRideInfo(rideId: Long): Ride?
    fun findDriver(rideId: Long): ResponseInterface
    fun driverConfirmedRide(rideId: Long, driverUsername: String): ResponseInterface
    fun finishRide(rideId: Long, driverUsername: String): ResponseInterface
}
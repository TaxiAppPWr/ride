package taxiapp.ride.service

import org.springframework.web.bind.annotation.RequestParam
import taxiapp.ride.dto.response.ResponseInterface
import taxiapp.ride.model.Ride
import java.sql.Driver

interface RideService {
    fun requestRide(originId: String, destinationId: String, passengerUsername: String): ResponseInterface
    fun acceptProposedRide(rideId: Long): ResponseInterface
    fun cancelRide(rideId: Long, refundPercentage: Int): ResponseInterface
    fun paymentStatusUpdated(rideId: Long, status: String): ResponseInterface
    fun getRideInfo(rideId: Long): Ride?
    fun findDriver(rideId: Long): ResponseInterface
//    fun generatePaymentLink(paymentInfo: GeneratePaymentEvent)
//    fun updatePaymentStatus(confirmationInfo: PaymentStatusUpdatedEvent)
//    fun queueTest(generatePaymentEvent: GeneratePaymentEvent)
}
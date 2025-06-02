package taxiapp.ride.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import taxiapp.ride.repository.RideRepository
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import taxiapp.ride.dto.event.CancelRideEvent
import taxiapp.ride.dto.event.GeneratePaymentEvent
import taxiapp.ride.dto.remote.response.CoordinatesTO
import taxiapp.ride.dto.remote.response.FareCalculationResponse
import taxiapp.ride.dto.requests.MatchingRequest
import taxiapp.ride.dto.response.ResponseInterface
import taxiapp.ride.dto.response.ResultTO
import taxiapp.ride.dto.response.RideRequestResponse
import taxiapp.ride.model.Ride
import taxiapp.ride.model.EventName
import taxiapp.ride.model.PaymentStatus
import taxiapp.ride.model.RideStatus
import java.math.BigDecimal
import kotlin.Int
import kotlin.String

@Service
class RideServiceImpl @Autowired constructor(
    private val rideRepository: RideRepository,
    private val eventCounterService: EventCounterService,
    @Qualifier("rideExchange") private val rideExchange: TopicExchange,
    private val template: RabbitTemplate,
    private val restTemplate: RestTemplate,
    @Value("\${service.address.ppayment}")
    private val paymentServiceAddress: String,
    @Value("\${service.address.fare}")
    private val fareServiceAddress: String,
    @Value("\${service.address.maps}")
    private val mapsServiceAddress: String,
    @Value("\${service.address.matching}")
    private val matchingServiceAddress: String,
    @Value("\${rabbit.routing-key.ppayment.generate}")
    private val generatePaymentTopic: String,
    @Value("\${rabbit.routing-key.ride.cancel}")
    private val cancelRideTopic: String,

    ) : RideService {
    private val logger = LoggerFactory.getLogger(RideServiceImpl::class.java)

    override fun requestRide(
        originId: String,
        destinationId: String,
        passengerUsername: String
    ): ResponseInterface {
        val fareCalculationUri = UriComponentsBuilder
            .fromUriString("$fareServiceAddress/api/fare/calculate")
            .queryParam("originId", originId)
            .queryParam("destinationId", destinationId)
            .build()
            .toUri()

        val response = restTemplate.getForEntity(fareCalculationUri, FareCalculationResponse::class.java)

        if (!response.statusCode.is2xxSuccessful) {
            return ResultTO(
                httpStatus = HttpStatus.BAD_GATEWAY,
                messages = listOf("Fare calculation service returned: ${response.statusCode}")
            )
        }

        val body = response.body!!

        val newRide = Ride(
            pickupId = originId,
            destinationId = destinationId,
            passengerUsername = passengerUsername,
            fare = body.amount,
            driverEarnings = body.driverProfit,
            distance = body.distance,
            duration =  body.duration
        )

        rideRepository.save(newRide)

        logger.atInfo().log("fun requestRide - Ride requested sucessfully: rideId=${newRide.rideId}")

        return RideRequestResponse(
            rideId = newRide.rideId!!,
            pickupId = newRide.pickupId,
            destinationId = newRide.destinationId,
            distance = newRide.distance,
            duration = newRide.duration,
            fare = newRide.fare
        )
    }

    override fun acceptProposedRide(rideId: Long): ResponseInterface {
        val ride = rideRepository.findById(rideId)

        if (!ride.isPresent) {
            return ResultTO(HttpStatus.NOT_FOUND, listOf("No ride with id $rideId"))
        }

        if (ride.get().status != RideStatus.NEW) {
            return ResultTO(HttpStatus.BAD_REQUEST, listOf("Ride with id $rideId has already been accepted"))
        }

        ride.get().status = RideStatus.AWAITING_PAYMENT

        // TODO - get user info from cognito
        val event = GeneratePaymentEvent(
            generatePaymentEventId = eventCounterService.getNextId(EventName.GENERATE_LINK),
            rideId = rideId,
            amount = ride.get().fare,
            firstname = "temp",
            lastname = "temp",
            phoneNumber = "+48123456789",
            email = "temp@temp.com"
        )
        template.convertAndSend(rideExchange.name, generatePaymentTopic, event)
        logger.atInfo().log("fun acceptProposedRide - Ride accepted: rideId=${rideId}")
        return ResultTO(HttpStatus.CREATED)
    }

    override fun cancelRide(
        rideId: Long,
        refundPercentage: Int
    ): ResponseInterface {
        val ride = rideRepository.findById(rideId)

        if (ride.isEmpty) {
            return ResultTO(HttpStatus.NOT_FOUND, listOf("No ride with id $rideId"))
        }

        ride.get().status = RideStatus.CANCELLED

        val event = CancelRideEvent(
            cancelRideEventId = eventCounterService.getNextId(EventName.CANCEL_RIDE),
            rideId = rideId,
            refundPercentage = refundPercentage
        )
        template.convertAndSend(rideExchange.name, cancelRideTopic, event)
        logger.info("Ride $rideId cancelled with $refundPercentage% refund")

        return ResultTO(HttpStatus.CREATED)
    }

    override fun paymentStatusUpdated(
        rideId: Long,
        paymentStatus: String
    ): ResponseInterface {
        val ride = rideRepository.findById(rideId)

        if (ride.isEmpty) {
            return ResultTO(HttpStatus.NOT_FOUND, listOf("No ride with id $rideId"))
        }
        logger.info("fun paymentStatusUpdated - Payment status updated event captured. New payment status for ride $rideId is $paymentStatus.")
        if (paymentStatus == PaymentStatus.COMPLETED.name) {
            ride.get().status = RideStatus.PAYMENT_RECEIVED
            rideRepository.save(ride.get())
            logger.info("Finding driver")
            return findDriver(rideId)
        }

        if (paymentStatus == PaymentStatus.FAILED.name ||  paymentStatus == PaymentStatus.CANCELED.name) {
            cancelRide(rideId, 100)
        }
        
        return ResultTO(HttpStatus.OK)
    }

    override fun getRideInfo(rideId: Long): Ride? {
        val ride = rideRepository.findById(rideId)
        return if (ride.isPresent) {
            ride.get()
        } else {
            null
        }
    }

    override fun findDriver(rideId: Long): ResponseInterface {
        val ride = rideRepository.findById(rideId)

        if (ride.isEmpty) {
            logger.error("fun findDriver - Ride with id $rideId has incorrect status to perform this action.")
            return ResultTO(HttpStatus.NOT_FOUND, listOf("No ride with id $rideId"))
        }

        if (ride.get().status != RideStatus.PAYMENT_RECEIVED) {
            logger.error("fun findDriver - Ride with id $rideId has incorrect status to perform this action.")
            return ResultTO(HttpStatus.BAD_REQUEST, listOf("Ride with id $rideId has incorrect status to perform this action."))
        }
        
        val matchingServiceUri = UriComponentsBuilder
            .fromUriString("$matchingServiceAddress/api/matching/find-driver")
            .build()
            .toUri()

        val pickupCoordsRequestUri = UriComponentsBuilder
            .fromUriString("$mapsServiceAddress/api/maps/coordinates")
            .queryParam("placeId", ride.get().pickupId)
            .build()
            .toUri()

        val dropoffCoordsRequestUri = UriComponentsBuilder
            .fromUriString("$mapsServiceAddress/api/maps/coordinates")
            .queryParam("placeId", ride.get().destinationId)
            .build()
            .toUri()

        val pickupCoordsResponse = restTemplate.getForEntity(pickupCoordsRequestUri, CoordinatesTO::class.java)
        val dropoffCoordsResponse = restTemplate.getForEntity(dropoffCoordsRequestUri, CoordinatesTO::class.java)

        if (!pickupCoordsResponse.statusCode.is2xxSuccessful || !dropoffCoordsResponse.statusCode.is2xxSuccessful) {
            logger.error("fun findDriver - Maps service returned error when calling for coordinates.")
            return ResultTO(HttpStatus.SERVICE_UNAVAILABLE, listOf("Maps service returned an error when calling for coordinates."))
        }

        val pickupCoords = pickupCoordsResponse.body
        val dropoffCoords = dropoffCoordsResponse.body

        if (pickupCoords == null ||  dropoffCoords == null) {
            logger.error("fun findDriver - Maps service returned empty body when calling for coordinates.")
            return ResultTO(HttpStatus.SERVICE_UNAVAILABLE, listOf("Maps service returned empty body when calling for coordinates."))
        }

        val request = MatchingRequest(
            rideId = rideId,
            pickupLatitude = pickupCoords.latitude,
            pickupLongitude = pickupCoords.longitude,
            dropoffLatitude = dropoffCoords.latitude,
            dropoffLongitude = dropoffCoords.longitude,
            estimatedPrice = BigDecimal(ride.get().driverEarnings).divide(BigDecimal(100))
        )

        val response = restTemplate.postForEntity(matchingServiceUri, request, Object::class.java)

        if (!response.statusCode.is2xxSuccessful) {
            return ResultTO(
                httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
                messages = listOf("Matching service returned: ${response.statusCode}")
            )
        }
        logger.info("fun findDriver - Request for driver sent successfully.")
        return ResultTO(HttpStatus.OK)
    }
}


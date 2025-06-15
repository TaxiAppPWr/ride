package taxiapp.ride.service

import org.slf4j.LoggerFactory
import org.springframework.amqp.core.DirectExchange
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import taxiapp.ride.repository.RideRepository
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import taxiapp.ride.dto.event.CancelRideEvent
import taxiapp.ride.dto.event.GeneratePaymentEvent
import taxiapp.ride.dto.event.RideFinishedEvent
import taxiapp.ride.dto.remote.response.CoordinatesTO
import taxiapp.ride.dto.remote.response.DriverPersonalInfoResponse
import taxiapp.ride.dto.remote.response.FareCalculationResponse
import taxiapp.ride.dto.requests.MatchingRequest
import taxiapp.ride.dto.requests.PushNotificationRequest
import taxiapp.ride.dto.response.ResponseInterface
import taxiapp.ride.dto.response.ResultTO
import taxiapp.ride.dto.response.RideRequestResponse
import taxiapp.ride.dto.response.RideResponse
import taxiapp.ride.model.Ride
import taxiapp.ride.model.EventName
import taxiapp.ride.model.PaymentStatus
import taxiapp.ride.model.RideStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import kotlin.Int
import kotlin.String

@Service
class RideServiceImpl @Autowired constructor(
    private val rideRepository: RideRepository,
    private val eventCounterService: EventCounterService,
    @Qualifier("rideExchange") private val rideExchange: DirectExchange,
    private val template: RabbitTemplate,
    private val restTemplate: RestTemplate,
    @Value("\${service.address.fare}")
    private val fareServiceAddress: String,
    @Value("\${service.address.maps}")
    private val mapsServiceAddress: String,
    @Value("\${service.address.drivers}")
    private val driverServiceAddress: String,
    @Value("\${service.address.notification}")
    private val notificationServiceAddress: String,
    @Value("\${service.address.matching}")
    private val matchingServiceAddress: String,
    @Value("\${rabbit.routing-key.ppayment.generate}")
    private val generatePaymentTopic: String,
    @Value("\${rabbit.routing-key.ride.cancel}")
    private val cancelRideTopic: String,
    @Value("\${rabbit.routing-key.ride.finished}")
    private val rideFinishedTopic: String,

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

        val event = GeneratePaymentEvent(
            generatePaymentEventId = eventCounterService.getNextId(EventName.GENERATE_LINK),
            rideId = rideId,
            amount = ride.get().fare,
            passengerUsername = ride.get().passengerUsername,
        )
        template.convertAndSend(rideExchange.name, generatePaymentTopic, event)
        logger.atInfo().log("fun acceptProposedRide - Ride accepted: rideId=${rideId}")
        return ResultTO(HttpStatus.CREATED)
    }

    override fun rejectProposedRide(rideId: Long): ResponseInterface {
        val ride = rideRepository.findById(rideId)

        if (!ride.isPresent) {
            return ResultTO(HttpStatus.NOT_FOUND, listOf("No ride with id $rideId"))
        }

        if (ride.get().status != RideStatus.NEW) {
            return ResultTO(HttpStatus.BAD_REQUEST, listOf("Ride with id $rideId has already been accepted"))
        }

        ride.get().status = RideStatus.CANCELLED

        return ResultTO(HttpStatus.OK)
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
            refundPercentage = refundPercentage,
            driverId = ride.get().driverUsername ?: "",
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
            val result = findDriver(rideId)
            if (result.httpStatus == HttpStatus.SERVICE_UNAVAILABLE) {
                logger.info("Error encountered when finding driver, cancelling ride.")
                cancelRide(rideId, 100)
            }
            return result
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
            logger.error("fun findDriver - No ride with id $rideId")
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
            pickupAddress = "${pickupCoords.latitude},${pickupCoords.longitude}",
            pickupLatitude = pickupCoords.latitude,
            pickupLongitude = pickupCoords.longitude,
            dropoffAddress = "${dropoffCoords.latitude},${dropoffCoords.longitude}",
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
        ride.get().status = RideStatus.AWAITING_DRIVER
        rideRepository.save(ride.get())
        logger.info("fun findDriver - Ride $rideId awaiting driver confirmation.")
        return ResultTO(HttpStatus.OK)
    }

    override fun driverConfirmedRide(
        rideId: Long,
        driverUsername: String
    ): ResponseInterface {
        val ride = rideRepository.findById(rideId)

        if (ride.isEmpty) {
            logger.error("fun driverConfirmedRide - No ride with id $rideId")
            return ResultTO(HttpStatus.NOT_FOUND, listOf("No ride with id $rideId"))
        }

        if (ride.get().status != RideStatus.AWAITING_DRIVER) {
            logger.error("fun driverConfirmedRide - Expected ride $rideId to have status ${RideStatus.AWAITING_DRIVER}. Ride status is ${ride.get().status}")
            return ResultTO(HttpStatus.BAD_REQUEST, listOf("Expected ride $rideId to have status ${RideStatus.AWAITING_DRIVER}"))
        }

        ride.get().driverUsername = driverUsername

        val driverUri = UriComponentsBuilder
            .fromUriString("$driverServiceAddress/api/drivers")
            .queryParam("username", driverUsername)
            .build()
            .toUri()

        val driverInfo = restTemplate.getForEntity(driverUri, DriverPersonalInfoResponse::class.java)

        if (!driverInfo.statusCode.is2xxSuccessful) {
            logger.error("Driver service returned: ${driverInfo.statusCode}")
            throw HttpServerErrorException(driverInfo.statusCode, "Driver service returned: ${driverInfo.statusCode}")
        }

        if (driverInfo.body == null) {
            logger.error("fun driverConfirmedRide - Driver info request returned empty body")
            throw HttpServerErrorException(driverInfo.statusCode, "fun driverConfirmedRide - Driver info request returned empty body")
        }

        val notificationUri = UriComponentsBuilder
            .fromUriString("$notificationServiceAddress/api/notification/push")
            .build()
            .toUri()

        val message = "Driver confirmed ride\nDriver information:" +
                "\nfirstname: ${driverInfo.body!!.driverPersonalData.firstname}" +
                "\nlastname: ${driverInfo.body!!.driverPersonalData.lastname}" +
                "\nphone number: ${driverInfo.body!!.driverPersonalData.phoneNumber}"

        val pushRequest = PushNotificationRequest(
            username = ride.get().passengerUsername,
            title = "Driver accepted ride",
            body = message
        )

        restTemplate.postForEntity(notificationUri, pushRequest, Object::class.java)
        ride.get().status = RideStatus.IN_PROGRESS
        ride.get().startTime = OffsetDateTime.now()
        rideRepository.save(ride.get())
        logger.info("fun driverConfirmedRide - Email sent successfully. Driver $driverUsername | ride ${ride.get()}")

        return  ResultTO(HttpStatus.OK)
    }

    override fun driverArrived(
        rideId: Long,
        driverUsername: String
    ): ResponseInterface {
        val ride = rideRepository.findById(rideId)

        if (ride.isEmpty) {
            logger.info("fun driverArrived - No ride with id $rideId")
            return ResultTO(HttpStatus.NOT_FOUND, listOf("No ride with id $rideId"))
        }

        if (ride.get().status != RideStatus.IN_PROGRESS) {
            logger.info("fun driverArrived - Ride $rideId that is not ${RideStatus.IN_PROGRESS}")
            return ResultTO(HttpStatus.BAD_REQUEST, listOf("Cannot finish ride $rideId that is not ${RideStatus.IN_PROGRESS}"))
        }

        if (ride.get().driverUsername != driverUsername) {
            logger.info("fun driverArrived - Driver username does not match the driver assigned to ride $rideId")
            return ResultTO(HttpStatus.BAD_REQUEST, listOf("Driver username ($driverUsername) does not match the driver assigned to ride $rideId (${ride.get().driverUsername})"))
        }

        val notificationUri = UriComponentsBuilder
            .fromUriString("$notificationServiceAddress/api/notification/push")
            .build()
            .toUri()

        val message = "Driver is waiting for you at the selected pickup point";

        val pushRequest = PushNotificationRequest(
            username = ride.get().passengerUsername,
            title = "Driver arrived",
            body = message
        )

        val response = restTemplate.postForEntity(notificationUri, pushRequest, Object::class.java)

        return ResultTO(HttpStatus.OK, listOf("Driver arrived at pickup point"))
    }

    override fun finishRide(
        rideId: Long,
        driverUsername: String
    ): ResponseInterface {
        val ride = rideRepository.findById(rideId)

        if (ride.isEmpty) {
            logger.info("fun finishRide - No ride with id $rideId")
            return ResultTO(HttpStatus.NOT_FOUND, listOf("No ride with id $rideId"))
        }

        if (ride.get().status != RideStatus.IN_PROGRESS) {
            logger.info("fun finishRide - Cannot finish ride $rideId that is not ${RideStatus.IN_PROGRESS}")
            return ResultTO(HttpStatus.BAD_REQUEST, listOf("Cannot finish ride $rideId that is not ${RideStatus.IN_PROGRESS}"))
        }

        if (ride.get().driverUsername != driverUsername) {
            logger.info("fun finishRide - Driver username does not match the driver assigned to ride $rideId")
            return ResultTO(HttpStatus.BAD_REQUEST, listOf("Driver username ($driverUsername) does not match the driver assigned to ride $rideId (${ride.get().driverUsername})"))
        }

        ride.get().status = RideStatus.FINISHED
        ride.get().endTime = OffsetDateTime.now()
        rideRepository.save(ride.get())

        val event = RideFinishedEvent(
            rideFinishedEventId = eventCounterService.getNextId(EventName.RIDE_FINISHED),
            rideId = rideId,
            driverUsername = driverUsername,
            startTime = ride.get().startTime!!,
            endTime = ride.get().endTime!!,
            driverEarning = ride.get().driverEarnings,
        )
        template.convertAndSend(rideExchange.name, rideFinishedTopic, event)
        logger.info("Ride $rideId finished")

        return ResultTO(HttpStatus.OK, listOf("Ride finished successfully"))
    }

    override fun getCurrentPassengerRide(passengerUsername: String): ResponseInterface {
        val currentRides = rideRepository.findByPassengerUsernameAndStatus(passengerUsername, RideStatus.IN_PROGRESS)
        if (currentRides.isEmpty()) {
            return ResultTO(HttpStatus.NOT_FOUND)
        }
        if (currentRides.size > 1) {
            return ResultTO(HttpStatus.INTERNAL_SERVER_ERROR, listOf("Passenger $passengerUsername has more than one Ride currently in progress."))
        }
        return currentRides[0].let {
            RideResponse(
                rideId = it.rideId!!,
                status = it.status,
                pickupId = it.pickupId,
                destinationId = it.destinationId,
                fare = it.fare,
                distance = it.distance,
                duration = it.duration,
            )
        }
    }

    override fun getCurrentDriverRide(driverUsername: String): ResponseInterface {
        val currentRides = rideRepository.findByDriverUsernameAndStatus(driverUsername, RideStatus.IN_PROGRESS)
        if (currentRides.isEmpty()) {
            return ResultTO(HttpStatus.NOT_FOUND)
        }
        if (currentRides.size > 1) {
            return ResultTO(HttpStatus.INTERNAL_SERVER_ERROR, listOf("Driver $driverUsername has more than one Ride currently in progress."))
        }
        return currentRides[0].let {
            RideResponse(
                rideId = it.rideId!!,
                status = it.status,
                pickupId = it.pickupId,
                destinationId = it.destinationId,
                fare = it.fare,
                distance = it.distance,
                duration = it.duration,
            )
        }
    }
}


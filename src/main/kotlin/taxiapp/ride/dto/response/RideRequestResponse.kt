package taxiapp.ride.dto.response

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.http.HttpStatus

data class RideRequestResponse (
    val rideId: Long,
    var pickupId: String,
    var destinationId: String,
    var distance: Int,
    var duration: Long,
    var fare: Int,
    @JsonIgnore
    override val httpStatus: HttpStatus = HttpStatus.OK,
    @JsonIgnore
    override val messages: List<String>? = null
) : ResponseInterface

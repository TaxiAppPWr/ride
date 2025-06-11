package taxiapp.ride.dto.response

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.http.HttpStatus
import taxiapp.ride.model.Ride
import taxiapp.ride.model.RideStatus

data class RideResponse (
    val rideId: Long,
    val status: RideStatus,

    @JsonIgnore
    override val httpStatus: HttpStatus = HttpStatus.OK,
    @JsonIgnore
    override val messages: List<String>? = null
) : ResponseInterface

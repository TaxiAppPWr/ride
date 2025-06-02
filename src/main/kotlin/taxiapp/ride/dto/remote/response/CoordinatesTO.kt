package taxiapp.ride.dto.remote.response;

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.http.HttpStatus

data class CoordinatesTO(
    val latitude: Double,
    val longitude: Double
)

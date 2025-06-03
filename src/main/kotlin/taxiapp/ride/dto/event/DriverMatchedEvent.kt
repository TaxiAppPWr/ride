package taxiapp.ride.dto.event

import java.time.LocalDateTime

data class DriverMatchedEvent(
    val rideId: Long,
    val driverId: String,
    val matchedAt: LocalDateTime,
)

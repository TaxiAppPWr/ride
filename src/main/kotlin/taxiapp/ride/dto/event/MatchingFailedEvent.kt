package taxiapp.ride.dto.event

import java.time.LocalDateTime

data class MatchingFailedEvent(
    val rideId: Long,
    val reason: String,
    val failedAt: LocalDateTime
)
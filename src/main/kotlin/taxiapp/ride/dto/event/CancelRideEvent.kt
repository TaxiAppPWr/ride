package taxiapp.ride.dto.event

data class CancelRideEvent(
    val cancelRideEventId: Long,
    val rideId: Long,
    val refundPercentage: Int,
    val driverId: String,
)
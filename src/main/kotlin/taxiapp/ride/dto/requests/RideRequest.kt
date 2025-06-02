package taxiapp.ride.dto.requests

data class RideRequest (
    val passengerUsername: String,
    val originId: String,
    val destinationId: String
)

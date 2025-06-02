package taxiapp.ride.dto.remote.response

data class MatchingStartedResponse(
    val rideId: Long,
    val status: String,
    val message: String
)

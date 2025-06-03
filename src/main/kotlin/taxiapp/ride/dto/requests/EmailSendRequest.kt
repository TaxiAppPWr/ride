package taxiapp.ride.dto.requests

data class EmailSendRequest(
    val recipient: String,
    val subject: String,
    val body: String,
)
package taxiapp.ride.dto.requests

data class EmailNotificationRequest(
    val recipient: String,
    val subject: String,
    val body: String,
)
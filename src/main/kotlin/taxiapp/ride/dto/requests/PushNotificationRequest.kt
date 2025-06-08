package taxiapp.ride.dto.requests

data class PushNotificationRequest(
    val username: String,
    val title: String,
    val body: String,
)
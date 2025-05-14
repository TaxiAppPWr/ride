package taxiapp.driver.dto.event

data class UserConfirmedEvent(
    val userConfirmedEventId: Long,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val isDriver: Boolean
)
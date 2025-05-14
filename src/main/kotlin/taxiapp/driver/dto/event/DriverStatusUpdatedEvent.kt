package taxiapp.driver.dto.event

data class DriverStatusUpdatedEvent(
    val driverStatusUpdatedId: Long,
    val username: String,
    val verificationStatus: Boolean
)

package taxiapp.driver.dto.event

data class DriverCreatedEvent(
    val driverCreatedEventId: Long,
    val username: String,
    val firstname: String,
    val lastname: String,
    val email: String,
    val phoneNumber: String
)
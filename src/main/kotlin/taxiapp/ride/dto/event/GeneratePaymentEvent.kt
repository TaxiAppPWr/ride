package taxiapp.ride.dto.event

data class GeneratePaymentEvent(
    val generatePaymentEventId: Long,
    val rideId: Long,
    val amount: Int,
    val passengerUsername: String
)
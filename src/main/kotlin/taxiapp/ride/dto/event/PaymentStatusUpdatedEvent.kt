package taxiapp.ride.dto.event

data class PaymentStatusUpdatedEvent(
    val passengerPaymentStatusUpdatedEventId: Long,
    val paymentId: String,
    val status: String
)
package taxiapp.ride.model

enum class PaymentStatus {
    NEW,
    SUCCESS,
    COMPLETED,
    WAITING_FOR_CONFIRMATION,
    PENDING,
    CANCELED,
    FAILED
}

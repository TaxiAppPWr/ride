package taxiapp.ride.model

enum class RideStatus {
    NEW,
    AWAITING_PAYMENT,
    PAYMENT_RECEIVED,
    AWAITING_DRIVER,
    IN_PROGRESS,
    FINISHED,
    CANCELLED
}

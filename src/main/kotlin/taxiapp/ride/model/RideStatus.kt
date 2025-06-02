package taxiapp.ride.model

enum class RideStatus {
    NEW,
    AWAITING_PAYMENT,
    PAYMENT_RECEIVED,
    AWAITING_DRIVER,
    CANCELLED,
    IN_PROGRESS,
    FINISHED
}

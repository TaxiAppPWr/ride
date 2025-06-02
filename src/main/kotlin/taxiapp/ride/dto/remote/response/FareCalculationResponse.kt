package taxiapp.ride.dto.remote.response

data class FareCalculationResponse(
    val distance: Int,
    val duration: Long,
    val amount: Int,
    val driverProfit: Int
)
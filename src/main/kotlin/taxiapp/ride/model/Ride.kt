package taxiapp.ride.model;

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity(name = "ride")
data class Ride (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var rideId: Long?  = null,

    @Column
    var pickupId: String,
    @Column
    var destinationId: String,
    @Column
    var distance: Int,
    @Column
    var duration: Long,
    @Column
    var passengerUsername: String,
    @Column
    var fare: Int,
    @Column
    var driverEarnings: Int,
    @Column
    var driverUsername: String? = null,
    @Column
    @Enumerated(EnumType.STRING)
    var status: RideStatus = RideStatus.NEW,
    @Column
    var startTime: OffsetDateTime? = null,
    @Column
    var endTime: OffsetDateTime? = null
)
package taxiapp.ride.repository;

import taxiapp.ride.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import taxiapp.ride.model.RideStatus

interface RideRepository : JpaRepository<Ride, Long> {
    fun findByPassengerUsernameAndStatusNotIn(passengerUsername: String, status: Collection<RideStatus>): List<Ride>
    fun findByDriverUsernameAndStatusNotIn(driverUsername: String, status: Collection<RideStatus>): List<Ride>
}

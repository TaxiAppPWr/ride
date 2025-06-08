package taxiapp.ride.repository;

import taxiapp.ride.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import taxiapp.ride.model.RideStatus

interface RideRepository : JpaRepository<Ride, Long> {
    fun findByPassengerUsernameAndStatus(passengerUsername: String, status: RideStatus): List<Ride>
    fun findByDriverUsernameAndStatus(driverUsername: String, status: RideStatus): List<Ride>
}

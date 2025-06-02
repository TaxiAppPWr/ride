package taxiapp.ride.repository;

import taxiapp.ride.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

interface RideRepository : JpaRepository<Ride, Long>

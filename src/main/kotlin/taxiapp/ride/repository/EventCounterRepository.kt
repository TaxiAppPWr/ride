package taxiapp.ride.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taxiapp.ride.model.EventCounter
import taxiapp.ride.model.EventName

interface EventCounterRepository : JpaRepository<EventCounter, Long> {
    fun findByName(name: EventName): EventCounter?
}

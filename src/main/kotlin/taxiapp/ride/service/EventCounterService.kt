package taxiapp.ride.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import taxiapp.ride.model.EventName
import taxiapp.ride.model.EventCounter
import taxiapp.ride.repository.EventCounterRepository

@Service
class EventCounterService @Autowired constructor(
    private val eventCounterRepository: EventCounterRepository,
) {
    fun getNextId(event: EventName) : Long {
        val eventCounter = eventCounterRepository.findByName(event)
        if (eventCounter == null) {
            eventCounterRepository.save(EventCounter(name = event, eventId = 0))
            return 0
        }
        eventCounter.eventId += 1
        eventCounterRepository.save(eventCounter)
        return eventCounter.eventId
    }
}


package taxiapp.ride.model;

import jakarta.persistence.*

@Entity(name = "event_counters")
data class EventCounter (
    @Id
    @Enumerated(EnumType.STRING)
    var name: EventName,
    @Column
    var eventId: Long,
)
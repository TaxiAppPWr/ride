package taxiapp.ride.messaging

import com.rabbitmq.client.Channel
import org.springframework.amqp.core.Message
import taxiapp.ride.service.RideService
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import taxiapp.ride.dto.event.MatchingFailedEvent
import taxiapp.ride.dto.event.PassengerPaymentStatusUpdatedEvent

@RabbitListener(queues = ["\${rabbit.queue.ride.name}"])
@Component
class RideMessagesReceiver(
    private val rideService: RideService
) {

    @RabbitHandler
    fun receivePaymentStatusUpdateEvent(event: PassengerPaymentStatusUpdatedEvent) {
        rideService.paymentStatusUpdated(event.rideId, event.status)
    }

    @RabbitHandler
    fun receiveDriverMatchingFailedEvent(event: MatchingFailedEvent) {
        println("Received matching failed event")
        rideService.cancelRide(event.rideId, 100)
    }

    @RabbitHandler(isDefault = true)
    fun receiveDefault(event: Any) {
        // Do nothing
    }
}
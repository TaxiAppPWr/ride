package taxiapp.driver.messaging

import taxiapp.driver.dto.event.DriverCreatedEvent
import taxiapp.driver.dto.event.DriverAuthenticationApprovedEvent
import taxiapp.driver.dto.event.UserConfirmedEvent
import taxiapp.driver.service.DriversService
import org.springframework.amqp.rabbit.annotation.RabbitHandler
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@RabbitListener(queues = ["\${rabbit.queue.driver.name}"])
@Component
class UserMessagesReceiver(
    private val driverService: DriversService
) {
    @RabbitHandler
    fun receiveUserConfirmedEvent(event: UserConfirmedEvent) {
        driverService.addDriver(event)
    }

    @RabbitHandler
    fun receiveFormSubmittedEvent(event: DriverAuthenticationApprovedEvent) {
        driverService.driverVerified(event)
    }

    @RabbitHandler(isDefault = true)
    fun receiveDefault(event: Any) {
        // Do nothing
    }
}
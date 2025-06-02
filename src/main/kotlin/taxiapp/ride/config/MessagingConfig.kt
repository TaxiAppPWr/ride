package taxiapp.ride.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.core.TopicExchange
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MessagingConfig {
    @Value("\${rabbit.exchange.ride.name}")
    private val rideExchangeName: String? = null

    @Value("\${rabbit.exchange.driver-matching.name}")
    private val driverMatchingExchangeName: String? = null

    @Value("\${rabbit.queue.ride.name}")
    private val rideQueueName: String? = null

    @Value("\${rabbit.routing-key.ppayment.status-updated}")
    private val passengerPaymentStatusUpdated: String? = null

    @Value("\${rabbit.routing-key.driver-matching.failed}")
    private val driverMatchingFailed: String? = null


    @Bean
    fun rideExchange(): TopicExchange {
        return TopicExchange("$rideExchangeName")
    }

    @Bean
    fun driverMatchingExchange(): DirectExchange {
        return DirectExchange("$driverMatchingExchangeName")
    }

    @Bean
    fun rideQueue(): Queue {
        return QueueBuilder.durable("$rideQueueName").build()
    }

    @Bean
    fun paymentStatusUpdatedBinding(@Qualifier("rideExchange") exchange: TopicExchange, rideQueue: Queue): Binding {
        return BindingBuilder.bind(rideQueue).to(exchange).with("$passengerPaymentStatusUpdated")
    }

    @Bean
    fun driverMatchingFailedBinding(@Qualifier("driverMatchingExchange") exchange: DirectExchange, rideQueue: Queue): Binding {
        return BindingBuilder.bind(rideQueue).to(exchange).with("$driverMatchingFailed")
    }
}
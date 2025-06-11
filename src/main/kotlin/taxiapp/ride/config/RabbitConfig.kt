package taxiapp.ride.config
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper
import taxiapp.ride.dto.event.DriverMatchedEvent
import taxiapp.ride.dto.event.MatchingFailedEvent
import taxiapp.ride.dto.event.PassengerPaymentStatusUpdatedEvent

@Configuration
class RabbitConfig {

    @Bean
    fun jsonMessageConverter(): MessageConverter {
        val mapper = ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

        val typeMapper = DefaultJackson2JavaTypeMapper().apply {
            setTrustedPackages("*")
            setIdClassMapping(
                mapOf(
                    "taxiapp.passengerpayment.dto.event.PassengerPaymentStatusUpdatedEvent" to PassengerPaymentStatusUpdatedEvent::class.java,
                    "org.taxiapp.matching.dto.events.out.MatchingFailedEvent" to MatchingFailedEvent::class.java,
                    "org.taxiapp.matching.dto.events.out.DriverMatchedEvent" to DriverMatchedEvent::class.java
                )
            )
        }

        return Jackson2JsonMessageConverter(mapper).apply {
            setJavaTypeMapper(typeMapper)
        }
    }

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory, jsonMessageConverter: MessageConverter): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = jsonMessageConverter
        return rabbitTemplate
    }
}
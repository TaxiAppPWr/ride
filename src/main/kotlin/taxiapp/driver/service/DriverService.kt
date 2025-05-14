package taxiapp.driver.service

import java.time.LocalDateTime
import org.springframework.web.multipart.MultipartFile
import taxiapp.driver.dto.event.DriverCreatedEvent
import taxiapp.driver.dto.event.DriverAuthenticationApprovedEvent
import taxiapp.driver.dto.event.UserConfirmedEvent
import taxiapp.driver.dto.response.ResponseInterface
import taxiapp.driver.model.Driver

interface DriversService {
    fun getAllDrivers(): ResponseInterface

    fun getVerifiedDrivers(): ResponseInterface

    fun getVerifiedDriversPersonalInfo(): ResponseInterface

    fun addDriver(user: UserConfirmedEvent)

    fun driverVerified(approvedEvent: DriverAuthenticationApprovedEvent)
}
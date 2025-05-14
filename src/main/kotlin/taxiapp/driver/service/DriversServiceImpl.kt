package taxiapp.driver.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.nio.file.Files
import java.nio.file.Paths
import taxiapp.driver.repository.DriversRepository
import org.springframework.http.HttpStatus
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.core.RabbitTemplate
import taxiapp.driver.dto.event.DriverCreatedEvent
import taxiapp.driver.dto.event.DriverAuthenticationApprovedEvent
import taxiapp.driver.dto.event.DriverStatusUpdatedEvent
import taxiapp.driver.dto.event.UserConfirmedEvent
import taxiapp.driver.dto.response.ResponseInterface
import taxiapp.driver.dto.response.ResultTO
import taxiapp.driver.dto.response.DriverTO
import taxiapp.driver.dto.response.DriverPersonalInfoTO
import taxiapp.driver.dto.response.driverdata.DriverData
import taxiapp.driver.dto.response.driverdata.DriverPersonalData
import taxiapp.driver.dto.response.driverdata.DriverLegalData
import taxiapp.driver.dto.response.DriverListTO
import taxiapp.driver.dto.response.DriverPersonalInfoListTO
import taxiapp.driver.model.Driver
import taxiapp.driver.model.DriverLegalInfo

@Service
class DriversServiceImpl @Autowired constructor(
    private val driversRepository: DriversRepository,
    val exchange: TopicExchange,
    val template: RabbitTemplate
) : DriversService {

    @Value("\${rabbit.topic.drivers.created}")
    private val driverCreatedTopic: String? = null

    override fun getAllDrivers(): ResponseInterface {
        var drivers = driversRepository.findAll()
        if (drivers.isEmpty()) {
            return ResultTO(
                httpStatus = HttpStatus.NOT_FOUND,
                messages = listOf("No drivers found.")
            )
        }
        val driversData = drivers.map{ driver -> 
            DriverData(
                DriverPersonalData(
                    username = driver.username,
                    firstname = driver.firstname,
                    lastname = driver.lastname,
                    email = driver.email,
                    phoneNumber = driver.phoneNumber
                ),
                if (driver.driverLegalInfo != null)
                    DriverLegalData(
                        driverLicenceNumber = driver.driverLegalInfo!!.driverLicenceNumber ,
                        registrationDocumentNumber = driver.driverLegalInfo!!.registration_document_number ,
                        plateNumber = driver.driverLegalInfo!!.plateNumber ,
                        pesel = driver.driverLegalInfo!!.pesel ,
                        street = driver.driverLegalInfo!!.street ,
                        buildingNumber = driver.driverLegalInfo!!.buildingNumber ,
                        apartmentNumber = driver.driverLegalInfo!!.apartmentNumber ,
                        postCode = driver.driverLegalInfo!!.postCode ,
                        country = driver.driverLegalInfo!!.country
                    ) 
                else 
                    null
            )
        }
        
        return DriverListTO(drivers = driversData, httpStatus = HttpStatus.OK)
    }

    override fun getVerifiedDrivers(): ResponseInterface {
        val drivers = driversRepository.findAll()

        val verifiedDrivers = drivers
            .filter { it.verified }
        if (verifiedDrivers.size == 0) {
            return ResultTO(
                httpStatus = HttpStatus.NOT_FOUND,
                messages = listOf("No verified drivers found.")
            )
        }

        val driversData = verifiedDrivers.map{ driver -> 
            DriverData(
                DriverPersonalData(
                    username = driver.username,
                    firstname = driver.firstname,
                    lastname = driver.lastname,
                    email = driver.email,
                    phoneNumber = driver.phoneNumber
                ),
                DriverLegalData(
                    driverLicenceNumber = driver.driverLegalInfo!!.driverLicenceNumber ,
                    registrationDocumentNumber = driver.driverLegalInfo!!.registration_document_number ,
                    plateNumber = driver.driverLegalInfo!!.plateNumber ,
                    pesel = driver.driverLegalInfo!!.pesel ,
                    street = driver.driverLegalInfo!!.street ,
                    buildingNumber = driver.driverLegalInfo!!.buildingNumber ,
                    apartmentNumber = driver.driverLegalInfo!!.apartmentNumber ,
                    postCode = driver.driverLegalInfo!!.postCode ,
                    country = driver.driverLegalInfo!!.country
                ) 
            )
        }
        
        return DriverListTO(drivers = driversData, httpStatus = HttpStatus.OK)
    }
    

    override fun addDriver(user: UserConfirmedEvent) {
        if (!user.isDriver) {
            return
        }

        val newDriver = Driver(
            username = user.username,
            firstname = user.firstName,
            lastname = user.lastName,
            email = user.email,
            phoneNumber = user.phoneNumber,
            verified = false,
            lastUpdated = LocalDateTime.now()
        )
    
        driversRepository.save(newDriver)
        val event = DriverCreatedEvent(
            driverCreatedEventId = newDriver.id!!,
            username = newDriver.username,
            firstname = newDriver.firstname,
            lastname = newDriver.lastname,
            email = newDriver.email,
            phoneNumber = newDriver.phoneNumber
        )

        template.convertAndSend(exchange.name, "$driverCreatedTopic", event)
    }

    override fun getVerifiedDriversPersonalInfo(): ResponseInterface { 

        val drivers = driversRepository.findAll()
        val verifiedDrivers = drivers
            .filter { it.verified }
        if (verifiedDrivers.isEmpty()) {
            return ResultTO(
                httpStatus = HttpStatus.NOT_FOUND,
                messages = listOf("No verified drivers found.")
            )
        }

        val driversPersonalData = verifiedDrivers.map { driver ->
                DriverPersonalData(
                    username = driver.username,
                    firstname = driver.firstname,
                    lastname = driver.lastname,
                    email = driver.email,
                    phoneNumber = driver.phoneNumber
                )
            }
            
        return DriverPersonalInfoListTO(driversPersonalData, HttpStatus.OK)
    }

    override fun driverVerified(approvedEvent: DriverAuthenticationApprovedEvent) {
        val driver = driversRepository.findByUsername(approvedEvent.username)

        if (driver == null)
            return;

        val legalInfo = DriverLegalInfo(
            driverLicenceNumber = approvedEvent.driverLicenceNumber,
            registration_document_number = approvedEvent.registrationDocumentNumber,
            plateNumber = approvedEvent.plateNumber,
            pesel = approvedEvent.pesel,
            street = approvedEvent.address.street,
            buildingNumber = approvedEvent.address.buildingNumber,
            apartmentNumber = approvedEvent.address.apartmentNumber,
            postCode = approvedEvent.address.postCode,
            city = approvedEvent.address.city,
            country = approvedEvent.address.country
        )

        driver.driverLegalInfo = legalInfo;
        driver.verified = true;

        driversRepository.save(driver);

        val event = DriverStatusUpdatedEvent(
            driverStatusUpdatedId =  driver.id!!,
            username = driver.username,
            verificationStatus = driver.verified
        )
    }

}


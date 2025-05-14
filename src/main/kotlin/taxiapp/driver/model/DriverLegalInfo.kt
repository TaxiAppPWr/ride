package taxiapp.driver.model

import jakarta.persistence.*
import java.util.*

@Entity(name = "driver_legal_info")
class DriverLegalInfo(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column
    var driverLicenceNumber: String,
    @Column
    var registration_document_number: String,
    @Column
    var plateNumber: String,
    @Column
    var pesel: String,
    @Column
    var street: String,
    @Column
    val buildingNumber: String,
    @Column
    val apartmentNumber: String?,
    @Column
    val postCode: String,
    @Column
    val city: String,
    @Column
    val country: String
)
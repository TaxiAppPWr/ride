package taxiapp.driver.dto.response.driverdata

data class DriverLegalData(
    var driverLicenceNumber: String,
    var registrationDocumentNumber: String,
    var plateNumber: String,
    var pesel: String,
    var street: String,
    val buildingNumber: String,
    val apartmentNumber: String?,
    val postCode: String,
    val country: String
)
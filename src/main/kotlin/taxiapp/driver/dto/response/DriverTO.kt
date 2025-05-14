package taxiapp.driver.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.http.HttpStatus
import taxiapp.driver.dto.response.driverdata.DriverPersonalData
import taxiapp.driver.dto.response.driverdata.DriverLegalData

data class DriverTO(
    val driverPersonalData: DriverPersonalData,
    val driverLegalData: DriverLegalData?,

    @JsonIgnore
    override val httpStatus: HttpStatus = HttpStatus.OK,
    @JsonIgnore
    override val messages: List<String>? = null,
    
) : ResponseInterface

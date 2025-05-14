package taxiapp.driver.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.http.HttpStatus
import taxiapp.driver.dto.response.driverdata.DriverData

data class DriverListTO(
    val drivers: List<DriverData>,

    @JsonIgnore
    override val httpStatus: HttpStatus = HttpStatus.OK,
    @JsonIgnore
    override val messages: List<String>? = null,
    
) : ResponseInterface

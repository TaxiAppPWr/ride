package taxiapp.ride.dto.response

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.http.HttpStatus

data class PaymentLinkResponse(
    val status: String,
    val response: PaymentLinkBody,
    @JsonIgnore
    override val httpStatus: HttpStatus = HttpStatus.OK,
    @JsonIgnore
    override val messages: List<String>? = null
) : ResponseInterface
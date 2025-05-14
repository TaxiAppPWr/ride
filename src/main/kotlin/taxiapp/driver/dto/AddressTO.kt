package taxiapp.driver.dto;

import jakarta.persistence.*;

data class AddressTO (
    val street: String,
    val buildingNumber: String,
    val apartmentNumber: String?,
    val postCode: String,
    val city: String,
    val country: String
)
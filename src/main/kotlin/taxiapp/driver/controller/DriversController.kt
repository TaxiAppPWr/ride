package taxiapp.driver.controller;

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.beans.factory.annotation.Autowired
import kotlinx.coroutines.delay
import kotlin.random.Random
import taxiapp.driver.model.Driver
import taxiapp.driver.repository.DriversRepository
import taxiapp.driver.dto.VerificationDataDTO
import taxiapp.driver.service.DriversService

@RestController
@RequestMapping("api")
class DriversController @Autowired constructor(
    private val driversService: DriversService 
){    
    @GetMapping("/drivers/verified")
    fun getVerifiedDrivers(): ResponseEntity<Any> {
        return ResponseEntity.ok(driversService.getVerifiedDrivers());
    }

    @GetMapping("/drivers/all")
    fun getAll(): ResponseEntity<Any> { 
        return ResponseEntity.ok(driversService.getAllDrivers());
    }

}

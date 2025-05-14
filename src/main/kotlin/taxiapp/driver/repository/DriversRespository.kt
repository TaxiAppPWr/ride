package taxiapp.driver.repository;

import taxiapp.driver.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

interface DriversRepository : JpaRepository<Driver, Long> {
    fun findByUsername(username: String): Driver?
}

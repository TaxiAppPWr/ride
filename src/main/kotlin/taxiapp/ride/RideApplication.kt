package taxiapp.ride

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RideApplication

fun main(args: Array<String>) {
	runApplication<RideApplication>(*args)
}
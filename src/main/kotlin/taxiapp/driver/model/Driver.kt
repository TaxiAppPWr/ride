package taxiapp.driver.model;

import jakarta.persistence.*;
import java.time.LocalDateTime
import java.util.Date
import kotlin.booleanArrayOf
import taxiapp.driver.model.DriverLegalInfo

@Entity(name = "driver")
@Table(indexes = [
    Index(columnList = "username")
])
data class Driver (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column
    var username: String,
    @Column
    var firstname: String,
    @Column
    var lastname: String,
    @Column
    var email: String,
    @Column(nullable = true)
    var phoneNumber: String,
    @Column
    var verified: Boolean,
    @OneToOne
    @JoinColumn(name = "DriverLegalInfo_id", nullable = true)
    var driverLegalInfo: DriverLegalInfo? = null,
    @Column
    val lastUpdated: LocalDateTime
)
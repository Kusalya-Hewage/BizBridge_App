package com.example.bizbridgeapp.utils

import java.util.Locale
import kotlin.math.abs

object LocationUtils {

    // Default Colombo coordinates
    const val DEFAULT_LAT = 6.9271
    const val DEFAULT_LNG = 79.8612

    /**
     * Checks if coordinates are roughly within Sri Lanka
     * (lat ~ 5.8 to 10.0, lng ~ 79.5 to 82.2).
     */
    fun isWithinSriLanka(lat: Double, lng: Double): Boolean {
        return lat in 5.8..10.0 && lng in 79.5..82.2
    }

    /**
     * Map of known Sri Lankan locations to coordinates (ordered from most specific to broader regions).
     */
    val KNOWN_LOCATIONS = listOf(
        // Galle & Southern Coast
        "unawatuna" to (6.0108 to 80.2497),
        "rumassala" to (6.0185 to 80.2435),
        "thalpe" to (5.9967 to 80.2828),
        "habaraduwa" to (5.9897 to 80.3019),
        "ahangama" to (5.9723 to 80.3644),
        "midigama" to (5.9678 to 80.3956),
        "weligama" to (5.9736 to 80.4287),
        "mirissa" to (5.9483 to 80.4716),
        "koggala" to (5.9926 to 80.3276),
        "hikkaduwa" to (6.1395 to 80.1063),
        "ambalangoda" to (6.2364 to 80.0543),
        "bentota" to (6.4259 to 79.9958),
        "beruwala" to (6.4788 to 79.9828),
        "aluthgama" to (6.4344 to 80.0031),
        "galle" to (6.0535 to 80.2210),
        "matara" to (5.9549 to 80.5550),
        "dikwella" to (5.9634 to 80.6974),
        "tangalle" to (6.0243 to 80.7941),
        "hambantota" to (6.1429 to 81.1212),

        // Western Province
        "moratuwa" to (6.7730 to 79.8816),
        "panadura" to (6.7132 to 79.9074),
        "wadduwa" to (6.6669 to 79.9298),
        "kalutara" to (6.5854 to 79.9607),
        "dehiwala" to (6.8402 to 79.8712),
        "mount lavinia" to (6.8344 to 79.8653),
        "ratmalana" to (6.8193 to 79.8863),
        "nugegoda" to (6.8649 to 79.8997),
        "maharagama" to (6.8480 to 79.9265),
        "kotte" to (6.8906 to 79.9015),
        "battaramulla" to (6.8998 to 79.9168),
        "malabe" to (6.9042 to 79.9548),
        "rajagiriya" to (6.9092 to 79.8932),
        "homagama" to (6.8436 to 80.0032),
        "kaduwela" to (6.9333 to 79.9833),
        "wattala" to (6.9895 to 79.8917),
        "kelaniya" to (6.9553 to 79.9186),
        "kiribathgoda" to (6.9806 to 79.9272),
        "ja-ela" to (7.0759 to 79.8920),
        "ja ela" to (7.0759 to 79.8920),
        "kandana" to (7.0475 to 79.8934),
        "katunayake" to (7.1690 to 79.8906),
        "negombo" to (7.2008 to 79.8736),
        "gampaha" to (7.0840 to 79.9925),
        "colombo" to (6.9271 to 79.8612),

        // Central & Hill Country
        "peradeniya" to (7.2599 to 80.5977),
        "kandy" to (7.2906 to 80.6337),
        "matale" to (7.4675 to 80.6234),
        "dambulla" to (7.8731 to 80.6517),
        "sigiriya" to (7.9570 to 80.7603),
        "nuwara eliya" to (6.9497 to 80.7891),
        "hatton" to (6.8953 to 80.5954),
        "ella" to (6.8667 to 81.0466),
        "badulla" to (6.9934 to 81.0550),
        "bandarawela" to (6.8296 to 80.9904),
        "haputale" to (6.7686 to 80.9509),

        // Sabaragamuwa & North Western
        "ratnapura" to (6.7056 to 80.3847),
        "kegalle" to (7.2513 to 80.3464),
        "kurunegala" to (7.4863 to 80.3623),
        "kuliyapitiya" to (7.4689 to 80.0401),
        "chilaw" to (7.5758 to 79.7953),
        "puttalam" to (8.0362 to 79.8283),

        // North Central & Northern
        "anuradhapura" to (8.3114 to 80.4037),
        "polonnaruwa" to (7.9403 to 81.0188),
        "jaffna" to (9.6615 to 80.0255),
        "vavuniya" to (8.7514 to 80.4971),
        "kilinochchi" to (9.3803 to 80.3770),
        "mannar" to (8.9810 to 79.9044),

        // Eastern
        "trincomalee" to (8.5874 to 81.2152),
        "batticaloa" to (7.7310 to 81.6747),
        "ampara" to (7.2975 to 81.6724),
        "arugam bay" to (6.8415 to 81.8344),
        "pasikuda" to (7.9256 to 81.5644)
    )

    /**
     * Resolves coordinates from an address string by keyword matching.
     */
    fun getCoordinatesFromAddress(address: String?): Pair<Double, Double>? {
        if (address.isNullOrBlank()) return null
        val lower = address.lowercase(Locale.ROOT)
        for ((name, coords) in KNOWN_LOCATIONS) {
            if (lower.contains(name)) {
                return coords
            }
        }
        return null
    }

    /**
     * Checks if given coordinates are within the Colombo default area.
     */
    fun isColomboDefault(lat: Double?, lng: Double?): Boolean {
        if (lat == null || lng == null) return true
        return abs(lat - DEFAULT_LAT) < 0.05 && abs(lng - DEFAULT_LNG) < 0.05
    }

    /**
     * Resolves coordinates given an address and existing coordinates.
     * If existing coordinates are default Colombo and the address specifies another town/city
     * (e.g. "Unawatuna Galle"), the address's coordinates take precedence.
     */
    fun resolveCoordinates(
        address: String?,
        existingLat: Double? = null,
        existingLng: Double? = null
    ): Pair<Double, Double> {
        val addressCoords = getCoordinatesFromAddress(address)

        if (existingLat == null || existingLng == null || existingLat == 0.0 || existingLng == 0.0) {
            return addressCoords ?: (DEFAULT_LAT to DEFAULT_LNG)
        }

        if (addressCoords != null) {
            val isAddrColombo = address?.lowercase(Locale.ROOT)?.contains("colombo") == true
            if (isColomboDefault(existingLat, existingLng) && !isAddrColombo) {
                return addressCoords
            }

            val dLat = existingLat - addressCoords.first
            val dLng = existingLng - addressCoords.second
            if (dLat * dLat + dLng * dLng > 0.01) {
                return addressCoords
            }
        }

        return existingLat to existingLng
    }
}


package action.helper

import java.text.SimpleDateFormat
import java.util.*

class DateHelper {
    /**
     * Returns the date and time in the specified format
     */
    fun getDate(
        pattern: String,
        plusYears: Int = 0,
        plusMonths: Int = 0,
        plusDays: Int = 0,
        plusHours: Int = 0,
        plusMinutes: Int = 0,
        plusSeconds: Int = 0
    ): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, plusYears)
        calendar.add(Calendar.MONTH, plusMonths)
        calendar.add(Calendar.DAY_OF_MONTH, plusDays)
        calendar.add(Calendar.HOUR, plusHours)
        calendar.add(Calendar.MINUTE, plusMinutes)
        calendar.add(Calendar.SECOND, plusSeconds)
        val formatter = SimpleDateFormat(pattern)
        return formatter.format(calendar.time)
    }
}
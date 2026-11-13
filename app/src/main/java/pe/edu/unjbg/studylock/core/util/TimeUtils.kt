package pe.edu.unjbg.studylock.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

object TimeUtils {
    const val MINUTE_MS = 60_000L

    fun todayKey(now: Long = System.currentTimeMillis()): String = dayKey(now)

    fun dayKey(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    fun startOfDay(epochMs: Long): Long =
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun dayLabel(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.forLanguageTag("es")).uppercase(Locale.getDefault())

    fun lastNDays(n: Int, now: Long = System.currentTimeMillis()): List<LocalDate> {
        val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        return (n - 1 downTo 0).map { today.minusDays(it.toLong()) }
    }

    fun formatClock(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        return "%02d:%02d".format(s / 60, s % 60)
    }

    fun formatMinutes(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        else -> "${minutes / 60} h ${minutes % 60} m"
    }

    fun relativeDays(epochMs: Long): String {
        if (epochMs == 0L) return "sin repasar"
        val days = ((System.currentTimeMillis() - epochMs) / (24 * 60 * 60 * 1000L)).toInt()
        return when {
            days <= 0 -> "hoy"
            days == 1 -> "ayer"
            else -> "hace $days días"
        }
    }
}

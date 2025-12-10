package com.github.sahyuya.socialvotes.util

import java.util.*
import java.util.regex.Pattern
import kotlin.math.max

object TimeParser {
    /**
     * ユーザー指定フォーマット:
     * 例: 2025y12m30d18h18min21s
     * y: year, m: month, d: day, h: hour, min: minute, s: second
     * 年・月・分・秒は任意、日と時は必須（仕様より）
     *
     * 簡易実装：不足分は現在日時から補完（仕様に合わせて拡張可能）
     */
    private val PATTERN = Pattern.compile("(\\d+)y|(\\d+)m|(\\d+)d|(\\d+)h|(\\d+)min|(\\d+)s")

    fun parseSafe(input: String): Long? {
        // A very permissive parser: extract numbers with suffixes
        val cal = Calendar.getInstance()
        // We will parse tokens like "2025y", "12m", "30d", "18h", "18min", "21s"
        val regex = Regex("(\\d+)(y|m|d|h|min|s)")
        val matches = regex.findAll(input)
        var hasDay = false
        var hasHour = false
        for (m in matches) {
            val num = m.groupValues[1].toInt()
            when (m.groupValues[2]) {
                "y" -> cal.set(Calendar.YEAR, num)
                "m" -> cal.set(Calendar.MONTH, num - 1)
                "d" -> { cal.set(Calendar.DAY_OF_MONTH, num); hasDay = true }
                "h" -> { cal.set(Calendar.HOUR_OF_DAY, num); hasHour = true }
                "min" -> cal.set(Calendar.MINUTE, num)
                "s" -> cal.set(Calendar.SECOND, num)
            }
        }
        // requirement: day and hour must be specified
        return if (hasDay && hasHour) cal.timeInMillis else null
    }
}

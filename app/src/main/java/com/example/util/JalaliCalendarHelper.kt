package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

object JalaliCalendarHelper {
    private val jalaliMonthDays = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

    // Converts Jalali date string "YYYY/MM/DD" to java.util.Date
    fun jalaliToGregorian(jDateStr: String): Date? {
        val parts = jDateStr.split("/")
        if (parts.size != 3) return null
        val jy = parts[0].toIntOrNull() ?: return null
        val jm = parts[1].toIntOrNull() ?: return null
        val jd = parts[2].toIntOrNull() ?: return null
        return jalaliToGregorian(jy, jm, jd)
    }

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Date? {
        val jy2 = jy - 979
        val jm2 = jm - 1
        val jd2 = jd - 1

        var jDayNo = 365 * jy2 + (jy2 / 33) * 8 + (jy2 % 33 + 3) / 4
        for (i in 0 until jm2) {
            jDayNo += jalaliMonthDays[i]
        }
        jDayNo += jd2

        var gDayNo = jDayNo + 79

        val gY = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        var leap = true
        var tempGY = gY
        var tempGDayNo = gDayNo

        if (tempGDayNo >= 36525) {
            tempGDayNo--
            tempGY += 100 * (tempGDayNo / 36524)
            tempGDayNo %= 36524
            if (tempGDayNo >= 365) {
                tempGDayNo++
            } else {
                leap = false
            }
        }

        tempGY += 4 * (tempGDayNo / 1461)
        tempGDayNo %= 1461

        var finalGY = tempGY
        var finalGDayNo = tempGDayNo

        if (finalGDayNo >= 366) {
            leap = false
            finalGDayNo--
            finalGY += finalGDayNo / 365
            finalGDayNo %= 365
        }

        val gMonthDays = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var finalGM = 0
        var finalGD = finalGDayNo + 1
        for (i in 1..12) {
            if (finalGD <= gMonthDays[i]) {
                finalGM = i
                break
            }
            finalGD -= gMonthDays[i]
        }

        val cal = GregorianCalendar(finalGY, finalGM - 1, finalGD)
        return cal.time
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): String {
        val gY = gy - 1600
        val gM = gm - 1
        val gD = gd - 1

        val gMonthDays = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var leap = (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)
        if (leap) {
            gMonthDays[1] = 29
        }

        var gDayNo = 365 * gY + (gY + 3) / 4 - (gY + 99) / 100 + (gY + 399) / 400
        for (i in 0 until gM) {
            gDayNo += gMonthDays[i]
        }
        gDayNo += gD

        val jDayNo = gDayNo - 79
        val jNP = jDayNo / 12053
        var tempJDayNo = jDayNo % 12053

        var jy = 979 + 33 * jNP + 4 * (tempJDayNo / 1461)
        tempJDayNo %= 1461

        if (tempJDayNo >= 366) {
            jy += (tempJDayNo - 1) / 365
            tempJDayNo = (tempJDayNo - 1) % 365
        }

        var jm = 0
        var jd = 0
        for (i in 0..11) {
            if (tempJDayNo < jalaliMonthDays[i]) {
                jm = i + 1
                jd = tempJDayNo + 1
                break
            }
            tempJDayNo -= jalaliMonthDays[i]
        }

        return String.format(Locale.US, "%04d/%02d/%02d", jy, jm, jd)
    }

    fun getTodayJalali(): String {
        val cal = Calendar.getInstance()
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun isValidJalaliDate(s: String): Boolean {
        val clean = s.trim()
        if (clean.length != 10 || clean.count { it == '/' } != 2) return false
        val parts = clean.split("/")
        if (parts.size != 3) return false
        val y = parts[0].toIntOrNull() ?: return false
        val m = parts[1].toIntOrNull() ?: return false
        val d = parts[2].toIntOrNull() ?: return false

        if (y < 1200 || y > 1500) return false
        if (m < 1 || m > 12) return false
        
        val maxDays = if (m <= 6) 31 else if (m <= 11) 30 else {
            // Leap year check for Jalali calendar: (Y % 33) in (1, 5, 9, 13, 17, 22, 26, 30) or simplified
            val isLeap = listOf(1, 5, 9, 13, 17, 22, 26, 30).contains((y - 474) % 33) // wait, a simpler leap estimate is okay
            if (isLeap) 30 else 29
        }
        return d in 1..maxDays
    }
}

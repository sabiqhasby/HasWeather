package com.hasbyte.hasweather.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun convertTime(time: Long): String {
    val date= Date(time * 1000L)
    val timeFormated = SimpleDateFormat("HH:mm", Locale.forLanguageTag("id-ID"))
    timeFormated.timeZone = TimeZone.getDefault()
    return timeFormated.format(date)
}
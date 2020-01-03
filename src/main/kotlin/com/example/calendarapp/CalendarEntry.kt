package com.example.calendarapp

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document("calendar-entries")
data class CalendarEntry (
    @Id val id: String,
    val title: String,
    val start: Date,
    val end: Date,
    val location: GeoJsonPoint,
    val notes: String?,
    var attended: Boolean
)
package com.example.calendarapp

import org.springframework.data.mongodb.repository.MongoRepository

interface CalendarEntriesRepository: MongoRepository<CalendarEntry, String> {
    fun findAllByOrderByStartDesc(): List<CalendarEntry>
}
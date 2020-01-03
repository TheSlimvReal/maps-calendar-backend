package com.example.calendarapp

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.geo.Metrics
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation.*
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.NearQuery
import org.springframework.data.mongodb.core.query.Query
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@RestController
class CalendarAppController {
    @Autowired lateinit var calendarRepository: CalendarEntriesRepository
    @Autowired lateinit var mongoTemplate: MongoTemplate

    @PostMapping("/registerLocation")
    fun getCalendarEntries(
            @RequestParam longitude: Double,
            @RequestParam latitude: Double
            ): List<CalendarEntry> {
        val operations = mutableListOf<AggregationOperation>()
        val location = GeoJsonPoint(longitude, latitude)
        val nearQuery = NearQuery.near(location, Metrics.KILOMETERS)
        operations.add(geoNear(nearQuery, "distance"))

        val leCriteria = Criteria("distance").lte(0.5)
        operations.add(match(leCriteria))

        val undoneCriteria = Criteria("attended").`is`(false)
        operations.add(match(undoneCriteria))

        val timeCriteria = Criteria("start").lte(LocalDateTime.now()).and("end").gte(LocalDateTime.now())
        operations.add(match(timeCriteria))

        val pipeline = newAggregation(operations)
        val result = mongoTemplate
                .aggregate(pipeline, CalendarEntry::class.javaObjectType, CalendarEntry::class.javaObjectType)
                .toMutableList()

        result.forEach { it.attended = true }
        calendarRepository.saveAll(result)

        return result
    }

    @GetMapping("/entries")
    fun getCalendarEntries(): List<CalendarEntry> {
        return calendarRepository.findAll()
    }

    @GetMapping("/date")
    fun getCalendarEntriesForDate(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") date: Date?
    ): List<CalendarEntry> {
        val timeCriteria: Criteria = if (date != null) {
            Criteria("start").lte(date).and("end").gte(date)
        } else {
            Criteria("start").lte(LocalDateTime.now()).and("end").gte(LocalDateTime.now())
        }
        return mongoTemplate
                .find(Query(timeCriteria), CalendarEntry::class.javaObjectType)
    }

    @PostMapping("/entries")
    fun saveCalendarEntry(
            @RequestParam(required = true) id: String,
            @RequestParam(required = true) title: String,
            @RequestParam(required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") start: Date,
            @RequestParam(required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") end: Date,
            @RequestParam(required = true) longitude: Double,
            @RequestParam(required = true) latitude: Double,
            @RequestParam(required = false) notes: String?
    ) {
        val point = GeoJsonPoint(longitude, latitude)
        val entry = CalendarEntry(
                id = id,
                title = title,
                start = start,
                end = end,
                location = point,
                notes = notes,
                attended = false
        )
        calendarRepository.save(entry)
    }

    @GetMapping("/report")
    fun getDoneReport(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") start: Date?,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") end: Date?
    ): Double {
        val allCriteria = Criteria()
        val attendedCriteria = Criteria("attended").`is`(true)
        var startVal = start ?: Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())

        val endVal = if (end != null) {
            end
        } else {
            val cal = Calendar.getInstance()
            cal.time = startVal
            cal.add(Calendar.DATE, -1)
            val tmp = startVal
            startVal = Date(cal.timeInMillis)
            tmp
        }
        println("start $startVal end $endVal")
        attendedCriteria.orOperator(
                Criteria.where("start").gte(startVal).lte(endVal),
                Criteria.where("end").gte(startVal).lte(endVal)
        )
        allCriteria.orOperator(
                Criteria.where("start").gte(startVal).lte(endVal),
                Criteria.where("end").gte(startVal).lte(endVal)
        )

        val all = mongoTemplate.count(Query(allCriteria), CalendarEntry::class.javaObjectType)
        val attended = mongoTemplate.count(Query(attendedCriteria), CalendarEntry::class.javaObjectType)
        println("all $all attended $attended")
        return if (all.compareTo(0) == 0) { 0.0 } else { attended/all.toDouble() }
    }
    
}
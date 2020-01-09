package com.example.calendarapp

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
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

@CrossOrigin(origins = ["*"])
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

        val timeCriteria = Criteria("start").lte(getCurrentTime()).and("end").gte(getCurrentTime())
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
        return calendarRepository.findAllByOrderByStartDesc()
    }

    @GetMapping("/date")
    fun getCalendarEntriesForDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) date: Date?
    ): List<CalendarEntry> {
        val newDate = date ?: getCurrentTime()
        val c = Calendar.getInstance()
        c.time = newDate
        c.add(Calendar.DATE, 1)
        val endDate = c.time
        val timeCriteria = Criteria("end").gte(newDate).and("start").lte(endDate)
        val sort = Sort.by(Sort.Direction.ASC, "start", "end")
        return mongoTemplate
                .find(Query(timeCriteria).with(sort), CalendarEntry::class.javaObjectType)
    }

    @PostMapping("/entries")
    fun saveCalendarEntry(
            @RequestParam(required = true) id: String,
            @RequestParam(required = true) title: String,
            @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) start: Date,
            @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) end: Date,
            @RequestParam(required = true) longitude: Double,
            @RequestParam(required = true) latitude: Double,
            @RequestParam(required = false) notes: String?
    ): CalendarEntry {
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
        return calendarRepository.save(entry)
    }

    @GetMapping("/report")
    fun getDoneReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) start: Date?,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) end: Date?
    ): Double {
        val startDate = start ?: getCurrentTime()
        val allCriteria = Criteria()
        var startVal = startDate

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
        allCriteria.orOperator(
                Criteria.where("start").gte(startVal).lt(endVal),
                Criteria.where("end").gte(startVal).lt(endVal)
        )

        val all = mongoTemplate.count(Query(allCriteria), CalendarEntry::class.javaObjectType)
        val attended = mongoTemplate.count(Query(allCriteria.and("attended").`is`(true)), CalendarEntry::class.javaObjectType)
        println("all $all attended $attended")
        return if (all.compareTo(0) == 0) { 0.0 } else { attended/all.toDouble() }
    }

    @GetMapping("/dayReport")
    fun getDayReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) date: Date?
    ): Double {
        val end = date ?: this.getCurrentTime()
        return this.getDoneReport(this.getStart(end, Calendar.DATE, -1), end)
    }

    @GetMapping("/weekReport")
    fun getWeekReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) date: Date?
    ): Double {
        val end = date ?: this.getCurrentTime()
        return this.getDoneReport(this.getStart(end, Calendar.DATE, -7), end)
    }

    @GetMapping("/monthReport")
    fun getMonthReport(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) date: Date?
    ): Double {
        val end = date ?: this.getCurrentTime()
        return this.getDoneReport(this.getStart(end, Calendar.MONTH, -1), end)
    }

    @GetMapping("/yearReport")
    fun getYearReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) date: Date?
    ): Double {
        val end = date ?: this.getCurrentTime()
        return this.getDoneReport(this.getStart(end, Calendar.YEAR, -1), end)
    }

    fun getStart(date: Date, diffType: Int, diffAmount: Int): Date {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(diffType, diffAmount)
        return cal.time
    }

    fun getCurrentTime(): Date {
        return Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
    }
}
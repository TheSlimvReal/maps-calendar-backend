package com.example.calendarapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@SpringBootApplication
class CalendarAppApplication

fun main(args: Array<String>) {
    runApplication<CalendarAppApplication>(*args)
}

@Bean fun corsConfigurationSource(): CorsConfigurationSource {
    val config = CorsConfiguration()
    config.allowedOrigins = listOf("*")
    config.allowedMethods = listOf("GET", "POST", "OPTIONS", "DELETE", "PUT", "PATCH")
    config.allowedHeaders = listOf("X-Requested-With", "Origin", "Content-Type", "Accept", "Authorization")
    config.allowCredentials = true
    val source = UrlBasedCorsConfigurationSource()
    source.registerCorsConfiguration("/**", config)
    return source
}

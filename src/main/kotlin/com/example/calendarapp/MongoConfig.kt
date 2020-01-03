package com.example.calendarapp

import com.mongodb.client.MongoClients
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDbFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@ComponentScan(basePackages = ["com.example.calendarapp"])
@EnableMongoRepositories(basePackages = ["com.example.calendarapp"])
class MongoConfig {
    @Bean fun mongoDbFactory(): MongoDbFactory {
        val mongoClient = MongoClients.create("mongodb+srv://test:pass@car-finder-cluster-v4ued.mongodb.net/test?retryWrites=true&w=majority")
        return SimpleMongoClientDbFactory(mongoClient, "calendar-db")
    }

    @Bean fun mongoTemplate(@Autowired dbFactory: MongoDbFactory): MongoTemplate {
        return MongoTemplate(dbFactory)
    }
}
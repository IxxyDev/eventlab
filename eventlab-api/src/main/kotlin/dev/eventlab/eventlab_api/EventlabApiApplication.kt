package dev.eventlab.eventlab_api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EventlabApiApplication

fun main(args: Array<String>) {
	runApplication<EventlabApiApplication>(*args)
}

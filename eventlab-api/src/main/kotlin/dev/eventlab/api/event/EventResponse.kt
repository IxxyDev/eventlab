package dev.eventlab.api.event

data class EventResponse(
  val id: String,
  val title: String,
  val description: String,
  val dateLabel: String,
  val timeLabel: String,
  val venue: String,
  val capacity: Int,
  val availableSeats: Int,
)

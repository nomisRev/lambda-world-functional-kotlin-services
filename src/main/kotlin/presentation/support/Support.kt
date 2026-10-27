package presentation.support

import kotlinx.coroutines.flow.Flow
import opensavvy.spine.api.DynamicResource

// Domain and SDK contracts for the talk, not substitutes for any library API.
@JvmInline
value class UserId(val value: Long)

data class User(val id: UserId, val name: String, val active: Boolean = true)

fun interface Users {
  suspend fun find(id: UserId): User?
}

fun interface Logger {
  fun info(message: String)
}

fun interface Request {
  fun cancel()
}

interface Sdk {
  // Single-shot callback: one terminal result; cancel must be thread-safe.
  fun fetch(id: UserId, callback: (Result<User>) -> Unit): Request
}

data class Event(val id: Long)

interface EventSource {
  val events: Flow<Event>
  suspend fun close()
}

fun interface EventConnector {
  suspend fun connect(): EventSource
}

// A small stand-in for application-specific event processing.
suspend fun handle(event: Event) {
  kotlinx.coroutines.yield()
  require(event.id >= 0)
}

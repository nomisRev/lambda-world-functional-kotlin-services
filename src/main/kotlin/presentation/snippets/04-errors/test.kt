package presentation.snippets.`04-errors`

import arrow.core.NonEmptyList
import arrow.core.raise.context.Raise
import arrow.core.raise.context.accumulate
import arrow.core.raise.context.either
import presentation.support.accumulating
import presentation.support.ensureOrAccumulate

@JvmInline
value class Username private constructor(val value: String) {
  companion object {
    context(_: Raise<NonEmptyList<String>>)
    operator fun invoke(value: String): Username = accumulate {
      ensureOrAccumulate(value.length >= 4) { "Username is too short" }
      ensureOrAccumulate(value.length <= 255) { "Username is too long" }
      Username(value)
    }
  }
}

@JvmInline
value class Email private constructor(val value: String) {
  companion object {
    context(_: Raise<NonEmptyList<String>>)
    operator fun invoke(value: String): Email = accumulate {
      ensureOrAccumulate(value.contains("@")) { "Email is invalid" }
      Email(value)
    }
  }
}

@ConsistentCopyVisibility
data class User private constructor(val username: Username, val email: Email) {
  companion object {
    context(_: Raise<NonEmptyList<String>>)
    operator fun invoke(username: String, email: String): User = accumulate {
      val username by accumulating { Username(username) }
      val email by accumulating { Email(email) }
      User(username, email)
    }
  }
}

fun main() {
  either<NonEmptyList<String>, User> {
    accumulate {
      User("username", "email@.com")
    }
  }.also(::println)
  either<NonEmptyList<String>, User> {
    accumulate {
      User("", "email")
    }
  }.also(::println)
  either<NonEmptyList<String>, User> {
    accumulate {
      User("username", "email")
    }
  }.also(::println)
}

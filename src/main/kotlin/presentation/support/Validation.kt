package presentation.support

import arrow.core.NonEmptyList
import arrow.core.raise.Accumulate
import arrow.core.raise.RaiseAccumulate.Value
import arrow.core.raise.accumulating
import arrow.core.raise.context.Raise
import arrow.core.raise.context.RaiseAccumulate
import arrow.core.raise.context.accumulate
import arrow.core.raise.ensureOrAccumulate
import arrow.core.raise.withError

// Application helpers adapted from ktor-arrow-example/Validation.kt.
// These are ordinary user code composed from real Arrow APIs, not library stubs.
data class InvalidField(val errors: NonEmptyList<String>, val name: String)
data class IncorrectInput(val errors: NonEmptyList<InvalidField>)

context(raise: Raise<E>)
inline fun <E, E2, A> validate(
  transform: (NonEmptyList<E2>) -> E,
  block: context(Accumulate<E2>) () -> A,
): A = raise.withError({ transform(it) }) { accumulate(block) }

context(raise: Raise<InvalidField>)
inline fun <A> validate(
  name: String,
  block: context(Accumulate<String>) () -> A,
): A = raise.withError({ InvalidField(it, name) }) { accumulate(block) }

context(raise: Accumulate<Error>)
inline fun <Error> ensureOrAccumulate(condition: Boolean, error: () -> Error): Value<Unit> =
  raise.ensureOrAccumulate(condition, error)

context(raise: Accumulate<Error>)
inline fun <Error, A> accumulating(block: context(RaiseAccumulate<Error>) () -> A): Value<A> =
  raise.accumulating(block)

context(_: Accumulate<String>)
fun String.notBlank(): String = also {
  ensureOrAccumulate(isNotBlank()) { "Cannot be blank" }
}

context(_: Accumulate<String>)
fun String.minSize(size: Int): String = also {
  ensureOrAccumulate(length >= size) { "is too short (minimum is $size characters)" }
}

context(_: Accumulate<String>)
fun String.maxSize(size: Int): String = also {
  ensureOrAccumulate(length <= size) { "is too long (maximum is $size characters)" }
}

@JvmInline
value class Username private constructor(val value: String) {
  companion object {
    context(_: Raise<InvalidField>)
    operator fun invoke(value: String): Username = validate("username") {
      Username(value.trim().notBlank().minSize(1).maxSize(25))
    }
  }
}

@JvmInline
value class Email private constructor(val value: String) {
  companion object {
    private val pattern = ".+@.+\\..+".toRegex()

    context(_: Raise<InvalidField>)
    operator fun invoke(value: String): Email = validate("email") {
      val normalized = value.notBlank().maxSize(350)
      ensureOrAccumulate(pattern.matches(normalized)) { "'$normalized' is invalid email" }
      Email(normalized)
    }
  }
}

data class RegisterUser(val username: Username, val email: Email)

---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Part 4</div>

# Failure can be a capability

## Keep the successful path direct

<!--
A sealed result describes all outcomes. Arrow gives us
a way to compose expected failures while keeping direct return values.
-->

---

# `Raise` adds an expected error to the signature

<DrawnAnnotation text="Raise<MissingUser>" />
<DrawnAnnotation text="raise(MissingUser(id))" />

```kotlin
import arrow.core.raise.Raise
import arrow.core.raise.context.raise

data class MissingUser(val id: UserId)

context(users: Users, _: Raise<MissingUser>)
suspend fun findUser(id: UserId): User =
  users.find(id) ?: raise(MissingUser(id))
```

<!--

MissingUser is an ordinary domain value, not an exception subclass. raise exits
this logical computation; it does not produce a User or resume after the call.
Unexpected exceptions and coroutine cancellation remain separate concerns.
Raise is a capability required by the function, just as Users is.
Source: https://arrow-kt.io/learn/typed-errors/working-with-typed-errors/
-->

---

# `either` supplies `Raise`

<DrawnAnnotation text="either" />

```kotlin
import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.context.raise
import arrow.core.raise.either

data class MissingUser(val id: UserId)

context(users: Users, _: Raise<MissingUser>)
suspend fun findUser(id: UserId): User =
  users.find(id) ?: raise(MissingUser(id))

// Example
context(_: Users)
suspend fun result(id: UserId): Either<MissingUser, User> =
  either { findUser(id) }
```

<!--

either supplies Raise and reifies its outcome at the boundary. The service did
not have to construct or unwrap Either at every intermediate step. A handler can
instead recover into its own response. Catching all exceptions would also risk
swallowing cancellation; that is not what we are doing.
-->

---

# `validate` turns checks into a field error

> Application helper · built on Arrow's `accumulate` and `withError`

<DrawnAnnotation text="context(Accumulate<String>)" />
<DrawnAnnotation text="InvalidField(it, name)" />

```kotlin
import arrow.core.NonEmptyList
import arrow.core.raise.Accumulate
import arrow.core.raise.context.Raise
import arrow.core.raise.context.accumulate
import arrow.core.raise.withError

data class InvalidField(val errors: NonEmptyList<String>, val name: String)

context(raise: Raise<InvalidField>)
inline fun <A> validate(
  name: String,
  block: context(Accumulate<String>) () -> A,
): A = raise.withError({ InvalidField(it, name) }) {
  accumulate(block)
}
```

<!--

This is the validate helper from the ktor-arrow-sample application, whose checkout
is named ktor-arrow-example. Arrow supplies accumulate and withError; the
application gives that composition a name. The block can collect several String
errors. withError groups those messages into one InvalidField with its field name.
The outer function needs Raise<InvalidField>, and its successful result stays A.
Source: /Users/simonvergauwen/Developer/ktor-arrow-example/src/main/kotlin/io/github/nomisrev/Validation.kt
-->

---

# A value class validates its construction

<DrawnAnnotation text="private constructor" />
<DrawnAnnotation text="validate(&quot;username&quot;)" />

```kotlin
import arrow.core.raise.context.Raise

@JvmInline
value class Username private constructor(val value: String) {
  companion object {
    context(_: Raise<InvalidField>)
    operator fun invoke(value: String): Username = validate("username") {
      val normalized = value.trim()
      Username(normalized.notBlank().minSize(1).maxSize(25))
    }
  }
}
```

<!--

The private constructor and public factory make validation the entry point.
notBlank, minSize, and maxSize are small application extensions with an
Accumulate<String> context, each built from Arrow's ensureOrAccumulate. They
return the String to compose checks. Blank input can report both blank and too
short; accumulate prevents returning the invalid value. Valid input is trimmed
and wrapped. These names and bounds follow the existing application example.
Source: /Users/simonvergauwen/Developer/ktor-arrow-example/src/main/kotlin/io/github/nomisrev/users/Username.kt
-->

---

# Independent fields accumulate before construction

<DrawnAnnotation text="validate(::IncorrectInput)" />
<DrawnAnnotation text="by accumulating" />

```kotlin
import arrow.core.raise.context.Raise

data class RegisterUserRequest(val username: String, val email: String) {
  context(_: Raise<IncorrectInput>)
  fun toRegisterUser(): RegisterUser = validate(::IncorrectInput) {
    val username by accumulating { Username(username) }
    val email by accumulating { Email(email) }
    RegisterUser(username, email)
  }
}
```

<!--

The generic validate overload takes an error constructor. Here it combines all
InvalidField values into IncorrectInput. Each accumulating block captures one
field's result without stopping the remaining checks. Reading the delegated
values allows constructing RegisterUser only when the needed values succeeded.
The original request also validates a password; two fields keep the same
composition legible. The application test asserts multiple errors per field and
errors across all invalid fields. This is actual accumulation, distinct from the
union of possible error types on the next slide.
Sources: /Users/simonvergauwen/Developer/ktor-arrow-example/src/main/kotlin/io/github/nomisrev/users/UserApi.kt
/Users/simonvergauwen/Developer/ktor-arrow-example/src/test/kotlin/io/github/nomisrev/ValidationSpec.kt
-->

---
snippets: false
---

# `|.` skips the call on an error

> Design proposal · KEEP-0462

<DrawnAnnotation text="User | MissingUser" />
<DrawnAnnotation text="|.name" />

```kotlin no-compile
error class MissingUser(val id: UserId)

suspend fun findUser(id: UserId): User | MissingUser

suspend fun greeting(id: UserId): String | MissingUser =
  findUser(id)|.name
```

<!--

This is proposed syntax, not an available compiler feature in this project.
The union-like return says success or a typed error. The proposed |. operator
accesses name on success and short-circuits with the error otherwise. Chaining
can widen the possible error alternatives; it does not accumulate multiple
failures from one run. Contrast only these semantics, not languages.
Source: https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0462-rich-errors.md
-->

---

# `suspend` and `context` compose

<DrawnAnnotation text="context(users: Users, logger: Logger, _: Raise<MissingUser>)" />
<DrawnAnnotation text="suspend fun" />

```kotlin
import arrow.core.raise.Raise
import arrow.core.raise.context.raise

data class MissingUser(val id: UserId)

context(users: Users, logger: Logger, _: Raise<MissingUser>)
suspend fun greeting(id: UserId): String {
  val user = users.find(id) ?: raise(MissingUser(id))
  logger.info("Welcoming ${user.id}")
  return "Hello, ${user.name}"
}
```

<!--

Read the complete contract. Users and Logger are the required services;
MissingUser is the expected failure; suspend permits asynchronous work; String
is success. This reaches the dependency, typed-error, and concurrency powers
that motivated the talk, using independent language and library mechanisms.
This is the tagless-final-style idea of a program against capabilities. It is
not a claim of higher-kinded effect polymorphism or purity enforcement.
-->

---

# The same function runs with in-memory users

<DrawnAnnotation text="object : Users" />
<DrawnAnnotation text="either { greeting(UserId(42)) }" />

```kotlin
import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.context.raise
import arrow.core.raise.either

data class MissingUser(val id: UserId)

context(users: Users, logger: Logger, _: Raise<MissingUser>)
suspend fun greeting(id: UserId): String {
  val user = users.find(id) ?: raise(MissingUser(id))
  logger.info("Welcoming ${user.id}")
  return "Hello, ${user.name}"
}

// Example
suspend fun example(logger: Logger): Either<MissingUser, String> {
  val users = object : Users {
    override suspend fun find(id: UserId): User? =
      if (id == UserId(42)) User(id, "Ada") else null
  }
  return with(users) {
    with(logger) { either { greeting(UserId(42)) } }
  }
}
```

<!--

The same greeting runs against an in-memory Users. The production boundary can
supply an SDK-backed implementation, the test a deterministic one. The Logger
can record messages. The Either boundary exposes an assertion-friendly value.
No framework container is required for this substitution.
-->

---

# The route turns a typed error into HTTP

<DrawnAnnotation text="with(users)" />
<DrawnAnnotation text="either { greeting(UserId(id)) }" />

```kotlin
import arrow.core.raise.Raise
import arrow.core.raise.context.raise
import arrow.core.raise.either
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.util.getValue

data class MissingUser(val id: UserId)

context(users: Users, logger: Logger, _: Raise<MissingUser>)
suspend fun greeting(id: UserId): String {
  val user = users.find(id) ?: raise(MissingUser(id))
  logger.info("Welcoming ${user.id}")
  return "Hello, ${user.name}"
}

// Example
fun Route.greetings(users: Users, logger: Logger) {
  get("/users/{id}/greeting") {
    val id: Long by call.pathParameters
    val result = with(users) {
      with(logger) { either { greeting(UserId(id)) } }
    }
    result.fold(
      { call.respondText("Unknown user", status = HttpStatusCode.NotFound) },
      { call.respondText(it) },
    )
  }
}
```

<!--

Bring the thread back to our original service: route, delegated parameter,
suspending service, required capabilities, domain failure, HTTP interpretation.
Users is supplied by application wiring, where the real SDK/database resources
live. MissingUser becomes 404 here; the business function knows no HTTP status.

-->

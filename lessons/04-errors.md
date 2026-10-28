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

<DrawnAnnotation text="Transaction" label="Type of the Exposed Typed SQL DSL" :connect="true" :geometry="{ label: { x: 0.5265, y: 0.1903 }, connector: { type: 'quadratic', start: { x: 0.3039, y: 0.2237 }, control: { x: 0.3295, y: 0.2200 }, end: { x: 0.3529, y: 0.1975 } } }"/>

```kotlin arrow
context(_: Transaction)
suspend fun insertUser(create: CreateUser): User =
  UserTable.insertReturning { insert ->
    insert[username] = create.username
    insert[email] = create.email
  }.toUser()
```

---

# `Raise` adds an expected error to the signature

<DrawnAnnotation text="catch" on="0" />
<DrawnAnnotation text="PLSQLException" on="0" label="`catch` specific errors and lift them to type signature"  :connect="true" :geometry="{ label: { x: 0.6281, y: 0.5964 }, connector: { type: 'quadratic', start: { x: 0.3260, y: 0.6474 }, control: { x: 0.3667, y: 0.6510 }, end: { x: 0.3826, y: 0.6154 } } }"/>
<DrawnAnnotation text="Raise<UserAlreadyExists>" on="1" />
<DrawnAnnotation text="raise(UserAlreadyExists(" on="1" label="Raise error into `context(Raise<UserAlreadyExists>)`"  :connect="true" :geometry="{ label: { x: 0.6246, y: 0.7644 } }"/>

```kotlin{1,3-5,10-13} arrow
import arrow.core.raise.Raise
import arrow.core.raise.context.raise

value class UserAlreadyExists(val username: String)

context(_: Transaction, _: Raise<UserAlreadyExists>)
suspend fun insertUser(create: CreateUser): User =
  catch({
    UserTable.insertReturning { insert ->
      insert[username] = create.username
      insert[email] = create.email
    }.toUser()
  }) { e: PLSQLException -> 
    if(e.uniqueViolation()) raise(UserAlreadyExists(create.username))
    else throw e
  }
```

---

# Handling the error supplies `Raise`

<TypeHint :line="3" context="Raise<UserAlreadyExists>">
<TypeHint :line="9" context="Raise<UserAlreadyExists>">

```kotlin arrow
import arrow.core.raise.Raise
import arrow.core.raise.context.raise

value class UserAlreadyExists(val username: String)

context(_: Transaction, _: Raise<UserAlreadyExists>)
suspend fun insertUser(create: CreateUser): User =
  catch({
    UserTable.insertReturning { insert ->
      insert[username] = create.username
      insert[email] = create.email
    }.toUser()
  }) { e: PLSQLException ->
    if(e.uniqueViolation()) raise(UserAlreadyExists(create.username))
    else throw e
  }

// Example
context(_: Transaction)
suspend fun insertUserOrNull(create: CreateUser): User? =
  recover({
    insertUser(create)
  }) { _ -> null }

context(_: Transaction)
suspend fun insertUserResult(create: CreateUser): Either<UserAlreadyExists, User> =
  either {
    insertUser(create)
  }
```

</TypeHint>
</TypeHint>

---

# Ktor Routes using Spine

<TypeHint :line="4" receiver="Routing">
<TypeHint :line="5" context="context(Raise<AlreadyExists>, Raise<InvalidUser>)">
<TypeHint :line="6" receiver="Transaction">
<DrawnAnnotation text="Raise<UserAlreadyExists>"/>
<DrawnAnnotation text="routeWithRaise" label="Spine's Arrow-kt support brings `Raise` into the context for all failures" on="0" :connect="true" :geometry="{ label: { x: 0.4065, y: 0.7630, width: 0.7047 }, connector: { type: 'quadratic', start: { x: 0.0856, y: 0.4185 }, control: { x: 0.0801, y: 0.5198 }, end: { x: 0.0956, y: 0.7297 } } }"/>
<DrawnAnnotation text="Transaction" on="1" color="var(--fundamentals-pink)"/>
<DrawnAnnotation text="transaction" on="1" color="var(--fundamentals-pink)"/>


```kotlin ktor
import presentation.support.*
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import opensavvy.spine.server.arrow.routeWithRaise
import opensavvy.spine.server.respond
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import presentation.snippets.s01suspend.slide32.Api
import presentation.snippets.s01suspend.slide32.CreateUser

sealed interface Result
data class Success(val user: User) : Result
data object AlreadyExists : Result

fun insertUser(create: CreateUser): Result = TODO()

//Example
context(_: Transaction, _: Raise<UserAlreadyExists>)
suspend fun insertUser(create: CreateUser): User = TODO()
  
fun Application.users(database: Database) = routing {
  routeWithRaise(Api.Users.create) {
    val user = transaction(database) {
      insertUser(body)
    }
    respond(user)
  }
}
```

</TypeHint>
</TypeHint>
</TypeHint>

---

# Validation

<DrawnAnnotation text="(val value: String)" label="Can create invalid values" color="red" :connect="true" :geometry="{ label: { x: 0.3807, y: 0.3328 } }"/>

```kotlin arrow
@JvmInline
value class Username(val value: String)
```

---
magic-move
---

# Validation

<DrawnAnnotation text="Raise<NonEmptyList<String>>" on="0" label="Accumulate `String` error messages into `NonEmptyList`, at least one error or the success value"  :connect="true" :geometry="{ label: { x: 0.5172, y: 0.5807, width: 0.5206 } }"/>
<DrawnAnnotation text="operator fun invoke" on="1" label="Custom `invoke` operator allows for `Username(...)` syntax"  :connect="true" :geometry="{ label: { x: 0.4276, y: 0.5660 } }"/>

```kotlin arrow
@JvmInline
value class Username private constructor(val value: String) {
  companion object {
    context(_: Raise<NonEmptyList<String>>)
    operator fun invoke(value: String): Username = TODO()
  }
}
```

---
magic-move
---


# Validation

<DrawnAnnotation text="ensure" />
<DrawnAnnotation text="ensure" :occurrence="2" label="Defined on `context(Raise<String>)` so it short-circuits"  :connect="true" :geometry="{ label: { x: 0.3688, y: 0.6412 } }"/>

```kotlin arrow
@JvmInline
value class Username private constructor(val value: String) {
  companion object {
    context(_: Raise<NonEmptyList<String>>)
    operator fun invoke(value: String): Username {
      ensure(value.length >= 4) { ["Username is too short"] }
      ensure(value.length <= 255) { ["Username is too long"] }
      return Username(value)
    }
  }
}
```

---
magic-move
---

# Validation

<TypeHint :line="6" context="Accumulate<String>, Raise<String>">
<DrawnAnnotation text="ensureOrAccumulate" />
<DrawnAnnotation text="ensureOrAccumulate" :occurrence="2" label="Defined on `context(Accumulate<String>)` so it _accumulates_ in `NonEmptyList<Error>`"  :connect="true" :geometry="{ label: { x: 0.5279, y: 0.7138 }, connector: { type: 'quadratic', start: { x: 0.1366, y: 0.5590 }, control: { x: 0.1389, y: 0.6177 }, end: { x: 0.1366, y: 0.6880 } } }"/>

```kotlin arrow
@JvmInline
value class Username private constructor(val value: String) {
  companion object {
    context(_: Raise<NonEmptyList<String>>)
    operator fun invoke(value: String): Username =
      accumulate {
        ensureOrAccumulate(value.length >= 4) { "Username is too short" }
        ensureOrAccumulate(value.length <= 255) { "Username is too long" }
        Username(value)
      }
  }
}
```

</TypeHint>

---

# Validation

<TypeHint :line="6" context="Accumulate<String>, Raise<String>">
<DrawnAnnotation text="ensureOrAccumulate" />
<DrawnAnnotation text="ensureOrAccumulate":occurrence="2" />
<DrawnAnnotation text="ensureOrAccumulate" :occurrence="3" label="Defined on `context(Accumulate<String>)` so it _accumulates_ in `NonEmptyList<Error>`"  :connect="true" :geometry="{ label: { x: 0.5382, y: 0.7478 }, connector: { type: 'quadratic', start: { x: 0.3209, y: 0.6066 }, control: { x: 0.3241, y: 0.6602 }, end: { x: 0.3209, y: 0.7270 } } }"/>

```kotlin arrow
@JvmInline
value class Username private constructor(val value: String) {
  companion object {
    context(_: Raise<NonEmptyList<String>>)
    operator fun invoke(value: String): Username =
      accumulate {
        ensureOrAccumulate(value.length >= 4) { "Username is too short" }
        ensureOrAccumulate(value.length <= 255) { "Username is too long" }
        ensureOrAccumulate(value.matches(Regex("[a-zA-Z0-9]+"))) { "Username is invalid" }
        Username(value)
      }
  }
}
```

</TypeHint>

---

# Validation

<TypeHint :line="6" context="Accumulate<String>, Raise<String>">

```kotlin arrow
@JvmInline
value class Email private constructor(val value: String) {
  companion object {
    context(_: Raise<NonEmptyList<String>>)
    operator fun invoke(value: String): Email =
      accumulate {
        ensureOrAccumulate(value.contains("@")) { "Email is invalid" }
        Email(value)
      }
  }
}

```

</TypeHint>

---

# Validation

```kotlin arrow
data class CreateUser(val username: Username, val email: Email)

context(_: Raise<NonEmptyList<String>>)
fun CreateUserRequest.toCreateUser(): CreateUser = TODO()
```

---
magic-move
---

# Validation

<DrawnAnnotation text="NonEmptyList<NonEmptyList" label="Accumulating `NonEmptyList<String>` into a `NonEmptyList`" :connect="true" :geometry="{ label: { x: 0.6554, y: 0.5450 }, connector: { type: 'quadratic', start: { x: 0.5740, y: 0.4214 }, control: { x: 0.5763, y: 0.4687 }, end: { x: 0.5740, y: 0.5263 } } }"/>

```kotlin{3-10} arrow
data class CreateUser(val username: Username, val email: Email)

context(_: Raise<NonEmptyList<String>>)
fun CreateUserRequest.toCreateUser(): CreateUser =
  withError({ errors: NonEmptyList<NonEmptyList<String>> ->
    it.reduce { acc, errors -> acc + errors }
  }) {
    
  }
```

---
magic-move
---

# Validation

<TypeHint :line="8" context="Accumulate<String>, Raise<String>">
<DrawnAnnotation text="accumulating" />
<DrawnAnnotation text="accumulating" occurrence="2" label="Lift `Raise<NonEmptyList<String>>` into `Accumulate<String>`"  :connect="true" :geometry="{ label: { x: 0.6418, y: 0.7430 } }"/>

```kotlin{8-12} arrow
data class CreateUser(val username: Username, val email: Email)

context(_: Raise<NonEmptyList<String>>)
fun CreateUserRequest.toCreateUser(): CreateUser =
  withError({ errors: NonEmptyList<NonEmptyList<String>> ->
    it.reduce { acc, errors -> acc + errors }
  }) {
    accumulate {
      val username by accumulating { Username(username) }
      val email by accumulating { Email(email) }
      User(username, email)
    }
  }
```

</TypeHint>

---
magic-move
---

# Validation

<TypeHint :line="12" context="Accumulate<String>, Raise<String>">

```kotlin{1,2,6,9} arrow
@JvmInline
value class InvalidUser(val errors: NonEmptyList<String>)

data class CreateUser(val username: Username, val email: Email)

context(_: Raise<InvalidUser>)
fun CreateUserRequest.toCreateUser(): CreateUser =
  withError({ errors: NonEmptyList<NonEmptyList<String>> ->
    InvalidUser(it.reduce { acc, errors -> acc + errors })
  }) {
    accumulate {
      val username by accumulating { Username(username) }
      val email by accumulating { Email(email) }
      User(username, email)
    }
  }
```

</TypeHint>

---

# Call directly in the route 

<DrawnAnnotation text="toCreateUser" :connect="true" :geometry="{ connector: { type: 'quadratic', start: { x: 0.4383, y: 0.3157 }, control: { x: 0.5142, y: 0.3131 }, end: { x: 0.6110, y: 0.2665 } } }"/>
<TypeHint :line="2" context="Raise<InvalidUser>, Raise<UserAlreadyExists>" >

```kotlin
fun Application.users() = routing {
  routeWithRaise(Api.Users.create) {
    val create = body.toCreateUser()
    when(val result = insertUser(create)) {
      is Success -> respond(result.user)
      AlreadyExists -> fail(UserAlreadyExists)
    }
  }
}
```

</TypeHint>

---

# `|.` skips the call on an error

> Design proposal · KEEP-0462

<DrawnAnnotation text="error" label="Explicit error types" />
<DrawnAnnotation text="User | MissingUser" label="Error union, single success type with multiple error types" />
<DrawnAnnotation text="|.name" label="Language level short-circuit syntax like `?.` for nullable types" />
<DrawnAnnotation text="Unit | MissingUser | NotEligible" label="Errors accumulate automatically in the type" />

```kotlin no-compile
error class MissingUser(val id: UserId)

suspend fun findUser(id: UserId): User | MissingUser

suspend fun User.mark(): Unit | NotEligible

suspend fun process(id: UserId): Unit | MissingUser | NotEligible =
  findUser(id)|.mark()
```

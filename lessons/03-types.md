---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Part 3</div>

# The types carry the domain

## From a request to a decision

<!--
We crossed the HTTP boundary. Now consider the values
the service accepts and returns, starting with the User? already in the examples.
-->

---

# A null check refines the type

<DrawnAnnotation text="user == null" />
<DrawnAnnotation text="user" :line="2" occurrence="3" connect="true" label="Smart casted `User?` to `User`" />

```kotlin
fun greeting(user: User?): String =
  if (user == null) "Unknown user" else "Hello, ${user.name}" // [!smartcast occurrence=3 user]
```

---
magic-move
---

# `when` handles absence and guarded values

<DrawnAnnotation text="is User if user.active" />

```kotlin
fun greeting(user: User?): String = when (user) {
  null -> "Unknown user"
  is User if user.active -> "Hello, ${user.name}" // [!smartcast occurrence=2 user]
  is User -> "Your account is inactive"
}
```

---

# A sealed interface names every outcome

<DrawnAnnotation text="sealed interface" />

```kotlin
sealed interface Access {
  data class Granted(val user: User) : Access
  data class Suspended(val reason: String) : Access
  data object Missing : Access
}

fun message(access: Access): String = when (access) {
  is Access.Granted -> "Hello, ${access.user.name}" // [!smartcast access]
  is Access.Suspended -> access.reason // [!smartcast access]
  Access.Missing -> "Unknown user"
}
```

---
magic-move
---

# `-Xcontext-sensitive-resolution` in Kotlin 2.4.20

<DrawnAnnotation text="when (access)" label="Prefixed type is known"  :connect="true" :geometry="{ label: { x: 0.6973, y: 0.5960 } }"/>

```kotlin
sealed interface Access {
  data class Granted(val user: User) : Access
  data class Suspended(val reason: String) : Access
  data object Missing : Access
}

fun message(access: Access): String = when (access) {
  is Granted -> "Hello, ${access.user.name}" // [!smartcast access]
  is Suspended -> access.reason // [!smartcast access]
  Missing -> "Unknown user"
}
```

---
magic-move
---

# A sealed interface names every outcome

<DrawnAnnotation text="if access.user.active" label="if-guard checks condition" :connect="true" :geometry="{ label: { x: 0.6988, y: 0.6260 }, connector: { type: 'quadratic', start: { x: 0.4385, y: 0.5521 }, control: { x: 0.5647, y: 0.5437 }, end: { x: 0.5800, y: 0.6052 } } }"/>

```kotlin
sealed interface Access {
  data class Granted(val user: User) : Access
  data class Suspended(val reason: String) : Access
  data object Missing : Access
}

fun message(access: Access): String = when (access) {
  is Granted if access.user.active -> "Hello, ${access.user.name}" // [!smartcast access]
  is Granted -> "Your account is inactive"
  is Suspended -> access.reason // [!smartcast access]
  Missing -> "Unknown user"
}
```

---

# Value classes distinguish domain values

```kotlin
data class Profile(val name: String, val age: Int, val email: String)
```

---

# Value classes distinguish domain values

<DrawnAnnotation text="@JvmInline" label="Valhalla (JEP 401) will bring value classes to JVM"  :geometry="{ label: { x: 0.3206, y: 0.4711 } }"/>

```kotlin
@JvmInline value class Name(val value: String)
@JvmInline value class Email(val value: String)

data class Profile(val name: Name, val age: Int, val email: Email)
```

---

# Destructuring can follow property names

> Experimental Kotlin 2.4.20 
> 
> `-Xname-based-destructuring=complete`

<DrawnAnnotation text="val (email, name)" />

```kotlin
data class Profile(val name: String, val age: Int, val email: String)

fun main() {
  val profile = Profile("Ada", 36, "ada@example.com")
  val (email, name) = profile
  println("$name <$email>")
}
```
```console
Ada <ada@example.com>
```

---

# Collection literals

```kotlin
val list: List<String> = ["a", "bb", "ccc"]
val set: Set<String> = ["a", "bb", "ccc"]
val iterable: Iterable<String> = ["a", "bb", "ccc"]
```

---

# Custom collection literals

```kotlin
data class NonEmptyList<A>(val head: A, val tail: List<A>)

val literal: NonEmptyList<String> =
  
  // [!error `["a", "bb", "ccc"]` `NonEmptyList<String>` for collection literal does not\ndefine operator `of` and is not a supertype of `List`]
  ["a", "bb", "ccc"]

val nonEmptyList = NonEmptyList("a", ["bb", "ccc"])
```

---

# Custom collection literals

```kotlin
data class NonEmptyList<A> private constructor(val head: A, val tail: List<A>) {
  companion object {
    operator fun <A> of(head: A, vararg tail: A) =
      NonEmptyList(head, tail.toList())
  }
}

val literal = ["a", "bb", "ccc"]

val nonEmptyList =
  // [!error `NonEmptyList("a", ["bb", "ccc"])` `constructor<A>(head: A, tail: List<A>): NonEmptyList<A>` is private]
  NonEmptyList("a", ["bb", "ccc"])

val empty = [] // [!error `[]` No value passed for parameter `head`]
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
  recover({ // [!context Raise<UserAlreadyExists>]
    insertUser(create)
  }) { _ -> null }

context(_: Transaction)
suspend fun insertUserResult(create: CreateUser): Either<UserAlreadyExists, User> =
  either { // [!context Raise<UserAlreadyExists>]
    insertUser(create)
  }
```

---

# Ktor Routes using Spine

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
  
fun Application.users(database: Database) = routing { // [!this Routing]
  routeWithRaise(Api.Users.create) { // [!context context(Raise<AlreadyExists>, Raise<InvalidUser>)]
    val create = body.toCreateUser()
    val user = transaction(database) { // [!this Transaction]
      insertUser(create)
    }
    respond(user)
  }
}
```

---
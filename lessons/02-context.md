---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Part 2</div>

# A function can require context

## Dependencies become capabilities

<!--
ResourceScope was our first example. Now make the
mechanism explicit with a familiar dependency.
-->

---

# `context` requires a dependency

<DrawnAnnotation text="context(logger: Logger)" />
<DrawnAnnotation text="logger.info" />

```kotlin
context(logger: Logger)
fun welcome(user: User): String {
  logger.info("Welcoming ${user.id}")
  return "Hello, ${user.name}"
}

context(_: Logger)
fun welcomeAll(users: List<User>): List<String> =
  users.map { welcome(it) }
```

<!--

Logger is a required, statically resolved argument. The compiler threads it
through welcomeAll into welcome. The underscore means we do not need to name
the capability here. It is not a global service lookup. Multiple requirements
can be listed independently, with no synthetic environment class.
Source: https://kotlinlang.org/docs/context-parameters.html
-->

---
magic-move
---

# A receiver supplies the context

<DrawnAnnotation text="with(logger)" />
<TypeHint text="with(logger) {" receiver="Logger">

```kotlin
context(logger: Logger)
fun welcome(user: User): String {
  logger.info("Welcoming ${user.id}")
  return "Hello, ${user.name}"
}

fun greeting(logger: Logger, user: User): String =
  with(logger) { welcome(user) }
```

</TypeHint>

<!--

with establishes a receiver; that receiver can satisfy the context requirement.
Real or recording logger, the function is the same. This is ordinary lexical
resolution, so missing or ambiguous context is a compile-time error.
-->

---

# The authentication scheme fixes the principal type

> Ktor 3.6.0 · experimental typed authentication

<DrawnAnnotation text="basic<UserIdPrincipal>" />

```kotlin
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.UserPasswordCredential
import io.ktor.server.auth.basic

fun authentication(verify: suspend (UserPasswordCredential) -> Boolean) =
  basic<UserIdPrincipal>("users") {
    realm = "Functional Kotlin Services"
    validate { credentials ->
      if (verify(credentials)) UserIdPrincipal(credentials.name)
      else null
    }
  }
```

<!--

The scheme carries UserIdPrincipal in its type. The verifier is a supplied
suspending boundary: no hardcoded password check. null refuses authentication.
The experimental Ktor API opt-in is configured in the compilation project.
Source: https://ktor.io/docs/server-typed-auth.html
-->

---

# Authenticated routes have a non-null principal

> Ktor 3.6.0 · experimental typed authentication

<DrawnAnnotation text="authenticateWith" />
<DrawnAnnotation text="call.principal" />
<TypeHint text="authenticateWith(auth) {" context="PrincipalContext<UserIdPrincipal>">

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.basic
import io.ktor.server.auth.principal
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

private fun authentication(verify: suspend (String, String) -> Boolean) =
  basic<UserIdPrincipal>("users") {
    validate { if (verify(it.name, it.password)) UserIdPrincipal(it.name) else null }
  }

// Example
fun Application.secureRoutes(verify: suspend (String, String) -> Boolean) {
  val auth = authentication(verify)
  routing {
    authenticateWith(auth) {
      get("/me") {
        val user: UserIdPrincipal = call.principal
        call.respondText("Hello, ${user.name}")
      }
    }
  }
}
```

</TypeHint>

<!--

The block supplies PrincipalContext<UserIdPrincipal>. The request supplies
ApplicationCall. Together they make this property available with a non-null
result. Required authentication rejects the request before the handler runs.
Optional authentication deliberately uses principalOrNull instead.
Source: https://ktor.io/docs/server-typed-auth.html
-->

---
snippets: false
---

# `principal` needs a receiver and a context

> Ktor's property signature · implementation omitted

<DrawnAnnotation text="PrincipalContext<P>" />
<DrawnAnnotation text="ApplicationCall.principal" />

```kotlin no-compile
context(authCtx: PrincipalContext<P>)
val <P : Any> ApplicationCall.principal: P
```

<!--

Read the two independent constraints: the extension receiver is an
ApplicationCall; the context is proof of which principal was established.
The receiver alone cannot conjure the authentication context. This is a real
library signature with its getter omitted, deliberately excluded from compilation.
Source: https://github.com/ktorio/ktor/blob/main/ktor-server/ktor-server-plugins/ktor-server-auth/common/src/io/ktor/server/auth/PrincipalContext.kt
-->

---

# A receiver satisfies a context

<DrawnAnnotation text="fun Logger.receiverCallsContext()" />

```kotlin
context(logger: Logger)
fun record(message: String) = logger.info(message)

fun Logger.receiverCallsContext() {
  record("The receiver supplies Logger")
}
```

<!--

This is why the previous with worked, and why older receiver-based DSLs
interoperate with new context APIs. An implicit receiver is a candidate when the
compiler resolves a context argument.
Source: https://kotlinlang.org/docs/context-parameters.html
-->

---

# A context parameter is not an implicit receiver

<DrawnAnnotation text="logger.receiverCallsContext()" />

```kotlin
context(logger: Logger)
fun record(message: String) = logger.info(message)

fun Logger.receiverCallsContext() = record("Recorded")

context(logger: Logger)
fun contextCallsReceiver() {
  logger.receiverCallsContext()
}
```

<!--

The reverse does not happen: deleting logger-dot from this call fails to find an
implicit receiver. Context parameters are named values, so use the name for
receiver extensions and members. They can satisfy other context parameters.
The distinction is resolution, not a restriction on the underlying value.
-->

---

# Route handlers already suspend

<DrawnAnnotation text="users.find" />
<DrawnAnnotation text="call.respondText" />

```kotlin
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.userRoutes(users: Users) {
  get("/users/{id}") {
    val id = UserId(call.pathParameters["id"]!!.toLong())
    val user = users.find(id)
    call.respondText(user?.name ?: "Unknown user")
  }
}
```

<!--

get takes a suspending handler. No launch or runBlocking is needed to await the
service. Responding is also suspending. Parameter parsing is intentionally
manual here; the next slide removes the unsafe assertion and conversion.
The text response is kept small to focus on composition; error-to-HTTP policy
belongs at this boundary once our typed domain errors arrive.
-->

---
magic-move
---

# The property name becomes the parameter key

<DrawnAnnotation text="val id: Long by call.pathParameters" />

```kotlin
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.util.getValue

fun Route.userRoutes(users: Users) {
  get("/users/{id}") {
    val id: Long by call.pathParameters
    val user = users.find(UserId(id))
    call.respondText(user?.name ?: "Unknown user")
  }
}
```

<!--

by delegates reading this local property. Ktor receives its name, id, and its
expected type, Long. Missing or invalid required values are reported through
Ktor's request-parameter error handling. The getValue operator is an extension
provided by Ktor; this is a language mechanism, not special route syntax.
Source: https://ktor.io/docs/server-requests.html
-->

---

# `by` calls the delegate's `getValue`

<DrawnAnnotation text="property.name" />
<DrawnAnnotation text="by Fields" />

```kotlin
import kotlin.reflect.KProperty

class Fields(private val values: Map<String, String>) {
  operator fun getValue(owner: Any?, property: KProperty<*>): String =
    values.getValue(property.name)
}

fun main() {
  val name by Fields(mapOf("name" to "Ada"))
  println(name)
}
```
```console
Ada
```

<!--

Small custom delegate, same protocol. This example only retrieves a String;
Ktor's delegate adds request-aware conversion and errors. A mutable property
can also delegate writes through setValue. Stop here: the purpose is recognizing
the language feature when it appears in a service API.
Source: https://kotlinlang.org/docs/delegated-properties.html
-->

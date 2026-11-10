---

# `suspend` is Continuation Passing Style

<SuspendGutter :lines="[2]">

```kotlin
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine
data class CreateUser(val name: String)
data class SdkUser(val name: String)
abstract class RequestHandler<T> {
  abstract fun onSuccess(value: T)
  abstract fun onFailure(error: Throwable)
}
fun CreateUser.toSdkRequest(): String = name
object sdk {
  fun execute(request: String, handler: RequestHandler<SdkUser>): Request = TODO()
}

// Example
suspend fun insertUser(create: CreateUser): User =
  suspendCoroutine { cont: Continuation<User> ->
    
  }
```

</SuspendGutter>

---

# `suspend` is Continuation Passing Style

<SuspendGutter :lines="[2]">
<DrawnAnnotation text="cont.resume(value)" on="0" />
<DrawnAnnotation text="cont.resumeWithException(error)" on="1" />

```kotlin
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
data class CreateUser(val name: String)
data class SdkUser(val name: String)
abstract class RequestHandler<T> {
  abstract fun onSuccess(value: T)
  abstract fun onFailure(error: Throwable)
}
fun CreateUser.toSdkRequest(): String = name
object sdk {
  fun execute(request: String, handler: RequestHandler<SdkUser>): Request = TODO()
}

// Example
suspend fun insertUser(create: CreateUser): SdkUser =
  suspendCoroutine { cont ->
    sdk.execute(create.toSdkRequest(), object : RequestHandler<SdkUser>() {
      override fun onSuccess(value: SdkUser) = cont.resume(value)
      override fun onFailure(error: Throwable) = cont.resumeWithException(error)
    })
  }
```

</SuspendGutter>

---

# `suspend` is cancellable

<DrawnAnnotation text="call" />
<DrawnAnnotation text="invokeOnCancellation { call.cancel() }" />

```kotlin
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.*
data class CreateUser(val name: String)
data class SdkUser(val name: String)
abstract class RequestHandler<T> {
  abstract fun onSuccess(value: T)
  abstract fun onFailure(error: Throwable)
}
fun CreateUser.toSdkRequest(): String = name
object sdk {
  fun execute(request: String, handler: RequestHandler<SdkUser>): Request = TODO()
}

// Example
suspend fun insertSdkUser(create: CreateUser): SdkUser =
  suspendCancellableCoroutine { cont ->
    val call = sdk.execute(create.toSdkRequest(), object : RequestHandler<SdkUser>() {
      override fun onSuccess(value: SdkUser) = cont.resume(value)
      override fun onFailure(error: Throwable) = cont.resumeWithException(error)
    })
    cont.invokeOnCancellation { call.cancel() }
  }
```

---

# `suspend` is cancellable

<DrawnAnnotation text="ensureActive()" label="Check cancellation, and stop if case of cancellation" />
<SuspendGutter :lines="[3]">

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

suspend fun ByteArray.checksum() =
  foldIndexed(0L) { index, acc, byte ->
    if (index % 1024 == 0) currentCoroutineContext().ensureActive()
    acc * 31 + byte
  }
```

</SuspendGutter>

---

# `suspend` is cancellable

<DrawnAnnotation text="yield()" label="Fairness operator, reschedule yourself to allow interleaving of tasks" />
<SuspendGutter :lines="[3]">

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

suspend fun ByteArray.checksum() =
  foldIndexed(0L) { index, acc, byte ->
    if (index % 1024 == 0) yield()
    acc * 31 + byte
  }
```

</SuspendGutter>

---

# Process events in parallel

<DrawnAnnotation text="Channel<Event>" label="KotlinX Coroutines suspending Queue" on="0"  :geometry="{ label: { x: 0.5182, y: 0.3454 } }"/>
<DrawnAnnotation text="for (event in events)" label="Suspending iterator" on="1"  :geometry="{ label: { x: 0.3758, y: 0.3484 } }"/>
<SuspendGutter :lines="[3, 4]">

```kotlin
import kotlinx.coroutines.channels.Channel

object sdk { fun events(): Channel<Event> = Channel() }

// Example
suspend fun processor() {
  val events: Channel<Event> = sdk.events()
  for (event in events) {
    handle(event)
  }
}
```

</SuspendGutter>

---

# Process events in parallel

<DrawnAnnotation text="consumeAsFlow" label="Flow is a concurrent stream" on="0"  :geometry="{ label: { x: 0.4948, y: 0.3006 } }"/>
<DrawnAnnotation text="map" label="Supports `suspend`" on="1"  :geometry="{ label: { x: 0.3781, y: 0.3645 }, connector: { type: 'quadratic', start: { x: 0.1471, y: 0.3139 }, control: { x: 0.2469, y: 0.3147 }, end: { x: 0.2928, y: 0.3436 } } }" :connect="true"/>
<DrawnAnnotation text="collect" label="Drain the `Flow`, `suspend` awaits until entire `Flow` is drained" on="2"  :geometry="{ label: { x: 0.4747, y: 0.3914 }, connector: { type: 'quadratic', start: { x: 0.1522, y: 0.3724 }, control: { x: 0.1703, y: 0.3995 }, end: { x: 0.1921, y: 0.3955 } } }" :connect="true"/>
<SuspendGutter :lines="[3,4]">

```kotlin
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import presentation.support.*

object sdk {
  fun events(): Channel<Event> = Channel()
}

// Example
suspend fun processor() {
  sdk.events().consumeAsFlow()
    .map { handle(it) }
    .collect()
}
```

</SuspendGutter>

---

# Process events in parallel

<DrawnAnnotation text="suspend {" />
<DrawnAnnotation text="asFlow()" label="Manually lift `suspend` to `Flow` to access parallelism"  :connect="true" :geometry="{ label: { x: 0.6792, y: 0.4651 } }"/>

```kotlin
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import presentation.support.*

object sdk {
  fun events(): Channel<Event> = Channel()
}

// Example
suspend fun processor() {
  sdk.events().consumeAsFlow()
    .flatMapMerge(concurrency = 64) { suspend { handle(it) }.asFlow() }
    .collect()
}
```

---

# Process events in parallel

<DrawnAnnotation text="parMapUnordered" label="Equivalent of `flatMapMerge`, ordering comes at a cost"  :connect="true" :geometry="{ label: { x: 0.3483, y: 0.4651 } }"/>

```kotlin arrow
import kotlinx.coroutines.flow.*
import arrow.fx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import presentation.support.*

object sdk {
  fun events(): Channel<Event> = Channel()
}

// Example
suspend fun processor() {
  sdk.events().consumeAsFlow()
    .parMapUnordered(concurrency = 64) { handle(it) }
    .collect()
}
```

---

# Process events in parallel

```kotlin arrow
import kotlinx.coroutines.flow.*
import arrow.fx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import presentation.support.*

object sdk {
  fun events(): Channel<Event> = Channel()
}

// Example
suspend fun processor() {
  sdk.events().consumeAsFlow()
    .parMap(concurrency = 64) { handle(it) }
    .collect()
}
```

---

# Process events in parallel

<DrawnAnnotation text="Flow<Unit>" label="Allow further composition" />

```kotlin arrow
import kotlinx.coroutines.flow.*
import arrow.fx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import presentation.support.*

object sdk {
  fun events(): Channel<Event> = Channel()
}

// Example
suspend fun processor(): Flow<Unit> =
  sdk.events().consumeAsFlow()
    .parMap(concurrency = 64) { handle(it) }
```

---

# Process events in parallel

<DrawnAnnotation text="retry" />
<DrawnAnnotation text="buffer" label="Typical streaming operators available, easy to build your own\nArrow Fx Coroutines has additional ones like `parMap`"  :connect="true" :geometry="{ label: { x: 0.5186, y: 0.6001 }, connector: { type: 'quadratic', start: { x: 0.1805, y: 0.5487 }, control: { x: 0.2197, y: 0.5556 }, end: { x: 0.2311, y: 0.5791 } } }"/>

```kotlin arrow
import kotlinx.coroutines.flow.*
import arrow.fx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import presentation.support.*

class RetryiableException : Exception()

object sdk {
  fun events(): Channel<Event> = Channel()
}

// Example
suspend fun processor(): Flow<Unit> =
  sdk.events().consumeAsFlow()
    .parMap(concurrency = 64) { handle(it) }

suspend fun program(): Unit {
  processor()
    .retry(3) { cause -> cause is RetryiableException }
    .buffer(100)
    .collect()
}

```

---

# Process events in parallel

<DrawnAnnotation text="onCompletion" label="We can backpressure SuspendApp process on exit"  :connect="true" :geometry="{ label: { x: 0.4629, y: 0.6593 } }"/>

```kotlin arrow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import arrow.fx.coroutines.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import presentation.support.*

class RetryiableException : Exception()

object sdk {
  fun events(): Channel<Event> = Channel()
}

// Example
suspend fun processor(): Flow<Unit> =
  sdk.events().consumeAsFlow()
    .parMap(concurrency = 64) { handle(it) }

suspend fun program(): Unit {
  processor()
    .retry(3) { cause -> cause is RetryiableException }
    .buffer(100)
    .onCompletion { withContext(NonCancellable) { delay(10.seconds) } }
    .collect()
}

```

---

# `async` starts independent work

<DrawnAnnotation text="coroutineScope" />
<DrawnAnnotation text="async" />
<DrawnAnnotation text="async" occurrence="2" />
<DrawnAnnotation text="await()"/>
<DrawnAnnotation text="await()" occurrence="2" />
<TypeHint :line="1" receiver="Routing">
<TypeHint :line="2" context="context(Raise<AlreadyExists>, Raise<InvalidUser>)">
<TypeHint :line="4" receiver="CoroutineScope">


```kotlin{4-8} ktor
import arrow.core.raise.context.Raise
import io.ktor.server.application.*
import kotlinx.coroutines.*
import presentation.snippets.s01suspend.slide19.UserAlreadyExists
import presentation.support.*
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import opensavvy.spine.server.arrow.routeWithRaise
import opensavvy.spine.server.respond
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction as Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import presentation.snippets.s01suspend.slide19.Api
import presentation.snippets.s01suspend.slide19.CreateUser

sealed interface Result
data class Success(val user: User) : Result
data object AlreadyExists : Result

fun CreateUser.toCreateUser(): CreateUser = this
fun createProfile(create: CreateUser): Unit = TODO()
fun User.withProfile(profile: Unit): User = this

context(_: Transaction, _: Raise<UserAlreadyExists>)
fun insertUser(create: CreateUser): User = TODO()
  
//Example
fun Application.users(database: Database) = routing {
  routeWithRaise(Api.Users.create) {
    val create = body.toCreateUser()
    val userWithProfile = coroutineScope {
        val user = async { transaction(database) { insertUser(create) } }
        val profile = async { transaction(database) { createProfile(create) } }
        user.await().withProfile(profile.await())
    }
    respond(userWithProfile)
  }
}
```

</TypeHint>
</TypeHint>
</TypeHint>

<!--

Both children start before we await. This is concurrent; actual parallel
execution depends on the dispatcher and work. The enclosing coroutineScope waits
for its children. A failing child cancels its sibling, and cancelling the caller
cancels both: structured concurrency makes ownership visible in the block.
Source: https://kotlinlang.org/docs/composing-suspending-functions.html
-->

---

# Typed authentication

<TypeHint :line="1" receiver="Routing">
<TypeHint :line="2" receiver="TypedRespondScope<Unit, List<User>, Never, Parameters.Empty>">

```kotlin
import io.ktor.server.application.*
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import opensavvy.spine.server.fail
import presentation.snippets.s01suspend.slide19.Api
import opensavvy.spine.server.route
import opensavvy.spine.server.respond
import presentation.snippets.s01suspend.slide19.CreateUser
import presentation.snippets.s01suspend.slide19.UserAlreadyExists
import presentation.support.*

sealed interface Result
data class Success(val user: User) : Result
data object AlreadyExists : Result

fun insertUser(createUser: CreateUser): Result = TODO()
fun getAllUsers(): List<User> = emptyList()

//Example
fun Application.users() = routing {
  route(Api.Users.list) {
    val users = getAllUsers()
    respond(users)
  }
}
```

</TypeHint>
</TypeHint>

---

# Typed authentication

<DrawnAnnotation text="JwtUser" :line="1" />
<DrawnAnnotation text="JwtUser" :line="2" />
<DrawnAnnotation text="JwtUser" :line="6" label="The `principal` is typed"  :connect="true" :geometry="{ label: { x: 0.2966, y: 0.5147 } }"/>

```kotlin ktor 
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.algorithms.Algorithm
import opensavvy.spine.server.route
import presentation.snippets.s01suspend.slide19.Api
enum class Role : AuthenticationRole { Admin }
typealias JwtRole = Role
data class JwtUser(val id: Long, val roles: Set<Role> = emptySet())
object env { val issuer = "issuer"; val secret = "secret" }
fun getAllUsers(): List<User> = emptyList()

// Example
val jwt: AuthenticationScheme<JwtUser, Unit> =
    jwt<JwtUser>("ktor-arrow-server") {
        verifier(env.issuer, "ktor-arrow-server", Algorithm.HMAC512(env.secret))
        validate {
            val userId = it.getClaim("user_id", Long::class) ?: return@validate null
            JwtUser(userId)
        }
    }
```

---

# Typed authentication

<TypeHint :line="4" receiver="Routing">
<TypeHint :line="5" context="context(PrincipalContext<JwtUser>, Unit)">
<TypeHint :line="6" receiver="TypedRespondScope<Unit, List<User>, Never, Parameters.Empty>">

```kotlin{1,5,6,10,11|7} ktor 
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.algorithms.Algorithm
import opensavvy.spine.server.route
import presentation.snippets.s01suspend.slide19.Api
import opensavvy.spine.server.respond
import io.ktor.server.application.*
import io.ktor.server.routing.routing
enum class Role : AuthenticationRole { Admin }
typealias JwtRole = Role
data class JwtUser(val id: Long, val roles: Set<Role> = emptySet())
object env { val issuer = "issuer"; val secret = "secret" }
fun getAllUsers(): List<User> = emptyList()

// Example
val jwt: AuthenticationScheme<JwtUser, Unit> =
    jwt<JwtUser>("ktor-arrow-server") { }

fun Application.users() = routing {
  authenticateWith(jwt) {
    route(Api.Users.list) {
      val user = call.principal
      val users = getAllUsers()
      respond(users)
    }
  }
}
```

</TypeHint>
</TypeHint>
</TypeHint>

---
magic-move
---

# Typed authentication

<TypeHint :line="4" receiver="Routing">
<TypeHint :line="5" context="context(PrincipalContext<JwtUser>, Unit)">
<TypeHint :line="6" receiver="TypedRespondScope<Unit, List<User>, Never, Parameters.Empty>">

```kotlin{7,14-16} ktor 
import io.ktor.server.auth.principal as typedPrincipal
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.algorithms.Algorithm
import opensavvy.spine.server.route
import presentation.snippets.s01suspend.slide19.Api
import opensavvy.spine.server.respond
import io.ktor.server.application.*
import io.ktor.server.routing.routing
enum class Role : AuthenticationRole { Admin }
typealias JwtRole = Role
data class JwtUser(val id: Long, val roles: Set<Role> = emptySet())
object env { val issuer = "issuer"; val secret = "secret" }
fun getAllUsers(): List<User> = emptyList()

// Example
val jwt: AuthenticationScheme<JwtUser, Unit> =
    jwt<JwtUser>("ktor-arrow-server") { }

fun Application.users() = routing {
  authenticateWith(jwt) {
    route(Api.Users.list) {
      val user = call.principal
      val users = getAllUsers()
      respond(users)
    }
  }
}

context(ctx: PrincipalContext<P>)
val <P : Any> ApplicationCall.principal: P
  get() = this.typedPrincipal
```

</TypeHint>
</TypeHint>
</TypeHint>

---

# Typed authentication

<TypeHint :line="5" receiver="Routing">
<TypeHint :line="6" context="context(RolesContext<JwtUser, JwtRole>, PrincipalContext<JwtUser>, Unit)">
<TypeHint :line="7" receiver="TypedRespondScope<Unit, List<User>, Never, Parameters.Empty>">
<DrawnAnnotation text="SchemeWithRoles<JwtUser, JwtRole" label="Roles become part of the typed scheme too"  :connect="true" :geometry="{ label: { x: 0.6637, y: 0.3117 }, connector: { type: 'quadratic', start: { x: 0.5634, y: 0.2278 }, control: { x: 0.5649, y: 0.2560 }, end: { x: 0.5634, y: 0.2908 } } }"/>
<DrawnAnnotation text="roles = [Role.Admin]" />
<DrawnAnnotation text="call.roles" />

```kotlin{1-3,6-13} ktor 
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.algorithms.Algorithm
import opensavvy.spine.server.route
import presentation.snippets.s01suspend.slide19.Api
import opensavvy.spine.server.respond
import io.ktor.server.application.*
import io.ktor.server.routing.routing
enum class Role : AuthenticationRole { Admin }
typealias JwtRole = Role
data class JwtUser(val id: Long, val roles: Set<Role> = emptySet())
object env { val issuer = "issuer"; val secret = "secret" }
fun getAllUsers(): List<User> = emptyList()

// Example
val jwt: AuthenticationSchemeWithRoles<JwtUser, JwtRole, Unit, AuthenticationScheme<JwtUser, Unit>> =
    jwt<JwtUser>("ktor-arrow-server") { }
      .withRoles { it.roles }

fun Application.users() = routing {
  authenticateWith(jwt, roles = [Role.Admin]) {
    route(Api.Users.list) {
      val user = call.principal
      val roles = user.roles
      val users = getAllUsers()
      respond(users)
    }
  }
}
```

</TypeHint>
</TypeHint>
</TypeHint>

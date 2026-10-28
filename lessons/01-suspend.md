---
layout: intro
class: section-slide
kodee: wave
---

<!-- @formatter:off -->

<div class="lesson-number">Part 1</div>

# What is suspend?

## Compose work and its lifetime

<!--
Start with the smallest signature.
-->

---
clicks: 4
---

# A service owns its work and its lifetime

<div class="jobtree-centre">
<JobTree
  :root="{ label: 'main = SuspendApp', cancelling: 1, cancelled: 4 }"
  :children="[
    { label: 'server', kind: 'resource', finalising: 2, done: 3 },
    { label: 'processor', cancelling: 2, cancelled: 3 },
  ]"
  :cancel="1"
  cancel-label="SIGTERM"
/>
</div>

<!--
- App runs in a process 
- resources & work form ~> tree
- SIGTERM ~> Graceful stop
- Completed when both are completed
- Model this in FP?
-->

---

# What is `suspend`?

```kotlin no-compile
typealias Thunk<A> = () -> A
typealias IO<A> = suspend () -> A
```

<!--
- Lambdas or Thunk not enough
- IO, or suspend
-> Clearly side-effects
-> Async / Concurrent
-->


---

# What is `suspend`?

<DrawnAnnotation text="Event" :line="2"/>

```kotlin no-compile
suspend fun events(): List<Event>
suspend fun Event.process( ): Unit
```

<DrawnAnnotation text="Event" :line="2" label="Extension function" :geometry="{ label: { x: 0.2395, y: 0.4575 } }" :connect="true"/>

```kotlin no-compile
fun events(): IO<List<Event>>
fun Event.process(): IO<Unit>
```

<!--
- suspend keyword
- IO return type
- Extension function
-->

---
magic-move
---

# What is `suspend`?

<DrawnAnnotation text="Event" :line="2" />

```kotlin no-compile
suspend fun events(): List<Event>
suspend fun Event.process( ): Unit {
  val event: Event = this
}
```

<DrawnAnnotation text="Event" :line="2" label="receiver of extension function" :connect="true" :geometry="{ label: { x: 0.2493, y: 0.5529 }, connector: { type: 'quadratic', start: { x: 0.1342, y: 0.5028 }, control: { x: 0.1334, y: 0.5198 }, end: { x: 0.1342, y: 0.5377 } } }"/>

```kotlin no-compile
fun events(): IO<List<Event>>
fun Event.process(): IO<Unit>
```

<!--
- Receiver becomes this
- Access public API in body
-->

---
magic-move
---

# What is `suspend`?

<SuspendGutter :lines="[5, 6]">

```kotlin{4-7} no-compile
suspend fun events(): List<Event>
suspend fun Event.process(): Unit

suspend fun program() {
  val events = events()
  events.process()
}
```

</SuspendGutter>

```kotlin{4-6} no-compile
fun events(): IO<List<Event>>
fun Event.process(): IO<Unit>

fun program(): IO<Unit> =
  events()
    .flatMap { it.process() }
```

---

# What is `suspend`?

<DrawnAnnotation text="suspend" label="suspend lambda" on="0"  :geometry="{ label: { x: 0.4280, y: 0.2681 } }"/>
<DrawnAnnotation text="SuspendAppScope" label="lambda with receiver, allows calling `exitApp(code)`" at="1"  :geometry="{ label: { x: 0.6493, y: 0.2711 } }"/>

```kotlin arrow no-compile
fun SuspendApp(block: suspend SuspendAppScope.() -> Unit): Unit
```

<br/>

<SuspendGutter :lines="[2]">
<TypeHint :line="1" receiver="SuspendAppScope">

```kotlin arrow
import arrow.continuations.SuspendApp

suspend fun program(): Unit = TODO()

//Example
fun main() = SuspendApp {
  program()
}
```

</TypeHint>
</SuspendGutter>

<v-click at="+2">
  <ul>
     <li>Cancellation on <code>SIGINT</code> & <code>SIGTERM</code></li>
     <li>Await all cancelled <code>Job</code>s to complete</li>
     <li>Structured Concurrency through KotlinX Coroutines</li>
  </ul>
</v-click>

---
class: job-slide
clicks: 6
---

# Server as a Resource

<DrawnAnnotation text="resourceScope" label="Scope for resource management" on="0"  :geometry="{ label: { x: 0.4293, y: 0.3527 } }"/>
<DrawnAnnotation text="server" label="`install` a server into the `resourceScope`" on="1"  :geometry="{ label: { x: 0.3171, y: 0.3490 } }"/>
<DrawnAnnotation text="awaitCancellation" label="Await `SIGTERM`/`SIGINT` which cancels the `SuspendApp`\n-> cancels and awaits the running `suspend` lambda and `CoroutineScope`" on="2"  :geometry="{ label: { x: 0.4509, y: 0.5137 } }"/>
<TypeHint :line="1" receiver="SuspendAppScope">
<TypeHint :line="2" context="ResourceScope">
<SuspendGutter :lines="[2,3,6]">

```kotlin arrow
fun main() = SuspendApp {
  resourceScope {
    server(Netty, host = getenv("HOST"), port = getenv("PORT").toInt()) {
      
    }
    awaitCancellation()
  }
}
```

</SuspendGutter>
</TypeHint>
</TypeHint>

<JobTree
  :root="{ label: 'SuspendApp', cancelling: 2, cancelled: 6 }"
  :children="[
    {
      label: 'resourceScope', kind: 'scope', cancelling: 3, cancelled: 6,
      children: [{ label: 'server(Netty)', kind: 'resource', finalising: 4, done: 5, exit: 'Cancelled' }],
    }
  ]"
  :cancel="2"
  cancel-label="SIGTERM"
/>

---

# Installing as a Resource

<DrawnAnnotation text="ExitCase" label="`Completed`, `Cancelled` or `Failure`"  :geometry="{ label: { x: 0.4792, y: 0.4694 } }"/>

```kotlin arrow no-compile
context(_: ResourceScope)
suspend fun <A> install(
  acquire: suspend () -> A,
  release: suspend (A, ExitCase) -> Unit,
): A
```

---

# Server as a Resource

<SuspendGutter :lines="[7]">

```kotlin arrow no-compile
context(_: ResourceScope)
suspend fun server(
  engine: ApplicationEngine,
  host: String,
  port: Int,
): ServerEngine =
  install({ engine.createAndStart(host, port) }) { server, _ ->
    server.stop()
  }
```

</SuspendGutter>

---
class: job-slide
clicks: 5
---

# Processors share that lifetime

<DrawnAnnotation text="launch" label="Starts a parallel task within the `SuspendAppScope`" on="0" :geometry="{ label: { x: 0.3208, y: 0.4760 }, connector: { type: 'quadratic', start: { x: 0.0918, y: 0.3663 }, control: { x: 0.0844, y: 0.4047 }, end: { x: 0.0931, y: 0.4519 } } }" :connect="true"/>
<DrawnAnnotation text="launch" color="var(--fundamentals-cancel)" on="2"/>
<DrawnAnnotation text="server" color="var(--fundamentals-finalise)" on="3"/>
<TypeHint :line="1" receiver="SuspendAppScope">
<TypeHint :line="2" context="ResourceScope">
<SuspendGutter :lines="[2, 3, 4, 5]">

```kotlin arrow
import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.fx.coroutines.resourceScope
import io.ktor.server.netty.Netty
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import java.lang.System.getenv

val host: String = getenv("HOST")
val port: Int = getenv("PORT").toInt()
suspend fun processor(): Unit = awaitCancellation()

// Example
fun main() = SuspendApp {
  resourceScope {
    server(Netty, host = host, port = port) { }
    launch { processor() }
    awaitCancellation()
  }
}
```

</SuspendGutter>
</TypeHint>
</TypeHint>

<JobTree
  :root="{ label: 'SuspendApp', cancelling: 1, cancelled: 5 }"
  :children="[
    {
      label: 'resourceScope', kind: 'scope', cancelling: 2, cancelled: 4,
      children: [{ label: 'server(Netty)', kind: 'resource', finalising: 3, done: 4, exit: 'Cancelled' }],
    },
    { label: 'launch { processor() }', cancelling: 2, cancelled: 3 },
  ]"
  :cancel="1"
  cancel-label="SIGTERM"
/>

---
class: job-slide job-slide-wide
clicks: 7
---

# Resources are released in reverse order

<DrawnAnnotation text="dependencies()" on="0"/>
<DrawnAnnotation text="server" color="var(--fundamentals-finalise)" on="3"/>
<DrawnAnnotation text="dependencies()" color="var(--fundamentals-finalise)" on="4"/>
<TypeHint :line="1" receiver="SuspendAppScope">
<TypeHint :line="2" context="ResourceScope">
<SuspendGutter :lines="[2,3,4,5,6]">

```kotlin arrow
import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resourceScope
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.lang.System.getenv

val host: String = getenv("HOST")
val port: Int = getenv("PORT").toInt()

class Dependencies {
  suspend fun processor(): Unit = awaitCancellation()
  suspend fun close(): Unit = Unit
}

suspend fun ResourceScope.dependencies(): Dependencies =
  install({ Dependencies() }) { dep, _ -> dep.close() }

suspend fun ResourceScope.coroutineScope(): CoroutineScope =
  install({ CoroutineScope(SupervisorJob() + Dispatchers.Default) }) { scope, _ ->
    scope.coroutineContext.job.cancelAndJoin()
  }

// Example
fun main() = SuspendApp {
  resourceScope {
    val dep = dependencies()
    server(Netty, host = host, port = port) { }
    launch { dep.processor() }
    awaitCancellation()
  }
}
```

</SuspendGutter>
</TypeHint>
</TypeHint>

<JobTree
  :root="{ label: 'SuspendApp', cancelling: 1, cancelled: 6 }"
  :children="[
    {
      label: 'resourceScope', kind: 'scope', cancelling: 2, cancelled: 5,
      children: [
        { label: 'dependencies()', kind: 'resource', order: 1, finalising: 4, done: 5, exit: 'Cancelled' },
        { label: 'server(Netty)', kind: 'resource', order: 2, finalising: 3, done: 4, exit: 'Cancelled' },
      ],
    },
    { label: 'launch { dep.processor() }', cancelling: 2, cancelled: 3 },
  ]"
  :cancel="1"
  cancel-label="SIGTERM"
/>

<v-click at="+7">
  <ul>
    <li color="red">Race condition between <code>dependencies</code> and <code>launch</code></li>
  </ul>
</v-click>

<!--
`dependencies()` is a resource like the server: database pools, clients,
whatever the processor needs. Both are `install`ed in the same `resourceScope`,
in this order, and the scope releases them in reverse: last acquired, first
released. Click 3: the server stops first, no new requests. Click 4: then the
dependencies close. The processor, still a child of `SuspendApp`, is cancelled
on click 2 and gone by click 3: it happens to be done before its dependencies
close, but nothing guarantees it. The next slide makes that a guarantee.
-->

---
class: job-slide job-slide-wide
clicks: 7
---

# Processors finish before dependencies close

<DrawnAnnotation text="coroutineScope" on="0"/>
<DrawnAnnotation text="server" color="var(--fundamentals-finalise)" on="3"/>
<DrawnAnnotation text="scope" color="var(--fundamentals-finalise)" on="4"/>
<DrawnAnnotation text="scope.launch" color="var(--fundamentals-finalise)" on="4"/>
<DrawnAnnotation text="dependencies()" color="var(--fundamentals-finalise)" on="5"/>
<TypeHint :line="1" receiver="SuspendAppScope">
<TypeHint :line="2" context="ResourceScope">
<SuspendGutter :lines="[2,3,4,5,6,7]">

```kotlin arrow
import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resourceScope
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.lang.System.getenv

val host: String = getenv("HOST")
val port: Int = getenv("PORT").toInt()

class Dependencies {
  suspend fun processor(): Unit = awaitCancellation()
  suspend fun close(): Unit = Unit
}

suspend fun ResourceScope.dependencies(): Dependencies =
  install({ Dependencies() }) { dep, _ -> dep.close() }

suspend fun ResourceScope.coroutineScope(): CoroutineScope =
  install({ CoroutineScope(SupervisorJob() + Dispatchers.Default) }) { scope, _ ->
    scope.coroutineContext.job.cancelAndJoin()
  }

// Example
fun main() = SuspendApp {
  resourceScope {
    val dep = dependencies()
    val scope = coroutineScope()
    server(Netty, host = host, port = port) { }
    scope.launch { dep.processor() }
    awaitCancellation()
  }
}
```

</SuspendGutter>
</TypeHint>
</TypeHint>

<JobTree
  :root="{ label: 'SuspendApp', cancelling: 1, cancelled: 7 }"
  :children="[
    {
      label: 'resourceScope', kind: 'scope', cancelling: 2, cancelled: 6,
      children: [
        { label: 'dependencies()', kind: 'resource', order: 1, finalising: 5, done: 6, exit: 'Cancelled' },
        {
          label: 'coroutineScope()', kind: 'resource', order: 2, finalising: 4, done: 5, exit: 'Cancelled',
          children: [{ label: 'launch { dep.processor() }', cancelling: 4, cancelled: 5 }],
        },
        { label: 'server(Netty)', kind: 'resource', order: 3, finalising: 3, done: 4, exit: 'Cancelled' },
      ],
    },
  ]"
  :cancel="1"
  cancel-label="SIGTERM"
/>

<!--
Make the `CoroutineScope` itself a resource: acquired after the dependencies,
before the server; its release cancels its children and waits for them. Now the
processor is owned by the `resourceScope`, not by `SuspendApp`, and the reverse
release order is the shutdown plan. Click 3: the server stops, no new requests.
Click 4: the scope's release cancels the processor and waits for it to finish
the event it is on. Click 5: only then do the dependencies close. Nothing is
closed under a running processor, by construction.
-->

---
magic-move
---

# Configuring your server

<DrawnAnnotation text="@Serializable" label="Compiler plugin to generate serialization code" :connect="true" :geometry="{ label: { x: 0.4434, y: 0.1922 }, connector: { type: 'quadratic', start: { x: 0.2030, y: 0.2196 }, control: { x: 0.2173, y: 0.2196 }, end: { x: 0.2294, y: 0.1980 } } }"/>
<DrawnAnnotation text="getAs<Config>" label="Read, and deserialize the configuration" :connect="true" :geometry="{ label: { x: 0.7430, y: 0.4796 } }"/>
<TypeHint :line="4" receiver="SuspendAppScope">
<TypeHint :line="6" context="ResourceScope">
<TypeHint :line="9" receiver="Application">
<SuspendGutter :lines="[7,8,9,12,13]">

```kotlin{1,2,5,9,10,11} arrow
import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.fx.coroutines.resourceScope
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.getAs
import io.ktor.server.netty.Netty
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.serialization.Serializable

fun processor(): Flow<Unit> = emptyFlow()

// Example
@Serializable
data class Config(val host: String, val port: Int)

fun main() = SuspendApp {
  val config = ApplicationConfig("application.yaml").getAs<Config>()
  resourceScope {
    val dep = dependencies()
    val scope = coroutineScope()
    server(Netty, host = config.host, port = config.port) {

    }
    scope.launch { dep.processor() }
    awaitCancellation()
  }
}

```

</SuspendGutter>
</TypeHint>
</TypeHint>
</TypeHint>

---

# Configuring your Ktor server

<TypeHint :line="2" receiver="ContentNegotiationConfig">
<TypeHint :line="3" receiver="JsonBuilder">

```kotlin ktor
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

fun Application.app() {
  install(ContentNegotiation) {
    json(Json {
      isLenient = true
      ignoreUnknownKeys = true
    })
  }
}
```

</TypeHint>
</TypeHint>

---

# Configuring your Ktor server

<TypeHint :line="2" receiver="ContentNegotiationConfig">
<TypeHint :line="3" receiver="JsonBuilder">

```kotlin ktor
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.ContentConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CustomConverter : ContentConverter {
  override suspend fun serialize(
    contentType: ContentType,
    charset: Charset,
    typeInfo: TypeInfo,
    value: Any?
  ): OutgoingContent? = TODO("Not yet implemented")

  override suspend fun deserialize(
    charset: Charset,
    typeInfo: TypeInfo,
    content: ByteReadChannel
  ): Any? = TODO("Not yet implemented")
}

//Example
fun Application.app() {
  install(ContentNegotiation) {
    json(Json {
      isLenient = true
      ignoreUnknownKeys = true
    })
    register(ContentType("application", "custom"), CustomConverter())
  }
}
```

</TypeHint>
</TypeHint>

---

# Ktor Routes using Spine

```kotlin ktor
import io.ktor.http.HttpStatusCode
import opensavvy.spine.api.RootResource
import opensavvy.spine.api.StaticResource
import presentation.support.*

data class CreateUser(val name: String)
object UserAlreadyExists
object InvalidUser {
  val errors: List<String> = ["username too short", "email doesn't contain @"]
}

//Example
object Api : RootResource("api") {                   // api
  object Users : StaticResource<Api>("users", Api) { // api/users
    val list by get()                                // GET /v3/users
      .response<List<User>>()
  }
}
```

---
magic-move
---

# Ktor Routes using Spine

```kotlin ktor
import io.ktor.http.HttpStatusCode
import opensavvy.spine.api.RootResource
import opensavvy.spine.api.StaticResource
import presentation.support.*

data class CreateUser(val name: String)
object UserAlreadyExists
object InvalidUser {
  val errors: List<String> = ["username too short", "email doesn't contain @"]
}

//Example
object Api : RootResource("api") {                   // api
  object Users : StaticResource<Api>("users", Api) { // api/users
    val list by get()                                // GET /v3/users
      .response<List<User>>()
    
    val create by post()                             // POST /api/users
      .request<CreateUser>()
      .response<User>() 
      .failure<UserAlreadyExists>(HttpStatusCode.Conflict)
      .failure<InvalidUser>(HttpStatusCode.UnprocessableEntity)
  }
}
```

---

# Calling Ktor Spine routes

<SuspendGutter :lines="[5,6]">
<DrawnAnnotation text="Apache5" on="0" label="Support for many different clients across all KMP platforms" :connect="true" :geometry="{ label: { x: 0.5625, y: 0.3105, width: 0.4243 }, connector: { type: 'quadratic', start: { x: 0.2810, y: 0.2676 }, control: { x: 0.3999, y: 0.2619 }, end: { x: 0.4342, y: 0.3025 } } }"/>
<DrawnAnnotation text="UserAlreadyExists" on="1" />
<DrawnAnnotation text="InvalidUser" on="1" />
<DrawnAnnotation text="User" occurrence="6" label="Handle all cases as defined by response and failures" on="1" :connect="true" :geometry="{ label: { x: 0.4133, y: 0.6818 } }"/>
<TypeHint :line="2" receiver="HttpClientConfig<Apache5Config>">

```kotlin ktor
import io.ktor.client.HttpClient
import opensavvy.spine.api.div
import opensavvy.spine.client.handle
import opensavvy.spine.client.request
import presentation.snippets.s01suspend.slide32.Api
import presentation.snippets.s01suspend.slide32.Api.Users
import presentation.snippets.s01suspend.slide32.Api.Users.create
import presentation.snippets.s01suspend.slide32.CreateUser
import presentation.snippets.s01suspend.slide32.InvalidUser
import presentation.snippets.s01suspend.slide32.UserAlreadyExists
import presentation.support.*

// Example
suspend fun main() {
  HttpClient(Apache5) {
    // Configure Apache5 Client
  }.use { client ->
    client.request(Api / Users / create, CreateUser("johndoe"))
      .handle(
        { _: UserAlreadyExists -> println("User already exists") },
        { errors: InvalidUser -> println(errors.joinToString { it.message }) },
        { _: User -> println("User created") }
      )
  }
}

```

</TypeHint>
</SuspendGutter>

---

# Ktor Routes using Spine

<TypeHint :line="1" receiver="Routing">
<TypeHint :line="2" receiver="TypedRespondScope<CreateUser, User, Or<Or<Never, ByCode<UserAlreadyExists>>, ByCode<InvalidUser>>, Parameters.Empty>">
<DrawnAnnotation text="body" :connect="true" on="0"  :geometry="{ connector: { type: 'quadratic', start: { x: 0.4481, y: 0.3153 }, control: { x: 0.5529, y: 0.3218 }, end: { x: 0.6209, y: 0.2636 } } }"/>
<DrawnAnnotation text="respond(result.user)" label="" :connect="true" on="1"  :geometry="{ connector: { type: 'quadratic', start: { x: 0.5036, y: 0.3647 }, control: { x: 0.6418, y: 0.3689 }, end: { x: 0.7178, y: 0.2754 } } }"/>
<DrawnAnnotation text="fail(UserAlreadyExists)" label=""  on="2" :connect="true" :geometry="{ connector: { type: 'quadratic', start: { x: 0.5736, y: 0.4102 }, control: { x: 0.7606, y: 0.4337 }, end: { x: 0.8020, y: 0.2793 } } }"/>

```kotlin ktor
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import opensavvy.spine.server.fail
import presentation.snippets.s01suspend.slide28.Api
import opensavvy.spine.server.route
import opensavvy.spine.server.respond
import presentation.snippets.s01suspend.slide28.CreateUser
import presentation.snippets.s01suspend.slide28.UserAlreadyExists
import presentation.support.*

sealed interface Result
data class Success(val user: User) : Result
data object AlreadyExists : Result

fun insertUser(createUser: CreateUser): Result = TODO()

//Example
fun Application.users() = routing {
  route(Api.Users.create) {
    when(val result = insertUser(body)) {
      is Success -> respond(result.user)
      AlreadyExists -> fail(UserAlreadyExists)
    }
  }
}
```

</TypeHint>
</TypeHint>

---

# Typed authentication

<TypeHint :line="1" receiver="Routing">
<TypeHint :line="2" receiver="TypedRespondScope<Unit, List<User>, Never, Parameters.Empty>">

```kotlin
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import opensavvy.spine.server.fail
import presentation.snippets.s01suspend.slide28.Api
import opensavvy.spine.server.route
import opensavvy.spine.server.respond
import presentation.snippets.s01suspend.slide28.CreateUser
import presentation.snippets.s01suspend.slide28.UserAlreadyExists
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
val jwt: AuthenticationScheme<JwtUser, Unit> =
    jwt<JwtUser>("ktor-arrow-server") {
        verifier(env.issuer, "ktor-arrow-server", Algorithm.HMAC512(env.secret))
        validate {
            val userId = it.getClaim("user_id", Long::class)
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
val jwt: AuthenticationScheme<JwtUser, Unit> =
    jwt<JwtContext>("ktor-arrow-server") { }

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
val jwt: AuthenticationScheme<JwtUser, Unit> =
    jwt<JwtContext>("ktor-arrow-server") { }

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
  get() = ctx.principal(this)
```

</TypeHint>
</TypeHint>
</TypeHint>


---

# `async` starts independent work

<DrawnAnnotation text="coroutineScope" />
<DrawnAnnotation text="async" />
<TypeHint text="coroutineScope {" receiver="CoroutineScope">

```kotlin
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun greetings(users: Users, first: UserId, second: UserId) =
  coroutineScope {
    val ada = async { users.find(first) }
    val grace = async { users.find(second) }
    listOf(ada.await()?.name, grace.await()?.name)
  }
```

</TypeHint>

<!--

Both children start before we await. This is concurrent; actual parallel
execution depends on the dispatcher and work. The enclosing coroutineScope waits
for its children. A failing child cancels its sibling, and cancelling the caller
cancels both: structured concurrency makes ownership visible in the block.
Source: https://kotlinlang.org/docs/composing-suspending-functions.html
-->

---

# `launch` gives a child a lifetime

<DrawnAnnotation text="launch" />
<DrawnAnnotation text="coroutineScope" />

```kotlin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun processBatch(events: List<Event>) = coroutineScope {
  for (event in events) {
    launch { handle(event) }
  }
}
```

<!--

launch gives a Job rather than a result. This function still waits for the whole
batch because the children belong to this scope. Keep this a small batch;
unbounded inputs need bounded concurrency and backpressure.
-->

---

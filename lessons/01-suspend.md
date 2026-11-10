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

```kotlin{4-7} no-compile
suspend fun events(): List<Event>
suspend fun Event.process(): Unit

suspend fun program() {
  val events = events() // [!suspend]
  events.process() // [!suspend]
}
```

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

```kotlin arrow
import arrow.continuations.SuspendApp

suspend fun program(): Unit = TODO()

//Example
fun main() = SuspendApp { // [!this SuspendAppScope]
  program() // [!suspend]
}
```

<v-click at="+2">
  <ul>
     <li>Cancellation on <code>SIGINT</code> & <code>SIGTERM</code></li>
     <li>Await all cancelled coroutines to complete</li>
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
<DrawnAnnotation text="awaitCancellation" label="Await `SIGTERM`/`SIGINT` which cancels the `SuspendApp`\n-> cancels and awaits the running `suspend` lambda and `CoroutineScope`" on="2"  :geometry="{ label: { x: 0.4765, y: 0.6209 } }" :connect="true"/>

```kotlin arrow
import io.ktor.server.netty.Netty
import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.fx.coroutines.resourceScope
import java.lang.System.getenv
import kotlinx.coroutines.*
fun main() = SuspendApp { // [!this SuspendAppScope]
  resourceScope { // [!context ResourceScope] [!suspend]
    server(Netty, host = getenv("HOST"), port = getenv("PORT").toInt()) { // [!suspend]
      
    }
    awaitCancellation() // [!suspend]
  }
}
```

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

```kotlin arrow no-compile
context(_: ResourceScope)
suspend fun server(
  engine: ApplicationEngine,
  host: String,
  port: Int,
): ServerEngine =
  install({ engine.createAndStart(host, port) }) { server, _ -> // [!suspend]
    server.stop()
  }
```

---
class: job-slide
clicks: 5
---

# Processors share that lifetime

<DrawnAnnotation text="launch" label="Starts a parallel task within the `SuspendAppScope`" on="0" :geometry="{ label: { x: 0.3208, y: 0.4760 }, connector: { type: 'quadratic', start: { x: 0.0918, y: 0.3663 }, control: { x: 0.0844, y: 0.4047 }, end: { x: 0.0931, y: 0.4519 } } }" :connect="true"/>
<DrawnAnnotation text="launch" color="var(--fundamentals-cancel)" on="2"/>
<DrawnAnnotation text="server" color="var(--fundamentals-finalise)" on="3"/>

```kotlin arrow
import kotlinx.coroutines.*
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
fun main() = SuspendApp { // [!this SuspendAppScope]
  resourceScope { // [!context ResourceScope] [!suspend]
    server(Netty, host = host, port = port) { } // [!suspend]
    launch { processor() } // [!suspend]
    awaitCancellation() // [!suspend]
  }
}
```

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
clicks: 8
---

# Resources are released in reverse order

<DrawnAnnotation text="dependencies()" on="0"/>
<DrawnAnnotation text="server" color="var(--fundamentals-finalise)" on="3"/>
<DrawnAnnotation text="dependencies()" color="var(--fundamentals-finalise)" on="4"/>

```kotlin arrow
import kotlinx.coroutines.*
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
fun main() = SuspendApp { // [!this SuspendAppScope]
  resourceScope { // [!context ResourceScope] [!suspend]
    val dep = dependencies() // [!suspend]
    server(Netty, host = host, port = port) { } // [!suspend]
    launch { dep.processor() } // [!suspend]
    awaitCancellation() // [!suspend]
  }
}
```

<JobTree
  :root="{ label: 'SuspendApp', cancelling: 1, cancelled: 7 }"
  :children="[
    {
      label: 'resourceScope', kind: 'scope', cancelling: 2, cancelled: 5,
      children: [
        { label: 'dependencies()', kind: 'resource', order: 1, finalising: 4, done: 5, exit: 'Cancelled' },
        { label: 'server(Netty)', kind: 'resource', order: 2, finalising: 3, done: 4, exit: 'Cancelled' },
      ],
    },
    { label: 'launch { dep.processor() }', cancelling: 2, cancelled: 6 },
  ]"
  :cancel="1"
  cancel-label="SIGTERM"
/>

<v-click at="+5">
  <ul>
    <li color="#FF0000">Race condition between <code>dependencies</code> and <code>launch</code></li>
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

```kotlin arrow
import kotlinx.coroutines.*
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
fun main() = SuspendApp { // [!this SuspendAppScope]
  resourceScope { // [!context ResourceScope] [!suspend]
    val dep = dependencies() // [!suspend]
    val scope = coroutineScope() // [!suspend]
    server(Netty, host = host, port = port) { } // [!suspend]
    scope.launch { dep.processor() } // [!suspend]
    awaitCancellation() // [!suspend]
  }
}
```

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

```kotlin{1,2,5,9,10,11} arrow
import arrow.fx.coroutines.ResourceScope
import presentation.snippets.s01suspend.slide13.dependencies
import presentation.snippets.s01suspend.slide13.coroutineScope
import io.ktor.server.application.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

fun main() = SuspendApp { // [!this SuspendAppScope]
  val config = ApplicationConfig("application.yaml").getAs<Config>()
  resourceScope { // [!context ResourceScope]
    val dep = dependencies() // [!suspend]
    val scope = coroutineScope() // [!suspend]
    server(Netty, host = config.host, port = config.port) { // [!this Application] [!suspend]

    }
    scope.launch { dep.processor() } // [!suspend]
    awaitCancellation() // [!suspend]
  }
}

```

---

# Configuring your Ktor server

```kotlin ktor
import io.ktor.server.application.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

fun Application.app() {
  install(ContentNegotiation) { // [!this ContentNegotiationConfig]
    json(Json { // [!this JsonBuilder]
      isLenient = true
      ignoreUnknownKeys = true
    })
  }
}
```

---

# Configuring your Ktor server

```kotlin ktor
import io.ktor.server.application.*
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
  install(ContentNegotiation) { // [!this ContentNegotiationConfig]
    json(Json { // [!this JsonBuilder]
      isLenient = true
      ignoreUnknownKeys = true
    })
    register(ContentType("application", "custom"), CustomConverter())
  }
}
```

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

<DrawnAnnotation text="Apache5" on="0" label="Support for many different clients across all KMP platforms" :connect="true" :geometry="{ label: { x: 0.5625, y: 0.3105, width: 0.4243 }, connector: { type: 'quadratic', start: { x: 0.2810, y: 0.2676 }, control: { x: 0.3999, y: 0.2619 }, end: { x: 0.4342, y: 0.3025 } } }"/>
<DrawnAnnotation text="UserAlreadyExists" on="1" />
<DrawnAnnotation text="InvalidUser" on="1" />
<DrawnAnnotation text="User" occurrence="6" label="Handle all cases as defined by response and failures" on="1" :connect="true" :geometry="{ label: { x: 0.4133, y: 0.6818 } }"/>

```kotlin ktor
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.HttpClient
import opensavvy.spine.api.div
import opensavvy.spine.client.handle
import opensavvy.spine.client.request
import presentation.snippets.s01suspend.slide19.Api
import presentation.snippets.s01suspend.slide19.Api.Users
import presentation.snippets.s01suspend.slide19.Api.Users.create
import presentation.snippets.s01suspend.slide19.CreateUser
import presentation.snippets.s01suspend.slide19.InvalidUser
import presentation.snippets.s01suspend.slide19.UserAlreadyExists
import presentation.support.*

// Example
suspend fun main() {
  HttpClient(Apache5) { // [!this HttpClientConfig<Apache5Config>]
    // Configure Apache5 Client
  }.use { client ->
    client.request(Api / Users / create, CreateUser("johndoe")) // [!suspend]
      .handle( // [!suspend]
        { _: UserAlreadyExists -> println("User already exists") },
        { errors: InvalidUser -> println(errors.errors.joinToString()) },
        { _: User -> println("User created") }
      )
  }
}

```

---

# Ktor Routes using Spine

<DrawnAnnotation text="body" :connect="true" on="0"  :geometry="{ connector: { type: 'quadratic', start: { x: 0.4481, y: 0.3153 }, control: { x: 0.5529, y: 0.3218 }, end: { x: 0.6209, y: 0.2636 } } }"/>
<DrawnAnnotation text="respond(result.user)" label="" :connect="true" on="1"  :geometry="{ connector: { type: 'quadratic', start: { x: 0.5036, y: 0.3647 }, control: { x: 0.6418, y: 0.3689 }, end: { x: 0.7178, y: 0.2754 } } }"/>
<DrawnAnnotation text="fail(UserAlreadyExists)" label=""  on="2" :connect="true" :geometry="{ connector: { type: 'quadratic', start: { x: 0.5736, y: 0.4102 }, control: { x: 0.7606, y: 0.4337 }, end: { x: 0.8020, y: 0.2793 } } }"/>

```kotlin ktor
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

sealed interface Result {
  data class Success(val user: User) : Result
  data object AlreadyExists : Result
}

fun insertUser(createUser: CreateUser): Result = TODO()

//Example
fun Application.users() = routing { // [!this Routing]
  // [!this TypedRespondScope<CreateUser, User, Or<Or<Never, ByCode<UserAlreadyExists>>, ByCode<InvalidUser>>, Parameters.Empty>]
  route(Api.Users.create) {
    when(val result = insertUser(body)) {
      is Success -> respond(result.user) // [!smartcast result]
      AlreadyExists -> fail(UserAlreadyExists)
    }
  }
}
```

---
magic-move
---

# When statement

<DrawnAnnotation text="sealed interface" on="0" label="Closed inheritance within this compilation unit (module)" :connect="true" :geometry="{ label: { x: 0.6300, y: 0.1132 }, connector: { type: 'quadratic', start: { x: 0.2358, y: 0.2215 }, control: { x: 0.3537, y: 0.2305 }, end: { x: 0.3846, y: 0.1438 } } }"/>
<DrawnAnnotation text="@JvmInline" on="1" label="Valhalla (JEP 401) will bring value classes to JVM,\n`@JvmInline` emulates it"  :connect="true" :geometry="{ label: { x: 0.5915, y: 0.1165, width: 0.5813 }, connector: { type: 'quadratic', start: { x: 0.1640, y: 0.2331 }, control: { x: 0.3511, y: 0.2287 }, end: { x: 0.4192, y: 0.1230 } } }"/>
<DrawnAnnotation text="data object" on="2" label="Singleton object"  :connect="true" :geometry="{ label: { x: 0.2833, y: 0.3441 }, connector: { type: 'quadratic', start: { x: 0.1807, y: 0.3152 }, control: { x: 0.1831, y: 0.3383 }, end: { x: 0.1991, y: 0.3441 } } }"/>

```kotlin ktor
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

//Example
sealed interface Result { 
  @JvmInline value class Success(val user: User) : Result
  data object AlreadyExists : Result
}

suspend fun insertUser(createUser: CreateUser): Result = TODO()

fun Application.users() = routing { // [!this Routing]
  // [!this TypedRespondScope<CreateUser, User, Or<Or<Never, ByCode<UserAlreadyExists>>, ByCode<InvalidUser>>, Parameters.Empty>]
  route(Api.Users.create) {
    when(val result = insertUser(body)) {
      is Success -> respond(result.user) // [!smartcast result]
      AlreadyExists -> fail(UserAlreadyExists)
    }
  }
}
```

---

# `Raise` adds an expected error to the signature

<DrawnAnnotation text="Raise<UserAlreadyExists>" label="Can `Raise` a `UserAlreadyExists` error" :connect="true" :geometry="{ label: { x: 0.6504, y: 0.2589 }, connector: { type: 'quadratic', start: { x: 0.4494, y: 0.3155 }, control: { x: 0.4660, y: 0.3086 }, end: { x: 0.4750, y: 0.2799 } } }"/>

```kotlin
import arrow.core.raise.context.Raise
import opensavvy.spine.server.respond
import io.ktor.server.application.*
import io.ktor.server.routing.routing
data object UserAlreadyExists

context(_: Raise<UserAlreadyExists>)
suspend fun insertUser(createUser: CreateUser): User = TODO()

fun Application.users() = routing {
  // [!this TypedRespondScope<CreateUser, User, Or<Or<Never, ByCode<UserAlreadyExists>>, ByCode<InvalidUser>>, Parameters.Empty>]
  route(Api.Users.create) {
    // [!error style="--inline-compiler-error-message-size: 1.55rem" insertUser No context argument for `_: Raise<UserAlreadyExists>` found]
    val user = insertUser(body)
    respond(user)
  }
}
```

---

# `Raise` adds an expected error to the signature

<DrawnAnnotation text="routeWithRaise" label="Spine's Arrow-kt support brings `Raise` into the context for all failures" on="0"/>
<DrawnAnnotation text="insertUser" :line="6" on="1" :connect="true"  :geometry="{ connector: { type: 'quadratic', start: { x: 0.2855, y: 0.4646 }, control: { x: 0.4678, y: 0.5416 }, end: { x: 0.8007, y: 0.4063 } } }"/>
<DrawnAnnotation text="body" :line="6" on="2" :connect="true" label="Missing validation" color="red" :geometry="{ label: { x: 0.4971, y: 0.5251 }, connector: { type: 'quadratic', start: { x: 0.3651, y: 0.4686 }, control: { x: 0.4678, y: 0.5416 }, end: { x: 0.6462, y: 0.4057 } } }"/>

```kotlin
import arrow.core.raise.context.Raise
import opensavvy.spine.server.arrow.routeWithRaise
import opensavvy.spine.server.respond
import io.ktor.server.application.*
import io.ktor.server.routing.routing
import presentation.snippets.s01suspend.slide19.Api
import presentation.snippets.s01suspend.slide19.CreateUser
import presentation.snippets.s01suspend.slide19.UserAlreadyExists
context(_: Raise<UserAlreadyExists>)
suspend fun insertUser(createUser: CreateUser): User = TODO()

fun Application.users() = routing {
  routeWithRaise(Api.Users.create) { // [!context Raise<InvalidUser>, Raise<UserAlreadyExists>]
    val user = insertUser(body)
    respond(user)
  }
}
```

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

<DrawnAnnotation text="private constructor" on="0" label="Enforce using smart-constructor"  :connect="true" :geometry="{ label: { x: 0.6915, y: 0.3019 }, connector: { type: 'quadratic', start: { x: 0.5045, y: 0.2768 }, control: { x: 0.5157, y: 0.3006 }, end: { x: 0.5331, y: 0.3015 } } }"/>
<DrawnAnnotation text="Raise<NonEmptyList<String>>" on="1" label="Accumulate `String` error messages into `NonEmptyList`, at least one error or the success value"  :connect="true" :geometry="{ label: { x: 0.5172, y: 0.5807, width: 0.5206 } }"/>
<DrawnAnnotation text="operator fun invoke" on="2" label="Custom `invoke` operator allows for `Username(...)` syntax"  :connect="true" :geometry="{ label: { x: 0.4276, y: 0.5660 } }"/>

```kotlin arrow
import arrow.core.raise.context.Raise
import arrow.core.nonEmptyListOf
import arrow.core.NonEmptyList
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
import arrow.core.raise.context.Raise
import arrow.core.nonEmptyListOf
import arrow.core.NonEmptyList
import arrow.core.raise.context.ensure
@JvmInline
value class Username private constructor(val value: String) {
  companion object {
    context(_: Raise<NonEmptyList<String>>)
    operator fun invoke(value: String): Username {
      ensure(value.length >= 4) { nonEmptyListOf("Username is too short") }
      ensure(value.length <= 255) { nonEmptyListOf("Username is too long") }
      return Username(value)
    }
  }
}
```

---

# Validation

<DrawnAnnotation text="ensureOrAccumulate" />
<DrawnAnnotation text="ensureOrAccumulate":occurrence="2" />
<DrawnAnnotation text="ensureOrAccumulate" :occurrence="3" label="Defined on `context(Accumulate<String>)` so it _accumulates_ in `NonEmptyList<String>`"  :connect="true" :geometry="{ label: { x: 0.5382, y: 0.7478 }, connector: { type: 'quadratic', start: { x: 0.3209, y: 0.6066 }, control: { x: 0.3241, y: 0.6602 }, end: { x: 0.3209, y: 0.7270 } } }"/>

```kotlin arrow
import arrow.core.raise.context.Raise
import arrow.core.nonEmptyListOf
import arrow.core.NonEmptyList
import presentation.support.ensureOrAccumulate
import arrow.core.raise.context.accumulate
@JvmInline
value class Username private constructor(val value: String) {
  companion object {
    context(_: Raise<NonEmptyList<String>>)
    operator fun invoke(value: String): Username =
      accumulate { // [!context Accumulate<String>, Raise<String>]
        ensureOrAccumulate(value.length >= 4) { "Username is too short" }
        ensureOrAccumulate(value.length <= 255) { "Username is too long" }
        ensureOrAccumulate(value.matches(Regex("[a-zA-Z0-9]+"))) { "Username is invalid" }
        Username(value)
      }
  }
}
```

---

# Validation

```kotlin arrow
import arrow.core.raise.context.Raise
import arrow.core.nonEmptyListOf
import arrow.core.NonEmptyList
import presentation.support.ensureOrAccumulate
import arrow.core.raise.context.accumulate
@JvmInline
value class Email private constructor(val value: String) {
  companion object {
    context(_: Raise<NonEmptyList<String>>)
    operator fun invoke(value: String): Email =
      accumulate { // [!context Accumulate<String>, Raise<String>]
        ensureOrAccumulate(value.contains("@")) { "Email is invalid" }
        Email(value)
      }
  }
}

```

---

# Validation

```kotlin arrow
import arrow.core.raise.context.Raise
import arrow.core.nonEmptyListOf
import arrow.core.NonEmptyList
import presentation.snippets.s01suspend.slide28.Username
import presentation.snippets.s01suspend.slide29.Email

data class CreateUserRequest(val username: String, val email: String)

// Example
data class CreateUser(val username: Username, val email: Email)

context(_: Raise<NonEmptyList<String>>)
fun CreateUserRequest.toCreateUser(): CreateUser = TODO()
```

---
magic-move
---

# Validation

<DrawnAnnotation text="NonEmptyList<NonEmptyList" label="Accumulating `NonEmptyList<String>` into a `NonEmptyList`" :connect="true" :geometry="{ label: { x: 0.6554, y: 0.5450 }, connector: { type: 'quadratic', start: { x: 0.5740, y: 0.4214 }, control: { x: 0.5763, y: 0.4687 }, end: { x: 0.5740, y: 0.5263 } } }"/>

```kotlin{3-10} arrow no-compile
import arrow.core.raise.context.Raise
import arrow.core.nonEmptyListOf
import arrow.core.NonEmptyList
import arrow.core.raise.context.withError
import presentation.snippets.s01suspend.slide28.Username
import presentation.snippets.s01suspend.slide29.Email

data class CreateUserRequest(val username: String, val email: String)

// Example
data class CreateUser(val username: Username, val email: Email)

context(_: Raise<NonEmptyList<String>>)
fun CreateUserRequest.toCreateUser(): CreateUser =
  withError({ errors: NonEmptyList<NonEmptyList<String>> ->
    errors.reduce { acc, errors -> acc + errors }
  }) {
    
  }
```

---
magic-move
---

# Validation

<DrawnAnnotation text="accumulating" />
<DrawnAnnotation text="accumulating" occurrence="2" label="Lift `Raise<NonEmptyList<String>>` into `Accumulate<String>`"  :connect="true" :geometry="{ label: { x: 0.6418, y: 0.7430 } }"/>

```kotlin{8-12} arrow
import arrow.core.raise.context.Raise
import arrow.core.nonEmptyListOf
import arrow.core.NonEmptyList
import presentation.support.accumulating
import arrow.core.raise.context.accumulate
import arrow.core.raise.context.withError
import presentation.snippets.s01suspend.slide28.Username
import presentation.snippets.s01suspend.slide29.Email

data class CreateUserRequest(val username: String, val email: String)

// Example
data class CreateUser(val username: Username, val email: Email)

context(_: Raise<NonEmptyList<String>>)
fun CreateUserRequest.toCreateUser(): CreateUser =
  withError({ errors: NonEmptyList<NonEmptyList<String>> ->
    errors.reduce { acc, errors -> acc + errors }
  }) {
    accumulate { // [!context Accumulate<String>, Raise<String>]
      val username by accumulating { Username(username) }
      val email by accumulating { Email(email) }
      CreateUser(username, email)
    }
  }
```

---
magic-move
---

# Validation

```kotlin{1,2,6,9} arrow
import arrow.core.raise.context.Raise
import arrow.core.nonEmptyListOf
import arrow.core.NonEmptyList
import presentation.support.accumulating
import arrow.core.raise.context.accumulate
import arrow.core.raise.context.withError
import presentation.snippets.s01suspend.slide28.Username
import presentation.snippets.s01suspend.slide29.Email

data class CreateUserRequest(val username: String, val email: String)

// Example
@JvmInline
value class InvalidUser(val errors: NonEmptyList<String>)

data class CreateUser(val username: Username, val email: Email)

context(_: Raise<InvalidUser>)
fun CreateUserRequest.toCreateUser(): CreateUser =
  withError({ errors: NonEmptyList<NonEmptyList<String>> ->
    InvalidUser(errors.reduce { acc, errors -> acc + errors })
  }) {
    accumulate {
      val username by accumulating { Username(username) } // [!context Accumulate<String>, Raise<String>]
      val email by accumulating { Email(email) }
      CreateUser(username, email)
    }
  }
```

---

# Call directly in the route

<DrawnAnnotation text="toCreateUser" :connect="true" :geometry="{ connector: { type: 'quadratic', start: { x: 0.4383, y: 0.3157 }, control: { x: 0.5142, y: 0.3131 }, end: { x: 0.6110, y: 0.2665 } } }"/>

```kotlin
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction as Transaction
import opensavvy.spine.server.arrow.routeWithRaise
import opensavvy.spine.server.respond
import io.ktor.server.application.*
import io.ktor.server.routing.routing
import presentation.snippets.s01suspend.slide19.Api
import presentation.snippets.s01suspend.slide19.CreateUser
import presentation.snippets.s01suspend.slide19.UserAlreadyExists
fun CreateUser.toCreateUser(): CreateUser = this
suspend fun insertUser(create: CreateUser): User = TODO()

// Example
fun Application.users() = routing {
  routeWithRaise(Api.Users.create) { // [!context Raise<InvalidUser>, Raise<UserAlreadyExists>]
    val create = body.toCreateUser()
    val user = insertUser(create)
    respond(user)
  }
}
```

---

# `|.` skips the call on an error

> Design proposal · KEEP-0462

<DrawnAnnotation on="1" text="error" label="Explicit error types"  :connect="true" :geometry="{ label: { x: 0.1981, y: 0.3617 } }"/>
<DrawnAnnotation on="2" text="User | MissingUser" label="Error union, single success type with multiple error types"  :connect="true" :geometry="{ label: { x: 0.7372, y: 0.5054, width: 0.3899 }, connector: { type: 'quadratic', start: { x: 0.5656, y: 0.4325 }, control: { x: 0.5676, y: 0.4811 }, end: { x: 0.6029, y: 0.4986 } } }"/>
<DrawnAnnotation on="3" text="|.mark()" label="Language level short-circuit syntax like `?.` for nullable types"  :connect="true" :geometry="{ label: { x: 0.4576, y: 0.7223 } }"/>
<DrawnAnnotation on="4" text="Unit | MissingUser | NotEligible" label="Errors accumulate automatically in the type"  :connect="true" :geometry="{ label: { x: 0.6400, y: 0.7172 } }"/>

```kotlin no-compile
error class MissingUser(val id: UserId)

suspend fun findUser(id: UserId): User | MissingUser

suspend fun User.mark(): Unit | NotEligible

suspend fun process(id: UserId): Unit | MissingUser | NotEligible =
  findUser(id)|.mark()
```

---

# `Raise` adds an expected error to the signature

<DrawnAnnotation text="Transaction" label="Type of the Exposed Typed SQL DSL" :connect="true" :geometry="{ label: { x: 0.5265, y: 0.1903 }, connector: { type: 'quadratic', start: { x: 0.3039, y: 0.2237 }, control: { x: 0.3295, y: 0.2200 }, end: { x: 0.3529, y: 0.1975 } } }"/>

```kotlin{1-2} arrow
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction as Transaction

data class CreateUser(val username: String, val email: String)
object UserTable : Table("users") {
  val username = varchar("username", 255)
  val email = varchar("email", 350)
}
fun Iterable<ResultRow>.toUser(): User = single().let { User(UserId(1), it[UserTable.username]) }

// Example
context(_: Transaction)
fun insertUser(create: CreateUser): User =
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
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.ResultRow
import java.sql.SQLException as PLSQLException
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction as Transaction
import arrow.core.raise.context.Raise
import arrow.core.raise.catch
import arrow.core.raise.context.raise

data class CreateUser(val username: String, val email: String)
object UserTable : Table("users") {
  val username = varchar("username", 255)
  val email = varchar("email", 350)
}
fun Iterable<ResultRow>.toUser(): User = single().let { User(UserId(1), it[UserTable.username]) }
fun PLSQLException.uniqueViolation(): Boolean = sqlState == "23505"

// Example
@JvmInline
value class UserAlreadyExists(val username: String)

context(_: Transaction, _: Raise<UserAlreadyExists>)
fun insertUser(create: CreateUser): User =
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
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.ResultRow
import java.sql.SQLException as PLSQLException
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction as Transaction
import arrow.core.raise.context.Raise
import arrow.core.raise.context.either
import arrow.core.raise.catch
import arrow.core.raise.context.raise

@JvmInline
value class UserAlreadyExists(val username: String)

context(_: Transaction, _: Raise<UserAlreadyExists>)
fun insertUser(create: CreateUser): User =
  catch({
    UserTable.insertReturning { insert ->
      insert[username] = create.username
      insert[email] = create.email
    }.toUser()
  }) { e: PLSQLException ->
    if(e.uniqueViolation()) raise(UserAlreadyExists(create.username))
    else throw e
  }

data class CreateUser(val username: String, val email: String)
object UserTable : Table("users") {
  val username = varchar("username", 255)
  val email = varchar("email", 350)
}
fun Iterable<ResultRow>.toUser(): User = single().let { User(UserId(1), it[UserTable.username]) }
fun PLSQLException.uniqueViolation(): Boolean = sqlState == "23505"

// Example
context(_: Transaction)
fun insertUserOrNull(create: CreateUser): User? =
  either { insertUser(create) }.getOrNull() // [!context Raise<UserAlreadyExists>]

context(_: Raise<UserAlreadyExists>)
suspend fun insertUserResult(database: Database, create: CreateUser): User =
  transaction(database) {
    insertUser(create)
  } // [!this Transaction]
```

---

# Ktor Routes using Spine

<DrawnAnnotation text="Raise<UserAlreadyExists>"/>
<DrawnAnnotation text="routeWithRaise"/>
<DrawnAnnotation text="Transaction" on="1" color="var(--fundamentals-pink)"/>
<DrawnAnnotation text="transaction" on="1" color="var(--fundamentals-pink)"/>

```kotlin ktor
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction as Transaction
import arrow.core.raise.context.Raise
import io.ktor.server.application.*
import presentation.snippets.s01suspend.slide19.UserAlreadyExists
import presentation.support.*
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import opensavvy.spine.server.arrow.routeWithRaise
import opensavvy.spine.server.respond
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import presentation.snippets.s01suspend.slide19.Api
import presentation.snippets.s01suspend.slide19.CreateUser

sealed interface Result
data class Success(val user: User) : Result
data object AlreadyExists : Result

fun CreateUser.toCreateUser(): CreateUser = this

//Example
context(_: Transaction, _: Raise<UserAlreadyExists>)
fun insertUser(create: CreateUser): User = TODO()
  
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
class: job-slide job-slide-wide
---

# Processors finish before dependencies close

```kotlin arrow
import org.jetbrains.exposed.v1.jdbc.Database
import presentation.snippets.s01suspend.slide39.users
import kotlinx.coroutines.*
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
  val database: Database = TODO()
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
fun main() = SuspendApp { // [!this SuspendAppScope]
  resourceScope { // [!context ResourceScope] [!suspend]
    val dep = dependencies() // [!suspend]
    val scope = coroutineScope() // [!suspend]
    server(Netty, host = host, port = port) { // [!suspend]
      users(dep.database)
    }
    scope.launch { dep.processor() } // [!suspend]
    awaitCancellation() // [!suspend]
  }
}
```

<JobTree
:root="{ label: 'SuspendApp' }"
:children="[
{
label: 'resourceScope', kind: 'scope',
children: [
{ label: 'dependencies()', kind: 'resource' },
{
label: 'coroutineScope()', kind: 'resource',
children: [{ label: 'launch { dep.processor() }' }],
},
{ label: 'server(Netty)', kind: 'resource' },
],
},
]"
:cancel="1"
cancel-label="SIGTERM"
/>

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
  is User if user.active -> "Hello, ${user.name}" // [!smartcast user]
  is User -> "Your account is inactive"
}
```

---

# A sealed interface names every outcome

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

# Context-sensitive resolution

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

> Experimental Kotlin 2.4.20
>
> `-Xcontext-sensitive-resolution`

---
magic-move
---

# Check patterns with if-guard

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
magic-move
---

# Destructuring follows property names

<DrawnAnnotation text="val (email, name)" label="Destructure by matching property names"  :connect="true" :geometry="{ label: { x: 0.5716, y: 0.4662 } }"/>

```kotlin
sealed interface Access {
  data class Granted(val user: User) : Access
  data class Suspended(val reason: String) : Access
  data object Missing : Access
}

//Example
data class User(val name: String, val age: Int, val email: String, val active: Boolean = true)

fun message(access: Access): String = when (access) {
  is Granted if access.user.active -> { // [!smartcast access]
    val (email, name) = access.user // [!smartcast access]
    "$name <$email>"
  }
  is Granted -> "Your account is inactive"
  is Suspended -> access.reason // [!smartcast access]
  Missing -> "Unknown user"
}
```

> Experimental Kotlin 2.4.20
>
> `-Xname-based-destructuring=complete`

---

# Destructuring follows property names

```kotlin
sealed interface Access {
  data class Granted(val user: User) : Access
  data class Suspended(val reason: String) : Access
  data object Missing : Access
}

//Example
data class User(val name: String, val age: Int, val email: String, val active: Boolean = true)

fun message(access: Access): String = when (access) {
  is Granted if access.user.active -> { // [!smartcast access]
    
    // [!error color=red email2 Unresolved reference `email2` on receiver of type `User`]
    // [!smartcast access]
    val (email2, name) = access.user
    "$name <$email2>"
  }
  is Granted -> "Your account is inactive"
  is Suspended -> access.reason // [!smartcast access]
  Missing -> "Unknown user"
}
```

> Experimental Kotlin 2.4.20
>
> `-Xname-based-destructuring=complete`

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
import arrow.core.nonEmptyListOf
import arrow.core.NonEmptyList
data class NonEmptyList<A>(val head: A, val tail: List<A>)

val literal: NonEmptyList<String> =
  
  // [!error `["a", "bb", "ccc"]` `NonEmptyList<String>` for collection literal does not\ndefine operator `of` and is not a supertype of `List`]
  ["a", "bb", "ccc"]

val nonEmptyList = NonEmptyList("a", ["bb", "ccc"])
```

---

# Custom collection literals

```kotlin
import arrow.core.nonEmptyListOf
import arrow.core.NonEmptyList
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

val empty: NonEmptyList<String> = [] // [!error `[]` No value passed for parameter `head`]
```

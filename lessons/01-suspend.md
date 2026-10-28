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
  :root="{ label: 'SuspendApp', cancelling: 1, cancelled: 4 }"
  :children="[
    { label: 'server', kind: 'resource', finalising: 2, done: 3 },
    { label: 'processor', cancelling: 2, cancelled: 3 },
  ]"
  :cancel="1"
  cancel-label="SIGTERM"
/>
</div>

<!--
The setup for this whole part, before any I/O. One application owns an HTTP
server and an event processor. Both run forever, and both belong to the
application: when it goes, they go. The server is drawn as a square box: it is
a thing we hold and must give back, not a task that runs.

Click 1: `SIGTERM` or `SIGINT` reaches the root, nothing else. Click 2: the
processor is cancelled, dashed amber, still finishing the event it is on; the
server is released, blue, a graceful stop rather than a cancellation. Click 3:
the processor is gone and the server is gone. Click 4: only now does the
application complete and the process exit.

The rest of Part 1 fills in the signatures that make this tree real:
`SuspendApp`, `resourceScope`, `server`, `launch`. The same tree comes back
with real code once the server is a resource in a `resourceScope`.
-->

---

# What is `suspend`?

```kotlin no-compile
typealias Thunk<A> = () -> A
typealias IO<A> = suspend () -> A
```

---

# What is `suspend`?

<DrawnAnnotation text="Event" :line="2"/>

```kotlin no-compile
suspend fun events(): List<Event>
suspend fun Event.process( ): Unit
```

<DrawnAnnotation text="Event" :line="2" label="Extension function" :geometry="{ label: { x: 0.2395, y: 0.4575 } }"/>

```kotlin no-compile
fun events(): IO<List<Event>>
fun Event.process(): IO<Unit>
```

---
magic-move
---

# What is `suspend`?

```kotlin no-compile
suspend fun events(): List<Event>
suspend fun Event.process( ): Unit {
  val event: Event = this
}
```
```kotlin no-compile
fun events(): IO<List<Event>>
fun Event.process(): IO<Unit>
```

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

# `suspend` is Continuation Passing Style


```kotlin
suspend fun events(): List<Event> =
  suspendCoroutine { cont: Continuation<List<Event>> ->
    
  }
```

---

# `suspend` is Continuation Passing Style

<DrawnAnnotation text="cont.resume(events)" on="0" />
<DrawnAnnotation text="cont.resumeWithException(error)" on="1" />

```kotlin
suspend fun events(): List<Event> =
  suspendCoroutine { cont ->
    sdk.fetchEvents(object : EventHandler() {
      override fun onSuccess(events: List<Event>) = cont.resume(events)
      override fun onFailure(error: Throwable) = cont.resumeWithException(error)
    })
  }
```

---

# `suspend` is Continuation Passing Style

<DrawnAnnotation text="invokeOnCancellation" />

```kotlin
suspend fun events(): List<Event> =
  suspendCoroutineCancellable { cont ->
    val call = sdk.fetchEvents(object : EventHandler() {
      override fun onSuccess(events: List<Event>) = cont.resume(events)
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

# Server as a Resource

<DrawnAnnotation text="resourceScope" label="Scope for resource management" on="0"  :geometry="{ label: { x: 0.6334, y: 0.2449 } }"/>
<DrawnAnnotation text="server" label="`install` a server into the `resourceScope`" on="1"  :geometry="{ label: { x: 0.3171, y: 0.3490 } }"/>
<DrawnAnnotation text="awaitCancellation" label="Await `SIGTERM`/`SIGINT` which cancels the `SuspendApp`\n-> cancels and awaits the running `suspend` lambda and `CoroutineScope`" on="2"  :geometry="{ label: { x: 0.4509, y: 0.5137 } }"/>
<TypeHint :line="1" receiver="SuspendAppScope">
<TypeHint :line="2" context="ResourceScope">

```kotlin arrow
fun main() = SuspendApp {
  resourceScope {
    server(Netty, host = getenv("HOST"), port = getenv("PORT").toInt()) {
      
    }
    awaitCancellation()
  }
}
```

</TypeHint>
</TypeHint>

---

# Server as a Resource

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
  install({ engine.createAndStart(host, port) }) { server, _ ->
    server.stop()
  }
```

---
class: job-slide
clicks: 4
---

# The server lives as long as the scope

<DrawnAnnotation text="awaitCancellation" color="var(--fundamentals-cancel)" on="2"/>
<DrawnAnnotation text="server" color="var(--fundamentals-finalise)" on="3"/>
<TypeHint :line="1" receiver="SuspendAppScope">
<TypeHint :line="2" context="ResourceScope">

```kotlin arrow
import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.fx.coroutines.resourceScope
import io.ktor.server.netty.Netty
import kotlinx.coroutines.awaitCancellation
import java.lang.System.getenv

val host: String = getenv("HOST")
val port: Int = getenv("PORT").toInt()

// Example
fun main() = SuspendApp {
  resourceScope {
    server(Netty, host = host, port = port) { }
    awaitCancellation()
  }
}
```

</TypeHint>
</TypeHint>

<JobTree
  :root="{ label: 'SuspendApp', cancelling: 1, cancelled: 4 }"
  :children="[
    {
      label: 'resourceScope', kind: 'scope', cancelling: 2, cancelled: 4,
      children: [{ label: 'server(Netty)', kind: 'resource', finalising: 3, done: 4, exit: 'Cancelled' }],
    },
  ]"
  :cancel="1"
  cancel-label="SIGTERM"
/>

<!--
The opening tree again, now with real code. The dotted edge is a suspending
call: `resourceScope` is not a child Job of `SuspendApp`, it is a function on
the stack, and the two scopes only meet through `suspend`. The square box is a
resource `install`ed in the `ResourceScope`: acquired now, released later.

Click 1: `SIGTERM` cancels the `SuspendApp` scope. Click 2: cancellation
reaches the block, `awaitCancellation` throws. Click 3: `resourceScope` runs
the release of every installed resource in reverse order with
`ExitCase.Cancelled`, so the server stops gracefully. Click 4: the server is
gone, the scope completes, the application can exit.
-->

---
class: job-slide
clicks: 5
magic-move
---

# Processors share that lifetime

<DrawnAnnotation text="launch" on="0"/>
<DrawnAnnotation text="launch" color="var(--fundamentals-cancel)" on="2"/>
<DrawnAnnotation text="server" color="var(--fundamentals-finalise)" on="3"/>
<TypeHint :line="1" receiver="SuspendAppScope">
<TypeHint :line="2" context="ResourceScope">

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

<!--
The event processor is the second forever task from the opening tree. `launch`
comes from `CoroutineScope`, so the processor is a child Job of `SuspendApp`,
not of `resourceScope`; `install` comes from `ResourceScope`. The two scopes
are decoupled and only composed through `suspend`, which is why the tree forks
here.

Click 1: `SIGTERM`. Click 2: cancellation reaches both the block and the
processor at once. Click 3: the processor is cancelled; `awaitCancellation`
has thrown, so the server's release runs with `ExitCase.Cancelled`. Click 4:
the server is gone and `resourceScope` returns. Click 5: the application
exits. Who owns the processor, and in which order it goes relative to the
server, is what the next slides make precise with `dependencies()` and
`coroutineScope()`.
-->

---

# Process events in parallel

<DrawnAnnotation text="launch" label="Starts a parallel task within the `SuspendAppScope`"  :geometry="{ label: { x: 0.4733, y: 0.5431 } }"/>
<TypeHint :line="1" receiver="SuspendAppScope">
<TypeHint :line="2" context="ResourceScope">
<TypeHint :line="3" receiver="Application">

```kotlin arrow
fun main() = SuspendApp {
  resourceScope {
    server(Netty, getenv("HOST"), getenv("PORT").toInt()) {
      
    }
    launch { processor() }
    awaitCancellation()
  }
}
```

</TypeHint>
</TypeHint>
</TypeHint>


---

# Process events in parallel

<DrawnAnnotation text="dependencies()" label="`suspend ResourceScope.() -> Dependencies`"  :connect="true" :geometry="{ label: { x: 0.6553, y: 0.3918 }, connector: { type: 'quadratic', start: { x: 0.3712, y: 0.3112 }, control: { x: 0.4285, y: 0.3133 }, end: { x: 0.4349, y: 0.3710 } } }"/>
<TypeHint :line="1" receiver="SuspendAppScope">
<TypeHint :line="2" context="ResourceScope">
<TypeHint :line="4" receiver="Application">

```kotlin arrow
fun main() = SuspendApp {
  resourceScope {
    val dep = dependencies()
    server(Netty, getenv("HOST"), getenv("PORT").toInt()) {
      
    }
    launch { dep.processor() }
    awaitCancellation()
  }
}
```

</TypeHint>
</TypeHint>
</TypeHint>


---

# Process events in parallel

<DrawnAnnotation text="coroutineScope" label="`suspend ResourceScope.() -> CoroutineScope`"  :connect="true" :geometry="{ label: { x: 0.6497, y: 0.3123 }, connector: { type: 'quadratic', start: { x: 0.3929, y: 0.3627 }, control: { x: 0.4187, y: 0.3590 }, end: { x: 0.4243, y: 0.3291 } } }"/>
<TypeHint :line="1" receiver="SuspendAppScope">
<TypeHint :line="2" context="ResourceScope">
<TypeHint :line="5" receiver="Application">

```kotlin arrow
fun main() = SuspendApp {
  resourceScope {
    val dep = dependencies()
    val scope = coroutineScope()
    server(Netty, getenv("HOST"), getenv("PORT").toInt()) {
      
    }
    scope.launch { dep.processor() }
    awaitCancellation()
  }
}
```

</TypeHint>
</TypeHint>
</TypeHint>


---

# Process events in parallel

<DrawnAnnotation text="Channel<Event>" label="KotlinX Coroutines suspending Queue" on="0"  :geometry="{ label: { x: 0.5182, y: 0.3454 } }"/>
<DrawnAnnotation text="for (event in events)" label="Suspending iterator" on="1"  :geometry="{ label: { x: 0.3758, y: 0.3484 } }"/>
<SuspendGutter :lines="[3, 4]">

```kotlin
import kotlinx.coroutines.Channel

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
<DrawnAnnotation text="map" label="Supports `suspend`" on="1"  :geometry="{ label: { x: 0.3358, y: 0.3438 } }"/>
<DrawnAnnotation text="collect" label="Drain the `Flow`, `suspend` awaits until entire `Flow` is drained" on="2"  :geometry="{ label: { x: 0.4445, y: 0.4002 } }"/>

```kotlin
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

---

# Process events in parallel

```kotlin
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

```kotlin arrow
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

<DrawnAnnotation text="Flow<Unit>" label="Allow further composition" />

```kotlin arrow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import presentation.support.*

object sdk {
  fun events(): Channel<Event> = Channel()
}

// Example
suspend fun processor(): Flow<Unit> =
  sdk.events().consumeAsFlow()
    .parMap(concurrency = 64) { handle(it) }
    .retry(3) { cause -> cause is RetryiableException }
    .buffer(100)
```

---

# Process events in parallel

<TypeHint :line="1" receiver="SuspendAppScope">
<TypeHint :line="2" context="ResourceScope">
<TypeHint :line="3" receiver="Application">

```kotlin arrow
import arrow.continuations.SuspendApp
import arrow.continuations.ktor.server
import arrow.fx.coroutines.resourceScope
import io.ktor.server.netty.Netty
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.serialization.Serializable

fun processor(): Flow<Unit> = emptyFlow()

// Example
fun main() = SuspendApp {
  resourceScope {
    server(Netty, getenv("HOST"), getenv("PORT").toInt()) {
      
    }
    processor().launchIn(this)
    awaitCancellation()
  }
}
```

</TypeHint>
</TypeHint>
</TypeHint>

---
magic-move
---

# Configuring your server

<DrawnAnnotation text="@Serializable" label="Compiler plugin to generate serialization code" on="0" :geometry="{ label: { x: 0.4348, y: 0.1982 } }"/>
<DrawnAnnotation text="getAs<Config>" label="Read, and deserialize the configuration" on="1"  :geometry="{ label: { x: 0.8299, y: 0.2919, width: 0.3115 }, connector: { type: 'quadratic', start: { x: 0.7997, y: 0.4112 }, control: { x: 0.8434, y: 0.3922 }, end: { x: 0.8491, y: 0.3415 } } }" :connect="true"/>
<TypeHint :line="4" receiver="SuspendAppScope">
<TypeHint :line="6" context="ResourceScope">
<TypeHint :line="7" receiver="Application">

```kotlin arrow
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
    server(Netty, host = config.host, port = config.port) {

    }
    processor().launchIn(this@SuspendApp)
    awaitCancellation()
  }
}

```

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
  HttpClient {
    /** HttpClient Configuration DSL */
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

---

# Ktor Routes using Spine

<TypeHint :line="1" receiver="Routing">
<TypeHint :line="2" receiver="TypedRespondScope<CreateUser, User, Or<Or<Never, ByCode<UserAlreadyExists>>, ByCode<InvalidUser>>, Parameters.Empty>">
<DrawnAnnotation text="body" :connect="true" on="0"  :geometry="{ connector: { type: 'quadratic', start: { x: 0.4481, y: 0.3153 }, control: { x: 0.5529, y: 0.3218 }, end: { x: 0.6209, y: 0.2636 } } }"/>
<DrawnAnnotation text="respond(result.user)" label="" :connect="true" on="1"  :geometry="{ connector: { type: 'quadratic', start: { x: 0.5036, y: 0.3647 }, control: { x: 0.6418, y: 0.3689 }, end: { x: 0.7178, y: 0.2754 } } }"/>
<DrawnAnnotation text="fail(UserAlreadyExists)" on="2"/>
<DrawnAnnotation text="fail(result.toInvalidUser())" label="" :connect="true" on="2"  :geometry="{ connector: { type: 'quadratic', start: { x: 0.6144, y: 0.4496 }, control: { x: 0.7869, y: 0.4247 }, end: { x: 0.7962, y: 0.2842 } } }"/>

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
      InvalidBody -> fail(result.toInvalidUser())
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

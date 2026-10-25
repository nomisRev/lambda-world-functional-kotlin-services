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

# What is `suspend`?

```kotlin
typealias Thunk<A> = () -> A
typealias IO<A> = suspend () -> A
```

---

# What is `suspend`?

<DrawnAnnotation text="Event" :line="2"/>

```kotlin
suspend fun events(): List<Event>
suspend fun Event.process( ): Unit
```

<DrawnAnnotation text="Event" :line="2" label="Extension function"/>

```kotlin
fun events(): IO<List<Event>>
fun Event.process(): IO<Unit>
```

---
magic-move
---

# What is `suspend`?

```kotlin
suspend fun events(): List<Event>
suspend fun Event.process( ): Unit {
  val event: Event = this
}
```
```kotlin
fun events(): IO<List<Event>>
fun Event.process(): IO<Unit>
```

---
magic-move
---

# What is `suspend`?

```kotlin{4-7}
suspend fun events(): List<Event>
suspend fun Event.process(): Unit

suspend fun program() {
  val events = events()
  events.process()
}
```
```kotlin{4-6}
fun events(): IO<List<Event>>
fun Event.process(): IO<Unit>

fun program(): IO<Unit> =
  events()
    .flatMap { it.process() }
```

---

# What is `suspend`?

<DrawnAnnotation text="suspend" label="suspend lambda" on="0"  :geometry="{ label: { x: 0.4432, y: 0.2843 } }"/>
<DrawnAnnotation text="SuspendAppScope" label="lambda with receiver, allows calling `exitApp(code)`" on="1"  :geometry="{ label: { x: 0.6555, y: 0.2907 } }"/>

```kotlin
fun SuspendApp(block: suspend SuspendAppScope.() -> Unit): Unit
```
```kotlin
fun main() = SuspendApp {
  program()
}
```

<v-click at="+2">
  <ul>
     <li>Cancellation on <code>SIGINT</code> & <code>SIGTERM</code>, graceful shutdown</li>
     <li>Structured Concurrency through KotlinX Coroutines</li>
     <li>???</li>
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

```kotlin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

suspend fun ByteArray.checksum() =
  foldIndexed(0L) { index, acc, byte ->
    if (index % 1024 == 0) currentCoroutineContext().ensureActive()
    acc * 31 + byte
  }
```

---

# `suspend` is cancellable

<DrawnAnnotation text="yield()" label="Fairness operator, reschedule yourself to allow interleaving of tasks" />

```kotlin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

suspend fun ByteArray.checksum() =
  foldIndexed(0L) { index, acc, byte ->
    if (index % 1024 == 0) yield()
    acc * 31 + byte
  }
```

---

# Server as a Resource

<DrawnAnnotation text="resourceScope" label="Scope for resource management" on="0"  :geometry="{ label: { x: 0.4021, y: 0.2457 } }"/>
<DrawnAnnotation text="server" label="`install` a server into the `resourceScope`" on="1"  :geometry="{ label: { x: 0.3185, y: 0.3440 } }"/>
<DrawnAnnotation text="awaitCancellation" label="Await `SIGTERM`/`SIGINT` which cancels the `SuspendApp`" on="2"  :geometry="{ label: { x: 0.3759, y: 0.5005 } }"/>

```kotlin
fun main() = SuspendApp {
  resourceScope {
    server(Netty, host = getenv("HOST"), port = getenv("PORT").toInt()) {
      
    }
    awaitCancellation()
  }
}
```

---

# Server as a Resource

<DrawnAnnotation text="ExitCase" label="`Completed`, `Cancelled` or `Failure`"  :geometry="{ label: { x: 0.4792, y: 0.4694 } }"/>

```kotlin
context(_: ResourceScope)
suspend fun <A> install(
  acquire: suspend () -> A,
  release: suspend (A, ExitCase) -> Unit,
): A
```

---

# Server as a Resource


```kotlin
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

# Process events in parallel

<DrawnAnnotation text="launch" label="Starts a parallel task within the `SuspendAppScope`"  :geometry="{ label: { x: 0.4733, y: 0.5431 } }"/>

```kotlin
fun main() = SuspendApp {
  resourceScope {
    server(Netty, host = getenv("HOST"), port = getenv("PORT").toInt()) {
      // configure server
    }
    launch { processor() }
    awaitCancellation()
  }
}
```

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

<DrawnAnnotation text="consumeAsFlow" label="Flow is a concurrent stream" on="0"  :geometry="{ label: { x: 0.4752, y: 0.2184 } }"/>
<DrawnAnnotation text="map" label="Supports `suspend`" on="1"  :geometry="{ label: { x: 0.3358, y: 0.3438 } }"/>
<DrawnAnnotation text="collect" label="Drain the `Flow`, `suspend` awaits until entire `Flow` is drained" on="2"  :geometry="{ label: { x: 0.4445, y: 0.4002 } }"/>

```kotlin
import kotlinx.coroutines.Channel

suspend fun processor() {
  sdk.events().consumeAsFlow()
    .map { handle(it) }
    .collect()
}
```

---

# Process events in parallel

```kotlin
import kotlinx.coroutines.Channel
import arrow.fx.coroutines.parMapUnordered

suspend fun processor() {
  sdk.events().consumeAsFlow()
    .flatMapMerge(concurrency = 64) { suspend { handle(it) }.asFlow() }
    .collect()
}
```

---

# Process events in parallel

```kotlin
import kotlinx.coroutines.Channel
import arrow.fx.coroutines.parMapUnordered

suspend fun processor() {
  sdk.events().consumeAsFlow()
    .parMapUnordered(concurrency = 64) { handle(it) }
    .collect()
}
```

---

# Process events in parallel

```kotlin
import kotlinx.coroutines.Channel
import arrow.fx.coroutines.parMap

suspend fun processor() {
  sdk.events().consumeAsFlow()
    .parMap(concurrency = 64) { handle(it) }
    .collect()
}
```

---

# Process events in parallel

<DrawnAnnotation text="Flow<Unit>" label="Allow further composition" />

```kotlin
import kotlinx.coroutines.Channel

fun processor(): Flow<Unit> =
  sdk.events().consumeAsFlow()
    .map { handle(it) }
```

---

# Process events in parallel

<DrawnAnnotation text="launchIn" label="Drain in parallel within the `SuspendAppScope`" />

```kotlin
fun main() = SuspendApp {
  resourceScope {
    server(Netty, host = getenv("HOST"), port = getenv("PORT").toInt()) {
      
    }
    processor().launchIn(this)
    awaitCancellation()
  }
}
```

---

# Configuring your server

<DrawnAnnotation text="@Serializable" label="Compiler plugin to generate serialization code" on="0" :geometry="{ label: { x: 0.4102, y: 0.2468 } }"/>
<DrawnAnnotation text="getAs<Config>" label="Read, and deserialize the configuration" on="1"  :geometry="{ label: { x: 0.8351, y: 0.4198, width: 0.3115 } }"/>

```kotlin
@Serializable data class Config(val host: String, val port: Int)

fun main() = SuspendApp {
  val config = ApplicationConfig("application.yaml").getAs<Config>()
  resourceScope {
    server(Netty, host = config.host, port = config.port) {
      
    }
    processor().launchIn(this)
    awaitCancellation()
  }
}
```

---

# Configuring your Ktor server

```kotlin
fun Application.app(config: Config) {
  install(ContentNegotiation) {
    json(Json {
      isLenient = true
      ignoreUnknownKeys = true
    })
  }
}
```

---

# Configuring your Ktor server

```kotlin
fun Application.app(config: Config) {
  install(ContentNegotiation) {
    json(Json {
      isLenient = true
      ignoreUnknownKeys = true
    })
    register(ContentType.Application.Xml, CustomXmlConverter())
  }
}
```

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

# The application owns its event consumer

<DrawnAnnotation text="onEach { handle(it) }" />
<DrawnAnnotation text="launchIn(this)" />

```kotlin
import io.ktor.server.application.Application
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

fun Application.consume(events: Flow<Event>) {
  events
    .onEach { handle(it) }
    .launchIn(this)
}
```

<!--

onEach can call suspending handlers, and preserves sequential collection.
Nothing is collected until launchIn starts a collector in Application's
CoroutineScope. Application lifetime owns this background job, not one request.
Ktor cancels its application scope on shutdown. A real consumer needs a deliberate
failure/retry policy; launchIn does not invent one. This is the ownership shape.
Sources: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/launch-in.html
https://api.ktor.io/ktor-server-core/io.ktor.server.application/-application/index.html
-->

---

# `SuspendApp` makes shutdown cancellation

<DrawnAnnotation text="SuspendApp" />
<DrawnAnnotation text="finally" />

```kotlin
import arrow.continuations.SuspendApp
import kotlinx.coroutines.awaitCancellation

fun main() = SuspendApp {
  try {
    awaitCancellation()
  } finally {
    println("Application stopped")
  }
}
```

<!--

At the outer edge we need an owner too. SuspendApp bridges process shutdown,
including SIGTERM, into coroutine cancellation and waits for cleanup subject to
its shutdown timeout. The finally here does not suspend; resource finalizers
will handle cancellation-safe suspending release next.
Source: https://arrow-kt.io/learn/coroutines/suspendapp/
-->

---

# A resource carries its release action

> First context parameter: permission to register cleanup

<DrawnAnnotation text="context(_: ResourceScope)" />
<DrawnAnnotation text="source.close()" />

```kotlin
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resource.context.install

context(_: ResourceScope)
suspend fun eventSource(connector: EventConnector): EventSource =
  install({ connector.connect() }) { source, _ ->
    source.close()
  }
```

<!--

EventConnector and EventSource are our application interfaces; close can suspend.
Arrow's install pairs successful acquisition with release. Cancellation between
those operations must not leak a resource; the scope owns that protocol.
First mention of context: this function asks for a ResourceScope in addition to
its ordinary connector argument. We will unpack resolution in the next chapter.
Source: https://arrow-kt.io/learn/coroutines/resource-safety/
-->

---

# Resource functions compose in one scope

<DrawnAnnotation text="resourceScope" />
<DrawnAnnotation text="eventSource(connector)" />
<TypeHint text="resourceScope {" context="ResourceScope">

```kotlin
import arrow.continuations.SuspendApp
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resource.context.install
import arrow.fx.coroutines.resource.context.resourceScope
import kotlinx.coroutines.flow.collect

context(_: ResourceScope)
suspend fun eventSource(connector: EventConnector): EventSource =
  install({ connector.connect() }) { source, _ -> source.close() }

// Example
fun runConsumer(connector: EventConnector) = SuspendApp {
  resourceScope {
    val source = eventSource(connector)
    val audit = eventSource(connector)
    source.events.collect { event -> handle(event) }
  }
}
```

</TypeHint>

<!--

Acquisition stays sequential and compositional. If the second acquisition fails,
the first is released. On exit, resources release in reverse acquisition order,
also on cancellation. audit stands for another independently managed connection;
it need not appear in the collector for its lifetime to be correctly managed.
Release runs under Arrow's cancellation-safe finalization protocol.
-->

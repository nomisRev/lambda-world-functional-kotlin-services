
# Process events in parallel

<DrawnAnnotation text="Channel<Event>" label="KotlinX Coroutines suspending Queue" on="0"  :geometry="{ label: { x: 0.5182, y: 0.3454 } }"/>
<DrawnAnnotation text="for (event in events)" label="Suspending iterator" on="1"  :geometry="{ label: { x: 0.3758, y: 0.3484 } }"/>

```kotlin
import kotlinx.coroutines.Channel

suspend fun processor() {
  val events: Channel<Event> = sdk.events()
  for (event in events) { // [!suspend]
    handle(event) // [!suspend]
  }
}
```

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

suspend fun program(): Unit {
  processor()
    .retry(3) { cause -> cause is RetryiableException }
    .buffer(100)
    .collect()
}

```

---

# `async` starts independent work

<DrawnAnnotation text="coroutineScope" />
<DrawnAnnotation text="async" />

```kotlin
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun greetings(users: Users, first: UserId, second: UserId) =
  coroutineScope { // [!this text=`coroutineScope {` CoroutineScope]
    val ada = async { users.find(first) }
    val grace = async { users.find(second) }
    listOf(ada.await()?.name, grace.await()?.name)
  }
```

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

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

suspend fun program(): Unit {
  processor()
    .retry(3) { cause -> cause is RetryiableException }
    .buffer(100)
    .collect()
}

```

---

# `suspend` is Continuation Passing Style

<SuspendGutter :lines="[2]">

```kotlin
suspend fun events(): List<Event> =
  suspendCoroutine { cont: Continuation<List<Event>> ->
    
  }
```

</SuspendGutter>

---

# `suspend` is Continuation Passing Style

<SuspendGutter :lines="[2]">
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

</SuspendGutter>

---

# `suspend` is cancellable

<DrawnAnnotation text="call" />
<DrawnAnnotation text="invokeOnCancellation { call.cancel() }" />

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
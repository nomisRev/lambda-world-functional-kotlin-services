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
<SmartCast text="user" :line="2" occurrence="3">

```kotlin
fun greeting(user: User?): String =
  if (user == null) "Unknown user" else "Hello, ${user.name}"
```

</SmartCast>

<!--

The compiler knows this stable local parameter cannot be null after the early
return. No manual unwrapping. Smart casts also follow type checks and control
flow, provided the value cannot change between the check and use.
Source: https://kotlinlang.org/docs/typecasts.html
-->

---
magic-move
---

# `when` handles absence and guarded values

<DrawnAnnotation text="is User if user.active" />
<SmartCast text="user" :line="3" occurrence="2">

```kotlin
fun greeting(user: User?): String = when (user) {
  null -> "Unknown user"
  is User if user.active -> "Hello, ${user.name}"
  is User -> "Your account is inactive"
}
```

</SmartCast>

---

# A sealed interface names every outcome

<DrawnAnnotation text="sealed interface" />
<SmartCast text="access" :line="8">
<SmartCast text="access" :line="9">

```kotlin
sealed interface Access {
  data class Granted(val user: User) : Access
  data class Suspended(val reason: String) : Access
  data object Missing : Access
}

fun message(access: Access): String = when (access) {
  is Access.Granted -> "Hello, ${access.user.name}"
  is Access.Suspended -> access.reason
  Access.Missing -> "Unknown user"
}
```

</SmartCast>
</SmartCast>

---
magic-move
---

# `-Xcontext-sensitive-resolution` in Kotlin 2.4.20


<DrawnAnnotation text="when (access)" label="Prefixed type is known"  :connect="true" :geometry="{ label: { x: 0.6973, y: 0.5960 } }"/>
<SmartCast text="access" :line="8">
<SmartCast text="access" :line="9">

```kotlin
sealed interface Access {
  data class Granted(val user: User) : Access
  data class Suspended(val reason: String) : Access
  data object Missing : Access
}

fun message(access: Access): String = when (access) {
  is Granted -> "Hello, ${access.user.name}"
  is Suspended -> access.reason
  Missing -> "Unknown user"
}
```

</SmartCast>
</SmartCast>

---
magic-move
---

# A sealed interface names every outcome

<DrawnAnnotation text="if access.user.active" label="if-guard checks condition" :connect="true" :geometry="{ label: { x: 0.6988, y: 0.6260 }, connector: { type: 'quadratic', start: { x: 0.4385, y: 0.5521 }, control: { x: 0.5647, y: 0.5437 }, end: { x: 0.5800, y: 0.6052 } } }"/>
<SmartCast text="access" :line="8">
<SmartCast text="access" :occurrence="2" :line="8">
<SmartCast text="access" :line="10">

```kotlin
sealed interface Access {
  data class Granted(val user: User) : Access
  data class Suspended(val reason: String) : Access
  data object Missing : Access
}

fun message(access: Access): String = when (access) {
  is Granted if access.user.active -> "Hello, ${access.user.name}"
  is Granted -> "Your account is inactive"
  is Suspended -> access.reason
  Missing -> "Unknown user"
}
```

</SmartCast>
</SmartCast>
</SmartCast>

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

<InlineCompilerError text="[&quot;a&quot;, &quot;bb&quot;, &quot;ccc&quot;]" message="`NonEmptyList<String>` for collection literal does not\ndefine operator `of` and is not a supertype of `List`">

```kotlin
data class NonEmptyList<A>(val head: A, val tail: List<A>)

val literal: NonEmptyList<String> =
  
  ["a", "bb", "ccc"]

val nonEmptyList = NonEmptyList("a", ["bb", "ccc"])
```

</InlineCompilerError>

---

# Custom collection literals

<InlineCompilerError text="NonEmptyList(&quot;a&quot;, [&quot;bb&quot;, &quot;ccc&quot;])" message="`constructor<A>(head: A, tail: List<A>): NonEmptyList<A>` is private">
<InlineCompilerError text="[]" message="No value passed for parameter `head`">

```kotlin
data class NonEmptyList<A> private constructor(val head: A, val tail: List<A>) {
  companion object {
    operator fun <A> of(head: A, vararg tail: A) =
      NonEmptyList(head, tail.toList())
  }
}

val literal = ["a", "bb", "ccc"]

val nonEmptyList =
  NonEmptyList("a", ["bb", "ccc"])

val empty = []
```

</InlineCompilerError>
</InlineCompilerError>

---
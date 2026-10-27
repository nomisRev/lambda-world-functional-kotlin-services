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
<SmartCast text="user" :line="3">

```kotlin
fun greeting(user: User?): String {
  if (user == null) return "Unknown user"
  return "Hello, ${user.name}"
}
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
  else -> "Your account is inactive"
}
```

</SmartCast>

<!--

when is an expression. The guard is checked only after the type condition
matches, so user is already smart-cast in the guard. A failed guard continues
matching; we still need the inactive case. Nullable input and guard conditions
compose in one expression.
Source: https://kotlinlang.org/docs/control-flow.html#guard-conditions
-->

---

# Value classes distinguish domain values

> JVM value classes today · Valhalla is a separate JVM evolution

<DrawnAnnotation text="@JvmInline" />

```kotlin
@JvmInline
value class UserId(val value: Long)

@JvmInline
value class OrderId(val value: Long)

fun userKey(id: UserId): String = "users/${id.value}"

val key = userKey(UserId(42))
```

<!--

UserId and OrderId cannot be interchanged, even though both wrap Long. The JVM
compiler can use the underlying value in many positions, but nullable, generic,
and interface usage can require boxing. Do not promise zero allocation.
Valhalla concerns JVM value objects and future representation opportunities;
this example does not depend on it or imply Kotlin already uses it.
Sources: https://kotlinlang.org/docs/inline-classes.html
https://openjdk.org/projects/valhalla/
-->

---

# Destructuring can follow property names

> Experimental Kotlin 2.4.20 · `-Xname-based-destructuring=complete`

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

<!--

email is the third declared property, but the first selected value. Under this
explicit experimental compiler mode, the names decide rather than component order.
Reordering properties does not silently change this meaning. The flag is enabled
in this deck's compiler project; do not imply this is positional destructuring
under the old default mode.
Source: https://kotlinlang.org/docs/whatsnew2320.html#name-based-destructuring
-->

---

# A sealed interface names every outcome

<DrawnAnnotation text="sealed interface Access" />

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

<!--

The compiler checks exhaustiveness. Each case brings its own payload, and each
is-branch smart-casts the subject. Adding a new direct subtype means reviewing
the decisions that used to cover all outcomes. No catch-all is needed.
Source: https://kotlinlang.org/docs/sealed-classes.html
-->

---
magic-move
---

# The expected type resolves the case names

> Experimental Kotlin 2.4.20 · `-Xcontext-sensitive-resolution`

<DrawnAnnotation text="is Granted if access.user.active" />

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

<!--

The subject's type supplies the namespace. Context-sensitive resolution is a
separate feature from context parameters: here it shortens subtype names.
Read the first branch left-to-right: match a Granted, refine access, then check
its user's state. The unguarded Granted branch still handles a failed guard.
The version label matches this deck's compiler; the flag is enabled in build.gradle.kts.
Source: https://kotlinlang.org/docs/whatsnew22.html#preview-of-context-sensitive-resolution
-->

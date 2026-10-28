
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

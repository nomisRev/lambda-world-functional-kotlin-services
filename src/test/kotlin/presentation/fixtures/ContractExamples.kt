package presentation.fixtures

import arrow.core.raise.Raise
import arrow.fx.coroutines.resource.context.ResourceScope
import arrow.fx.coroutines.resource.context.install
import kotlinx.coroutines.suspendCancellableCoroutine
import presentation.support.*
import kotlin.coroutines.resumeWithException

// Standalone contract examples retained for regression tests after the deck changed.
suspend fun Sdk.awaitUser(id: UserId): User = suspendCancellableCoroutine { cont ->
  val request = fetch(id) { result ->
    result.fold({ cont.resumeWith(Result.success(it)) }, cont::resumeWithException)
  }
  cont.invokeOnCancellation { request.cancel() }
}

context(scope: ResourceScope)
suspend fun eventSource(connector: EventConnector): EventSource =
  install({ connector.connect() }) { source, _ -> source.close() }

data class RegisterUserRequest(val username: String, val email: String) {
  context(_: Raise<IncorrectInput>)
  fun toRegisterUser(): RegisterUser = validate(::IncorrectInput) {
    val name by accumulating { Username(username) }
    val address by accumulating { Email(email) }
    RegisterUser(name, address)
  }
}

data class MissingUser(val id: UserId)

context(users: Users, logger: Logger, raise: Raise<MissingUser>)
suspend fun greeting(id: UserId): String {
  val user = users.find(id) ?: raise.raise(MissingUser(id))
  return "Hello, ${user.name}".also { logger.info(it) }
}

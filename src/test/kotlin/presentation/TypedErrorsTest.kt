package presentation

import arrow.core.Either
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import presentation.snippets.s04errors.slide36.Username as SlideUsername
import presentation.snippets.s04errors.slide37.RegisterUserRequest
import presentation.snippets.s04errors.slide39.MissingUser
import presentation.snippets.s04errors.slide39.greeting
import presentation.support.IncorrectInput
import presentation.support.InvalidField
import presentation.support.Logger
import presentation.support.User
import presentation.support.UserId
import presentation.support.Users

class TypedErrorsTest {
  @Test
  fun `independent fields collect every error within each field`() {
    val result = either { RegisterUserRequest("", "").toRegisterUser() }
    assertEquals(
      Either.Left(IncorrectInput(nonEmptyListOf(
        InvalidField(nonEmptyListOf("Cannot be blank", "is too short (minimum is 1 characters)"), "username"),
        InvalidField(nonEmptyListOf("Cannot be blank", "'' is invalid email"), "email"),
      ))),
      result,
    )
  }

  @Test
  fun `a successful request constructs normalized domain values`() {
    val result = either { RegisterUserRequest("  Ada  ", "ada@example.com").toRegisterUser() }
    val user = assertNotNull(result.getOrNull())
    assertEquals("Ada", user.username.value)
    assertEquals("ada@example.com", user.email.value)
  }

  @Test
  fun `the shown value class constructor raises all failing rules`() {
    val result = either { SlideUsername(" ") }
    assertEquals(
      Either.Left(InvalidField(
        nonEmptyListOf("Cannot be blank", "is too short (minimum is 1 characters)"),
        "username",
      )),
      result,
    )
  }

  @Test
  fun `the supplied capabilities interpret the successful program`() = runTest {
    val messages = mutableListOf<String>()
    val users = Users { id -> User(id, "Ada") }
    val logger = Logger { messages += it }
    val result = with(users) {
      with(logger) { either { greeting(UserId(42)) } }
    }
    assertEquals(Either.Right("Hello, Ada"), result)
    assertEquals(1, messages.size)
  }

  @Test
  fun `a missing user short circuits the greeting and its log`() = runTest {
    val messages = mutableListOf<String>()
    val users = Users { null }
    val logger = Logger { messages += it }
    val result = with(users) {
      with(logger) { either { greeting(UserId(42)) } }
    }
    assertEquals(Either.Left(MissingUser(UserId(42))), result)
    assertTrue(messages.isEmpty())
  }
}

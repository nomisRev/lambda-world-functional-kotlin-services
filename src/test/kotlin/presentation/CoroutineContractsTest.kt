package presentation

import arrow.fx.coroutines.resource.context.resourceScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import presentation.fixtures.awaitUser
import presentation.fixtures.eventSource
import presentation.support.EventConnector
import presentation.support.EventSource
import presentation.support.Request
import presentation.support.Sdk
import presentation.support.User
import presentation.support.UserId

// Regression contracts for callback adaptation and resource ownership.
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CoroutineContractsTest {
  private class CallbackSdk : Sdk {
    lateinit var callback: (Result<User>) -> Unit
    var cancellationCount = 0
    override fun fetch(id: UserId, callback: (Result<User>) -> Unit): Request {
      this.callback = callback
      return Request { cancellationCount++ }
    }
  }

  @Test
  fun `the callback result becomes the suspend result`() = runTest {
    val sdk = CallbackSdk()
    val expected = User(UserId(1), "Ada")
    val pending = async { sdk.awaitUser(expected.id) }
    runCurrent()
    sdk.callback(Result.success(expected))
    assertEquals(expected, pending.await())
    assertEquals(0, sdk.cancellationCount)
  }

  @Test
  fun `cancelling the coroutine cancels the SDK request`() = runTest {
    val sdk = CallbackSdk()
    val pending = async { sdk.awaitUser(UserId(1)) }
    runCurrent()
    pending.cancelAndJoin()
    assertEquals(1, sdk.cancellationCount)
    // A late single callback is legal: cancellation still wins delivery.
    sdk.callback(Result.success(User(UserId(1), "Ada")))
    assertTrue(pending.isCancelled)
  }

  private fun connector(closed: MutableList<Int>): EventConnector {
    var acquired = 0
    return EventConnector {
      val number = ++acquired
      object : EventSource {
        override val events = emptyFlow<presentation.support.Event>()
        override suspend fun close() {
          // Finalizers may suspend even while their parent is being cancelled.
          delay(1)
          closed += number
        }
      }
    }
  }

  @Test
  fun `resources close in reverse acquisition order`() = runTest {
    val closed = mutableListOf<Int>()
    val connector = connector(closed)
    resourceScope {
      eventSource(connector)
      eventSource(connector)
      assertTrue(closed.isEmpty())
    }
    assertEquals(listOf(2, 1), closed)
  }

  @Test
  fun `failed acquisition releases previously acquired resources`() = runTest {
    val closed = mutableListOf<Int>()
    val connector = connector(closed)
    assertFailsWith<IllegalStateException> {
      resourceScope {
        eventSource(connector)
        eventSource(EventConnector { error("connection refused") })
      }
    }
    assertEquals(listOf(1), closed)
  }

  @Test
  fun `cancellation still waits for suspending resource finalizers`() = runTest {
    val closed = mutableListOf<Int>()
    val connector = connector(closed)
    val pending = async {
      resourceScope {
        eventSource(connector)
        eventSource(connector)
        awaitCancellation()
      }
    }
    runCurrent()
    pending.cancelAndJoin()
    assertEquals(listOf(2, 1), closed)
  }
}

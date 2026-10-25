package presentation

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler

class ContextResolutionTest {
  private data class Compilation(val result: ExitCode, val diagnostics: String)

  private fun compile(source: String): Compilation {
    val directory = Files.createTempDirectory("context-resolution-").toFile()
    return try {
      val input = directory.resolve("Resolution.kt").apply { writeText(source) }
      val diagnostics = ByteArrayOutputStream()
      val result = K2JVMCompiler().exec(
        PrintStream(diagnostics),
        "-no-stdlib", "-no-reflect",
        "-classpath", System.getProperty("snippetTestClasspath"),
        "-d", directory.resolve("classes").path,
        input.path,
      )
      Compilation(result, diagnostics.toString())
    } finally {
      directory.deleteRecursively()
    }
  }

  @Test
  fun `an implicit receiver satisfies a context parameter`() {
    val (result, diagnostics) = compile("""
      class Logger
      context(logger: Logger) fun needsContext() = logger
      fun Logger.suppliesContext() = needsContext()
    """.trimIndent())
    assertEquals(ExitCode.OK, result, diagnostics)
  }

  @Test
  fun `a context parameter is not an implicit receiver`() {
    val (result, diagnostics) = compile("""
      class Logger
      fun Logger.needsReceiver() = this
      context(logger: Logger) fun cannotSupplyReceiver() = needsReceiver()
    """.trimIndent())
    assertEquals(ExitCode.COMPILATION_ERROR, result, diagnostics)
    assertTrue(diagnostics.contains("needsReceiver"), diagnostics)
    assertTrue(diagnostics.contains("unresolved reference", ignoreCase = true), diagnostics)
  }

  @Test
  fun `the named context value can be used explicitly as receiver`() {
    val (result, diagnostics) = compile("""
      class Logger
      fun Logger.needsReceiver() = this
      context(logger: Logger) fun explicitReceiver() = logger.needsReceiver()
    """.trimIndent())
    assertEquals(ExitCode.OK, result, diagnostics)
  }
}

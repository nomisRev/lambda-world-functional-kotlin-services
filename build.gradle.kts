plugins {
  id("org.jetbrains.kotlin.jvm") version "2.4.20"
}

repositories { mavenCentral() }

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
  implementation("io.arrow-kt:arrow-core:2.2.3")
  implementation("io.arrow-kt:arrow-fx-coroutines:2.2.3")
  implementation("io.arrow-kt:suspendapp:2.2.3")
  implementation("io.arrow-kt:suspendapp-ktor:2.2.3")
  implementation("io.ktor:ktor-server-netty:3.6.0")
  implementation("io.ktor:ktor-server-auth:3.6.0")

  testImplementation(kotlin("test"))
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
  testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.20")
}

kotlin {
  jvmToolchain(21)
  compilerOptions.freeCompilerArgs.addAll(
    "-Xcontext-sensitive-resolution",
    "-Xname-based-destructuring=complete",
    "-opt-in=io.ktor.utils.io.ExperimentalKtorApi",
    "-opt-in=arrow.core.raise.ExperimentalRaiseAccumulateApi",
  )
}

// Generation is explicit: check must fail if the checked-in examples lag the deck.
val snippetsCheck = tasks.register<Exec>("snippetsCheck") {
  commandLine("npm", "run", "snippets:check")
}

tasks.check { dependsOn(snippetsCheck) }

tasks.test {
  systemProperty("snippetTestClasspath", sourceSets.main.get().runtimeClasspath.asPath)
}

plugins {
  id("org.jetbrains.kotlin.jvm") version "2.4.20"
}

repositories { mavenCentral() }

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

  implementation(arrow.core)
  implementation(arrow.fx.coroutines)
  implementation(arrow.suspendapp)
  implementation(arrow.suspendapp.ktor)

  implementation(ktorLibs.serialization.kotlinx.json)
  implementation(ktorLibs.server.netty)
  implementation(ktorLibs.server.defaultHeaders)
  implementation(ktorLibs.server.cors)
  implementation(ktorLibs.server.contentNegotiation)
  implementation(ktorLibs.server.config.yaml)
  implementation(ktorLibs.server.auth.jwt)
  implementation(ktorLibs.client.contentNegotiation)

  implementation(exposed.jdbc)

  implementation("dev.opensavvy.spine:api:0.12.1")
  implementation("dev.opensavvy.spine:server:0.12.1")
  implementation("dev.opensavvy.spine:server-arrow:0.12.1")
  implementation("dev.opensavvy.spine:client:0.12.1")



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
    "-Xcollection-literals"
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

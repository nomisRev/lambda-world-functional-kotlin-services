rootProject.name = "functional-kotlin-services"

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
  versionCatalogs {
    create("arrow").from("io.arrow-kt:arrow-version-catalog:2.2.3")
    create("ktorLibs").from("io.ktor:ktor-version-catalog:3.6.0")
    create("exposed").from("org.jetbrains.exposed:exposed-version-catalog:1.6.0")
  }
}

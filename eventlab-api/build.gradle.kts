import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import java.util.Locale

plugins {
  kotlin("jvm") version "2.3.21"
  kotlin("plugin.spring") version "2.3.21"
  id("org.springframework.boot") version "4.1.1"
  id("io.spring.dependency-management") version "1.1.7"
  id("com.diffplug.spotless") version "8.10.4"
}

group = "dev.eventlab"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webmvc")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("tools.jackson.module:jackson-module-kotlin")
  testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.springframework.boot:spring-boot-testcontainers")
  testImplementation("org.testcontainers:testcontainers-postgresql")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")
  implementation("org.springframework.boot:spring-boot-starter-jdbc")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll(
      "-Xjsr305=strict",
      "-Xannotation-default-target=param-property",
    )
    allWarningsAsErrors = true
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging {
    events = emptySet()
  }
  addTestListener(ColoredTestOutput())
}

spotless {
  kotlin {
    ktlint("1.8.0")
  }
  kotlinGradle {
    ktlint("1.8.0")
  }
}

class ColoredTestOutput : TestListener {
  private val linesByClass = mutableMapOf<String, MutableList<String>>()

  override fun beforeSuite(suite: TestDescriptor) = Unit

  override fun beforeTest(testDescriptor: TestDescriptor) = Unit

  override fun afterTest(
    testDescriptor: TestDescriptor,
    result: TestResult,
  ) {
    val className = testDescriptor.className ?: return
    val name = testDescriptor.displayName.removeSuffix("()")
    val time = grey("${result.endTime - result.startTime} ms")
    val lines = linesByClass.getOrPut(className) { mutableListOf() }

    when (result.resultType) {
      TestResult.ResultType.SUCCESS -> {
        lines += "   ${green("✔")} $name  $time"
      }

      TestResult.ResultType.SKIPPED -> {
        lines += "   ${yellow("○ $name")}"
      }

      else -> {
        lines += "   ${red("✘ $name")}  $time"
        result.exceptions.forEach { lines += describeFailure(it, className) }
      }
    }
  }

  override fun afterSuite(
    suite: TestDescriptor,
    result: TestResult,
  ) {
    val className = suite.className

    when {
      suite.parent == null -> printSummary(result)
      className != null -> printClass(className, result)
    }
  }

  private fun printClass(
    className: String,
    result: TestResult,
  ) {
    val failed = result.resultType == TestResult.ResultType.FAILURE
    val badge =
      if (failed) {
        paint(
          "97;41",
          " FAIL ",
        )
      } else {
        paint("30;42", " PASS ")
      }
    val title = bold(className.substringAfterLast('.'))

    println()
    println("$badge $title  ${grey(seconds(result))}")
    linesByClass.remove(className)?.forEach(::println)
  }

  private fun printSummary(result: TestResult) {
    val counts =
      listOfNotNull(
        green("${result.successfulTestCount} passed"),
        red("${result.failedTestCount} failed")
          .takeIf { result.failedTestCount > 0 },
        yellow("${result.skippedTestCount} skipped")
          .takeIf { result.skippedTestCount > 0 },
        "${result.testCount} total",
      )

    println()
    println("${bold("Tests:")} ${counts.joinToString(", ")}")
    println("${bold("Time:")}  ${seconds(result)}")
  }

  private fun describeFailure(
    error: Throwable,
    className: String,
  ): List<String> {
    val rootCause = generateSequence(error) { it.cause }.last()
    val location = error.stackTrace.firstOrNull { it.className == className }

    return listOfNotNull(
      "       ${red(error.message ?: error.toString())}",
      "       ${red("caused by: $rootCause")}".takeIf { rootCause !== error },
      location?.let {
        "         ${grey(
          "at ${it.fileName}:${it.lineNumber}",
        )}"
      },
    )
  }

  private fun seconds(result: TestResult): String =
    String.format(
      Locale.ROOT,
      "%.1f s",
      (result.endTime - result.startTime) / 1000.0,
    )

  private fun paint(
    code: String,
    text: String,
  ): String = "\u001B[${code}m$text\u001B[0m"

  private fun bold(text: String): String = paint("1", text)

  private fun green(text: String): String = paint("32", text)

  private fun red(text: String): String = paint("31", text)

  private fun yellow(text: String): String = paint("33", text)

  private fun grey(text: String): String = paint("90", text)
}

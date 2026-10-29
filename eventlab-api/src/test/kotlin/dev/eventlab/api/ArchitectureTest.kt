package dev.eventlab.api

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.functions
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

class ArchitectureTest {
  private val production = Konsist.scopeFromProduction()

  @Test
  fun `should keep HTTP types out of services`() {
    val serviceFunctions =
      production
        .classes()
        .withNameEndingWith("Service")
        .functions()

    serviceFunctions.assertFalse { function ->
      val types =
        function.parameters.map { it.type.text } +
          listOfNotNull(function.returnType?.text)

      types.any { it.contains("Request") || it.contains("Response") }
    }
  }

  @Test
  fun `should keep domain models free of frameworks`() {
    val domainFiles =
      production.files.filter {
        it.name in listOf("Event", "Registration", "Participant")
      }

    domainFiles.assertFalse { file ->
      file.hasImport {
        it.name.startsWith("org.springframework") ||
          it.name.startsWith("tools.jackson") ||
          it.name.startsWith("com.fasterxml.jackson")
      }
    }
  }

  @Test
  fun `should let controllers depend only on services`() {
    val controllers = production.classes().withNameEndingWith("Controller")

    controllers.assertTrue { controller ->
      controller.primaryConstructor
        ?.parameters
        .orEmpty()
        .all { it.type.name.endsWith("Service") }
    }
  }

  @Test
  fun `should keep repositories unaware of services`() {
    val repositories = production.classes().withNameEndingWith("Repository")

    repositories.assertFalse { repository ->
      repository.primaryConstructor
        ?.parameters
        .orEmpty()
        .any { it.type.name.endsWith("Service") }
    }
  }
}

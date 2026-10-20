plugins {
  kotlin("jvm") version "2.3.21"
  kotlin("plugin.spring") version "2.3.21"
  id("org.springframework.boot") version "4.1.1"
  id("io.spring.dependency-management") version "1.1.7"
  id("com.diffplug.spotless") version "8.10.4"
}

group = "dev.eventlab"
version = "0.0.1-SNAPSHOT"

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
}

spotless {
  kotlin {
    ktlint("1.8.0")
  }
  kotlinGradle {
    ktlint("1.8.0")
  }
}

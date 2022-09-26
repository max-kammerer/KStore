import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization")
  id("com.android.library")
  id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.11.1"
  id("maven-publish")
  id("org.jetbrains.kotlinx.kover") version "0.6.0"
}

group = "io.github.xxfast"
version = "0.1-SNAPSHOT"

repositories {
  mavenCentral()
}

android {
  compileSdk = 31
  defaultConfig {
    minSdk = 21
    targetSdk = 31
  }

  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  lint {
    // TODO: Figure out why the linter is failing on CI
    abortOnError = false
  }
}

kotlin {
  android {
    compilations.all {
      kotlinOptions {
        jvmTarget = "1.8"
      }
    }
  }

  jvm("desktop") {
    compilations.all {
      kotlinOptions.jvmTarget = "1.8"
    }
    testRuns["test"].executionTask.configure {
      useJUnitPlatform()
    }
  }

  js(IR) {
    nodejs()
  }

  val macosX64 = macosX64()
  val macosArm64 = macosArm64()
  val iosArm64 = iosArm64()
  val iosX64 = iosX64()
  val iosSimulatorArm64 = iosSimulatorArm64()
  val watchosArm32 = watchosArm32()
  val watchosArm64 = watchosArm64()
  val watchosX64 = watchosX64()
  val watchosSimulatorArm64 = watchosSimulatorArm64()
  val tvosArm64 = tvosArm64()
  val tvosX64 = tvosX64()
  val tvosSimulatorArm64 = tvosSimulatorArm64()
  val appleTargets = listOf(
    macosX64, macosArm64,
    iosArm64, iosX64, iosSimulatorArm64,
    watchosArm32, watchosArm64, watchosX64,
    watchosSimulatorArm64,
    tvosArm64, tvosX64, tvosSimulatorArm64,
  )

  appleTargets.forEach { target ->
    with(target) {
      binaries {
        framework {
          baseName = "KStore"
        }
      }
    }
  }

  linuxX64("linux")
  mingwX64("windows")

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("com.squareup.okio:okio:3.2.0")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.4.0")

      }
    }

    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
        implementation("app.cash.turbine:turbine:0.9.0")
      }
    }

    val androidMain by getting
    val androidTest by getting {
      dependencies {
        implementation("junit:junit:4.13.2")
        implementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
        implementation("org.junit.jupiter:junit-jupiter-engine:5.8.1")
        implementation("androidx.test.ext:junit:1.1.3")
      }
    }

    val desktopMain by getting {
      dependencies {
        implementation("com.squareup.okio:okio:3.2.0")
      }
    }
    val desktopTest by getting

    val jsMain by getting {
      dependencies {
        implementation("com.squareup.okio:okio-nodefilesystem:3.2.0")
      }
    }
    val jsTest by getting

    val appleMain by creating {
      dependsOn(commonMain)
    }
    val appleTest by creating

    appleTargets.forEach { target ->
      getByName("${target.targetName}Main") { dependsOn(appleMain) }
      getByName("${target.targetName}Test") { dependsOn(appleTest) }
    }

    val linuxMain by getting {
      dependencies {
        implementation("com.squareup.okio:okio:3.2.0")
      }
    }

    val linuxTest by getting

    val windowsMain by getting {
      dependencies {
        implementation("com.squareup.okio:okio:3.2.0")
      }
    }

    val windowsTest by getting
  }

  val publicationsFromMainHost =
    listOf(jvm("desktop"), js(IR), android()).map { it.name } + "kotlinMultiplatform"

  publishing {
    publications {
      matching { it.name in publicationsFromMainHost }.all {
        val targetPublication = this@all
        tasks.withType<AbstractPublishToMaven>()
          .matching { it.publication == targetPublication }
          .configureEach { onlyIf {
            System.getProperty("os.name").startsWith("Linux")
          } }
      }
    }
  }
}

publishing {
  repositories {
    maven {
      name = "Space"
      url = uri("https://packages.jetbrains.team/maven/p/kmp-publish-research/kmp-publish-research")
      credentials {
        username = System.getenv("S_NAME")
        password = System.getenv("S_KEY")
      }
    }
  }
}

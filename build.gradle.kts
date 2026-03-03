import jdk.internal.agent.resources.agent
import sun.jvmstat.monitor.MonitoredVmUtil.commandLine
import sun.jvmstat.monitor.MonitoredVmUtil.mainClass

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.graalvm.native)
    application
}

group = "me.kartikarora"
version = libs.versions.klaat.get()

// Configure Java toolchain to use GraalVM
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }
}

repositories {
    mavenCentral()
}

// Build variant configuration
val buildVariant = project.findProperty("buildVariant")?.toString() ?: "dev"
val isDevBuild = buildVariant == "dev"
val isSnapshot = System.getenv("IS_SNAPSHOT") == "true"
val versionName = System.getenv("VERSION_NAME") ?: project.findProperty("versionName")?.toString() ?: project.version.toString()

dependencies {
    implementation(libs.clikt)
    implementation(libs.bundles.google.apis)
    implementation(libs.bundles.ktor)
    implementation(libs.logback.classic)

    // Markdown parsing
    implementation(libs.flexmark.all)

    // HTML templating
    implementation(libs.freemarker)

    // JSON handling
    implementation(libs.gson)

    // YAML for markdown frontmatter
    implementation(libs.kaml)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }
}

tasks.test {
    useJUnitPlatform()

    // Ensure test task uses the GraalVM toolchain
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    })
}

application {
    mainClass.set("me.kartikarora.klaat.MainKt")
    // Set different application name based on build variant
    applicationName = if (isSnapshot) "klaat-snapshot" else if (isDevBuild) "klaat-dev" else "klaat"
}

// Generate BuildConfig.kt with build variant info
val generateBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/source/buildConfig")
    outputs.dir(outputDir)

    doLast {
        val buildConfigFile = outputDir.get().file("me/kartikarora/klaat/BuildConfig.kt").asFile
        buildConfigFile.parentFile.mkdirs()
        buildConfigFile.writeText(
            """
            package me.kartikarora.klaat

            object BuildConfig {
                const val BUILD_VARIANT = "$buildVariant"
                const val IS_DEV_BUILD = $isDevBuild
                const val VERSION_NAME = "$versionName"
                const val IS_SNAPSHOT = $isSnapshot
                const val BINARY_NAME = "${if (isSnapshot) "klaat-snapshot" else "klaat"}"
            }
            """.trimIndent()
        )
    }
}

sourceSets {
    main {
        kotlin {
            srcDir(layout.buildDirectory.dir("generated/source/buildConfig"))
        }
    }
}

tasks.named("compileKotlin") {
    dependsOn(generateBuildConfig)
}

// Custom tasks for build variants
tasks.register("buildDev") {
    group = "build"
    description = "Build the development variant (JVM)"
    dependsOn("installDist")
    doFirst {
        project.setProperty("buildVariant", "dev")
    }
}

tasks.register("buildProd") {
    group = "build"
    description = "Build the production variant (JVM)"
    dependsOn("installDist")
    doFirst {
        project.setProperty("buildVariant", "prod")
    }
}

// Native build tasks
tasks.register("nativeDev") {
    group = "build"
    description = "Build development native binary"
    dependsOn("nativeCompile")
    doFirst {
        project.setProperty("buildVariant", "dev")
    }
}

tasks.register("nativeProd") {
    group = "build"
    description = "Build production native binary"
    dependsOn("nativeCompile")
    doFirst {
        project.setProperty("buildVariant", "prod")
    }
}

// Quick run tasks
tasks.register<Exec>("runNative") {
    group = "application"
    description = "Build and run native binary"
    dependsOn("nativeCompile")
    commandLine("${layout.buildDirectory.get()}/native/nativeCompile/klaat")
    args(project.findProperty("args")?.toString()?.split(" ") ?: emptyList<String>())
}

// GraalVM Native Image configuration
graalvmNative {
    // Use toolchain JVM for GraalVM native image
    toolchainDetection.set(true)

    binaries {
        named("main") {
            imageName.set(if (isSnapshot) "klaat-snapshot" else "klaat")
            mainClass.set("me.kartikarora.klaat.MainKt")

            // Set GraalVM home for native-image
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(25))
                vendor.set(JvmVendorSpec.GRAAL_VM)
            })

            // Optimize for local dev vs CI
            val isCI = System.getenv("CI") == "true"

            if (isCI) {
                buildArgs.add("--verbose")
            }

            buildArgs.add("--no-fallback")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("--enable-url-protocols=http,https")

            // Required for reflection used by libraries
            buildArgs.add("--initialize-at-build-time=kotlin,kotlinx,org.slf4j,ch.qos.logback,com.vladsch.flexmark")
            buildArgs.add("--initialize-at-run-time=io.ktor,io.netty,org.eclipse.jetty")

            // Native image configs will be auto-discovered from META-INF/native-image/

            // Quick build for local dev
            if (!isCI) {
                buildArgs.add("-Ob") // Quick build mode
            }
        }
    }

    // Agent configuration for generating native-image configs
    // Disabled by default since we have pre-configured reflection configs
    // Enable with: ./gradlew -Pagent test
    agent {
        defaultMode.set("standard")
        builtinCallerFilter.set(true)
        builtinHeuristicFilter.set(true)
        enableExperimentalPredefinedClasses.set(false)
        enableExperimentalUnsafeAllocationTracing.set(false)
        trackReflectionMetadata.set(true)
        enabled.set(project.hasProperty("agent"))
    }
}

tasks.register("printClasspath") {
    doLast {
        println(sourceSets.main.get().runtimeClasspath.asPath)
    }
}

// Print build info
gradle.projectsEvaluated {
    println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    println("  Klaat Build Configuration")
    println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    println("  Variant: $buildVariant")
    println("  App Name: ${application.applicationName}")
    println("  Version: ${project.version}")
    println("  Dev Build: $isDevBuild")
    println("  Snapshot Build: $isSnapshot")
    println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
}

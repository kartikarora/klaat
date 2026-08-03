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
val isReleaseTask = gradle.startParameter.taskNames.any { it.equals("buildRelease", ignoreCase = true) || it.equals("nativeRelease", ignoreCase = true) }
val buildVariant = if (isReleaseTask) "Release" else (project.findProperty("buildVariant")?.toString() ?: "dev")
val isDevBuild = buildVariant == "dev"
val isSnapshot = System.getenv("IS_SNAPSHOT") == "true"
// VERSION_NAME is calculated entirely in CI and passed as environment variable
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
    applicationDefaultJvmArgs = listOf(
        "-Dio.netty.noUnsafe=true",
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/sun.misc=ALL-UNNAMED"
    )
}

tasks.named<JavaExec>("run") {
    jvmArgs(
        "-Dio.netty.noUnsafe=true",
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/sun.misc=ALL-UNNAMED"
    )
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
}

tasks.register("buildRelease") {
    group = "build"
    description = "Build the release variant (JVM)"
    dependsOn("installDist")
}

// Native build tasks
tasks.register("nativeDev") {
    group = "build"
    description = "Build development native binary"
    dependsOn("nativeCompile")
}

tasks.register("nativeRelease") {
    group = "build"
    description = "Build release native binary"
    dependsOn("nativeCompile")
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
            buildArgs.add("--enable-native-access=ALL-UNNAMED")

            // Required for reflection used by libraries
            buildArgs.add("--initialize-at-build-time=kotlin,kotlinx,org.slf4j,ch.qos.logback,com.vladsch.flexmark,org.xml.sax")
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

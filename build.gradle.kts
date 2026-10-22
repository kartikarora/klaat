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
val isCi = System.getenv("CI") == "true"
val buildVariant = if (isReleaseTask || isCi) "Release" else (project.findProperty("buildVariant")?.toString() ?: "dev")
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
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.cio)
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

tasks.named<ProcessResources>("processResources") {
    inputs.dir(layout.projectDirectory.dir("skills"))
    inputs.dir(layout.projectDirectory.dir("examples"))
    // Package distributable skills directly from root skills/ directory
    from(layout.projectDirectory.dir("skills")) {
        into("skills")
        include("klaat-cli/**", "klaat-markdown-author/**", "klaat-gdocs-author/**")
    }
    // Package demo assets directly from examples/
    from(layout.projectDirectory.dir("examples")) {
        into("demo")
        include("feature-showcase.md", "images/klaat.png")
    }
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

val generateBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/source/buildConfig")
    inputs.property("versionName", versionName)
    inputs.property("buildVariant", buildVariant)
    inputs.property("isSnapshot", isSnapshot)
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

            // Debug symbols only enabled for dev builds (not for production)
            debug.set(isDevBuild)

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

tasks.register<JavaExec>("evalSkills") {
    group = "verification"
    description = "Evaluates AI agent skills autonomous invocation and adherence against Klaat compiler using live Gemini API"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("me.kartikarora.klaat.evals.SkillEvalRunnerKt")

    systemProperty("klaat.eval.docs.path", layout.projectDirectory.file("docs/docs/skills.html").asFile.absolutePath)
    systemProperty("klaat.eval.dataset.path", layout.projectDirectory.file("evals/dataset.json").asFile.absolutePath)
    systemProperty("klaat.eval.report.path", layout.buildDirectory.file("reports/evals/eval-report.json").get().asFile.absolutePath)

    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    })
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

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.dokka)
}

kotlin {
    explicitApi()
    jvmToolchain(21)

    // Public API dumps live in calculator-engine/api. Run updateKotlinAbi after an intended API change.
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()

    jvm()
    android {
        namespace = "dev.voir.calculator.engine"
        compileSdk = 37
        minSdk = 23
        // Runs commonTest on the JVM against the Android variant.
        withHostTest {}
    }
    iosArm64()
    iosSimulatorArm64()
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.voir.decimal)
            api(libs.kotlinx.coroutines)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}


dokka {
    moduleName.set("Calculator Engine")
    dokkaSourceSets.configureEach {
        // Module and package descriptions shown on the generated API reference pages.
        includes.from("Module.md")
    }
}

val isLocalPublish = gradle.startParameter.taskNames.any {
    it.contains("publishToMavenLocal", ignoreCase = true)
}

mavenPublishing {
    publishToMavenCentral()
    if (!isLocalPublish) {
        signAllPublications()
    }

    coordinates(
        groupId = "dev.voir",
        artifactId = "calculator-engine",
        version = project.version.toString()
    )

    pom {
        name.set("Calculator Engine - Kotlin Multiplatform")
        description.set("Simple Kotlin Multiplatform calculator engine powered by StateFlow and dev.voir:decimal.")
        url.set("https://github.com/VoirDev/calculator-engine-kmp/")

        licenses {
            license {
                name.set("Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("checksanity")
                name.set("Gary Bezruchko")
                email.set("hello@voir.dev")
                organization.set("VOIR")
                organizationUrl.set("https://voir.dev")
            }
        }

        scm {
            url.set("https://github.com/VoirDev/calculator-engine-kmp/")
            connection.set("scm:git:git://github.com/VoirDev/calculator-engine-kmp.git")
            developerConnection.set("scm:git:ssh://git@github.com/VoirDev/calculator-engine-kmp.git")
        }
    }
}

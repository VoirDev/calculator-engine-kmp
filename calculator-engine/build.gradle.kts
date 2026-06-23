plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    id("com.vanniktech.maven.publish") version "0.36.0"
}

kotlin {
    jvmToolchain(21)

    jvm()
    android {
        namespace = "dev.voir.calculator.engine"
        compileSdk = 37
        minSdk = 23
    }
    iosX64()
    iosArm64()
    macosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain = getByName("commonMain")
        val commonTest = getByName("commonTest")
        val jvmMain = getByName("jvmMain")
        val androidMain = getByName("androidMain")
        val iosX64Main = getByName("iosX64Main")
        val iosArm64Main = getByName("iosArm64Main")
        val iosSimulatorArm64Main = getByName("iosSimulatorArm64Main")
        val macosArm64Main = getByName("macosArm64Main")

        val jvmAndroidMain = create("jvmAndroidMain") {
            dependsOn(commonMain)
        }
        val appleMain = findByName("appleMain") ?: create("appleMain") {
            dependsOn(commonMain)
        }

        jvmMain.dependsOn(jvmAndroidMain)
        androidMain.dependsOn(jvmAndroidMain)
        iosX64Main.dependsOn(appleMain)
        iosArm64Main.dependsOn(appleMain)
        iosSimulatorArm64Main.dependsOn(appleMain)
        macosArm64Main.dependsOn(appleMain)

        commonMain.dependencies {
            api(libs.voir.decimal)
            api(libs.kotlinx.coroutines)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines)
        }
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

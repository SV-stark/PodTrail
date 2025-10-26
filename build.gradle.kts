// build.gradle.kts (Root Project)

plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
}

rootProject.layout.buildDirectory.set(file("build"))

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

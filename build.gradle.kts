// build.gradle.kts (Root Project)

plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
}

rootProject.layout.buildDirectory.set(file("build"))

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

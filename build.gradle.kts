// build.gradle.kts (Root Project)

plugins {
    id("com.android.application") version "9.0.0-rc02" apply false
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("com.google.devtools.ksp") version "2.2.0-2.0.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false
}

rootProject.layout.buildDirectory.set(file("build"))

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

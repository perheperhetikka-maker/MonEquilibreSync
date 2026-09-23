buildscript {
    dependencies {
        // AGP 9 has built-in Kotlin support. Keep the Kotlin runtime aligned
        // with the Compose compiler and serialization plugin.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}

plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10" apply false
}

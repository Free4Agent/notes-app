pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("multiplatform").version("1.9.22")
        kotlin("android").version("1.9.22")
        kotlin("plugin.serialization").version("1.9.22")
        id("com.android.application").version("8.2.0")
        id("com.android.library").version("8.2.0")
        id("app.cash.sqldelight").version("2.0.1")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "notes-app"
include(":shared")
include(":androidApp")

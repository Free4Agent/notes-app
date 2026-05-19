plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("app.cash.sqldelight")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    // iOS targets (future)
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Ktor for WebDAV
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-auth:2.3.7")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
                implementation("io.ktor:ktor-serialization-kotlinx-xml:2.3.7")
                
                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                
                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                
                // Markdown parsing
                implementation("org.jetbrains:markdown:0.6.1")
                
                // DI
                implementation("io.insert-koin:koin-core:3.5.3")
                
                // Logging
                implementation("co.touchlab:kermit:2.0.2")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:2.3.7")
                implementation("app.cash.sqldelight:android-driver:2.0.1")
            }
        }
        
        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }
    }
}

sqldelight {
    databases {
        create("NotesDatabase") {
            packageName.set("com.notes.app.database")
            
            // Migration verification
            verifyMigrations.set(true)
        }
    }
}

android {
    namespace = "com.notes.app.shared"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 26
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

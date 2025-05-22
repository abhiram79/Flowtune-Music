plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
}

kotlin {
    // Modern way to set JVM target
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
    
    // Ensures consistent JDK usage across builds
    jvmToolchain(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    // HTTP client
    implementation(libs.okhttp)
    
    // Logging interceptor (optional)
    implementation(libs.logging.interceptor)
    
    // Kotlin standard library
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // Add other dependencies as needed...
}

// Optional: Configure tasks for better build consistency
tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }
}
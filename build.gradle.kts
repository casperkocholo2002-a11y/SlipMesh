plugins {
    kotlin("jvm") version "2.4.20"
    id("com.android.application") version "9.4.0" apply false
}

group = "com.slipmesh"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test-junit5"))
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "failed", "skipped")
    }
}

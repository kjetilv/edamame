import org.gradle.api.JavaVersion.VERSION_21

plugins {
    java
    `jvm-test-suite`
}

group = "com.github.kjetilv"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    sourceCompatibility = VERSION_21
    targetCompatibility = VERSION_21

    modularity.inferModulePath.set(true)
}

@Suppress("UnstableApiUsage")
testing {
    this.suites.named<JvmTestSuite>("test") {
        useJUnitJupiter()
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.13.1")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.1")
}


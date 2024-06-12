plugins {
    eclipse
    `java-library`
    id("org.jetbrains.dokka") version "1.9.20"
    `maven-publish`
}

repositories {
    mavenCentral()
    mavenLocal() // This is to access the local Maven repository.
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    
    // common libraries
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")
    implementation("com.google.guava:guava:33.2.0-jre")
    
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
}

group = "com.metatrope"
version = "0.0.1"
description = "jact-core"
java.sourceCompatibility = JavaVersion.VERSION_21

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

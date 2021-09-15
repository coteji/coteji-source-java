plugins {
    kotlin("jvm") version "1.5.10"
    id("com.vanniktech.maven.publish") version "0.13.0"
    id("org.jetbrains.dokka") version "1.4.32"
}

version = "0.1.0"
group = "io.github.coteji"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.coteji:coteji-core:0.1.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.2")
    testImplementation("org.assertj:assertj-core:3.20.2")
    api("com.github.javaparser:javaparser-core:3.23.0")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
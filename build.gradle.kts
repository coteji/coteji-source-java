plugins {
    kotlin("jvm") version "1.5.31"
    id("com.vanniktech.maven.publish") version "0.13.0"
    id("org.jetbrains.dokka") version "1.4.32"
    jacoco
    id("io.gitlab.arturbosch.detekt") version "1.18.1"
}

version = "0.3.0"
group = "io.github.coteji"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.coteji:coteji-core:0.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("org.assertj:assertj-core:3.21.0")
    api("com.github.javaparser:javaparser-core:3.23.0")
}

jacoco {
    toolVersion = "0.8.7"
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        csv.required.set(false)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.95".toBigDecimal()
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    finalizedBy(tasks.jacocoTestReport)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false

    reports {
        html.enabled = true
        xml.enabled = true
        txt.enabled = true
        sarif.enabled = true
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "1.8"
}

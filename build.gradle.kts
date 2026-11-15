plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
    application
    id("org.jmailen.kotlinter")
    kotlin("plugin.serialization")
}

group = "me.varoa"
version = "1.0.0"

application {
    mainClass.set("me.varoa.ambil.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // HTTP client for Discord API calls
    implementation(Square.okHttp3)

    // JSON serialization
    implementation(KotlinX.serialization.json)

    // Selenium for site automation
    implementation("org.seleniumhq.selenium:selenium-java:_")
    implementation("io.github.bonigarcia:webdrivermanager:_")

    // Logging
    implementation("org.slf4j:slf4j-api:_")
    implementation("ch.qos.logback:logback-classic:_")

    // Kotlin stdlib
    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlinter {
    ignoreFormatFailures = false
    reporters = arrayOf("checkstyle", "plain")
}

tasks {
    shadowJar {
        archiveBaseName = "ambil"
        archiveVersion = ""
        archiveClassifier = ""
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
    }
}

kotlin {
    jvmToolchain(21)
}

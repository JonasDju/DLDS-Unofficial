plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.0"
}

group = "eu.bitflare"
version = "0.4-BETA"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("fr.mrmicky:fastboard:2.1.3")
    implementation("com.google.code.gson:gson:2.12.1")
}

tasks.shadowJar {
    relocate("fr.mrmicky.fastboard", "eu.bitflare.fastboard")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
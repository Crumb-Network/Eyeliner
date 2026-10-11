plugins {
    id("java")
}

group = "net.kalbskinder"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.minestom:minestom:2026.08.28-26.2")
    annotationProcessor("org.projectlombok:lombok:1.18.48")
    implementation("org.slf4j:slf4j-api:1.7.25")
}

tasks.test {
    useJUnitPlatform()
}
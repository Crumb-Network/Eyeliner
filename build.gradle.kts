plugins {
    id("java")
    id("com.vanniktech.maven.publish") version "0.37.0"
}

group = "net.kalbskinder"
// The release workflow passes -PreleaseVersion=<tag without leading "v">
version = providers.gradleProperty("releaseVersion").getOrElse("0.0.1-SNAPSHOT")

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.minestom:minestom:2026.08.28-26.2")
    annotationProcessor("org.projectlombok:lombok:1.18.48")
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("net.kyori:adventure-text-minimessage:5.2.0")
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    // Only sign when a key is available (CI), so local publishToMavenLocal still works
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    coordinates(group.toString(), "eyeliner", version.toString())

    pom {
        name.set("Eyeliner")
        description.set("A Minestom library for building in-world user interfaces from HTML-like markup.")
        inceptionYear.set("2026")
        url.set("https://github.com/Crumb-Network/Eyeliner")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("Kalbskinder")
                name.set("Kalbskinder")
                url.set("https://github.com/Kalbskinder")
            }
        }
        scm {
            url.set("https://github.com/Crumb-Network/Eyeliner")
            connection.set("scm:git:git://github.com/Crumb-Network/Eyeliner.git")
            developerConnection.set("scm:git:ssh://git@github.com/Crumb-Network/Eyeliner.git")
        }
    }
}

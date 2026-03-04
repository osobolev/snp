plugins {
    id("java")
}

version = "1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

dependencies {
    implementation("org.jsoup:jsoup:1.22.1")
}

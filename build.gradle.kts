plugins {
    id("java")
}

version = "1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "UTF-8"
    options.release.set(11)
}

dependencies {
    implementation("org.jsoup:jsoup:1.22.1")
    implementation("io.github.osobolev:small-json:1.4")
    implementation("org.owasp.encoder:encoder:1.4.0")
}

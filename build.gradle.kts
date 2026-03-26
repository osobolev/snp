plugins {
    id("java")
    id("com.github.ben-manes.versions") version "0.53.0"
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
    implementation("io.github.osobolev:small-json:1.4")
    implementation("org.owasp.encoder:encoder:1.4.0")
}

tasks.jar {
    manifest {
        attributes(
            "Class-Path" to configurations.runtimeClasspath.map { conf -> conf.files.map { f -> f.name }.sorted().joinToString(" ") },
            "Main-Class" to "snp.SNPBot"
        )
    }
}

tasks.clean {
    delete("$rootDir/distr")
}

tasks.register("distr", Copy::class) {
    from(configurations.runtimeClasspath)
    from(tasks.jar)
    into("$rootDir/distr")
}

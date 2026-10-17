plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.6.1"
}

repositories {
    mavenCentral()
    maven("https://maven.hapily.me/snapshots")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.hapily04:skript-minestom:1.0.0-alpha.48")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.ow2.asm:asm-tree:9.8")
    implementation("org.ow2.asm:asm-analysis:9.8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.mockito:mockito-core:5.15.2")
    testImplementation("net.bytebuddy:byte-buddy:1.18.12")
    testImplementation("net.bytebuddy:byte-buddy-agent:1.18.12")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations.testImplementation {
    extendsFrom(configurations.compileOnly.get())
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    test {
        useJUnitPlatform()
        // The server fat jar bundles older ASM/Byte Buddy; use our explicit versions first.
        classpath = files(classpath.files.sortedBy { if (it.name.startsWith("skript-minestom-")) 1 else 0 })
    }
    shadowJar {
        relocate("org.objectweb.asm", "org.skdocs.skdocstool.internal.asm")
        archiveClassifier.set("")
        destinationDirectory.set(layout.buildDirectory.dir("libs"))
    }

    jar {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}

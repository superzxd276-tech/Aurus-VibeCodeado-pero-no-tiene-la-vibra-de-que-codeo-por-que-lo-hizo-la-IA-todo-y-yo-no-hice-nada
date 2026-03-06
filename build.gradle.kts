plugins {
    `java`
    // El plugin Shadow equivale a maven-shade-plugin
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.fendrixx"
version = "1.0.2-BETA"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()

    // Repositorio de Paper (Reemplaza a spigotmc-repo)
    maven("https://repo.papermc.io/repository/maven-public/")

    // Repositorio de PlaceholderAPI
    maven("https://repo.helpch.at/releases/")

    // Repositorio de CodeMC (PacketEvents)
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    // CAMBIO: Spigot -> Paper API
    // Paper 1.20.1 ya incluye Adventure, MiniMessage y Gson.
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")

    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.12.2")

    // Exp4j
    implementation("net.objecthunter:exp4j:0.4.8")

    // PacketEvents
    implementation("com.github.retrooper:packetevents-spigot:2.11.2")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    shadowJar {
        // Configuramos las relocations igual que en tu pom.xml
        relocate("net.objecthunter", "com.fendrixx.aurus.libs.exp4j")
        relocate("com.github.retrooper.packetevents", "com.fendrixx.aurus.libs.packetevents.api")
        relocate("io.github.retrooper.packetevents", "com.fendrixx.aurus.libs.packetevents.impl")

        // Evita que el archivo se llame "Aurus-1.0.0-BETA-all.jar", dejándolo solo como el nombre original
        archiveClassifier.set("")
    }

    // Hacemos que al ejecutar build, se genere el shadow jar
    build {
        dependsOn(shadowJar)
    }
}

plugins {
    id("nebula.release") version "13.2.1" apply false
    id("com.bmuschko.docker-remote-api") version "6.6.1" apply false
    id("com.github.johnrengelman.shadow") version "6.0.0" apply false
}

group = "lit.fass"

allprojects {
    apply(plugin = "nebula.release")

    repositories {
        mavenCentral()
    }
}

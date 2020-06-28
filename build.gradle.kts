plugins {
    id("nebula.release") version "13.2.1" apply false
    id("com.bmuschko.docker-remote-api") version "6.1.4" apply false
}

group = "lit.fass"

allprojects {
    apply(plugin = "nebula.release")

    repositories {
        mavenCentral()
    }
}

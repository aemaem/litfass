plugins {
    kotlin("jvm") version "1.3.61" apply false
    kotlin("plugin.spring") version "1.3.61" apply false
    id("org.springframework.boot") version "2.2.5.RELEASE" apply false
    id("io.spring.dependency-management") version "1.0.9.RELEASE" apply false
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

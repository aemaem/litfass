import com.bmuschko.gradle.docker.DockerExtension
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import org.gradle.api.JavaVersion.VERSION_11
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.dsl.SpringBootExtension
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    kotlin("plugin.spring")
    distribution
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.bmuschko.docker-remote-api")
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot.experimental:spring-boot-bom-r2dbc:0.1.0.M3")
    }
}

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    //implementation("org.springframework.boot.experimental:spring-boot-starter-data-r2dbc")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.9")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.9")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.9")
    implementation("org.apache.httpcomponents:httpclient:4.5.11")
    implementation("org.apache.commons:commons-lang3:3.8.1")
    implementation("com.google.guava:guava:27.0.1-jre")
    implementation("com.cronutils:cron-utils:8.0.0")
    implementation("org.quartz-scheduler:quartz:2.3.0")
    implementation("org.quartz-scheduler:quartz-jobs:2.3.0")
    implementation("org.jooq:jooq:3.11.7")
    implementation("org.postgresql:postgresql:42.2.5")
    implementation("org.codehaus.groovy:groovy:2.5.5")
    implementation("org.codehaus.groovy:groovy-jsr223:2.5.5")
    implementation("org.codehaus.groovy:groovy-json:2.5.5")
    implementation("org.codehaus.groovy:groovy-xml:2.5.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.security:spring-security-test")
    //testImplementation("org.springframework.boot.experimental:spring-boot-test-autoconfigure-r2dbc")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    testImplementation("io.mockk:mockk:1.9.3")
    testImplementation("org.awaitility:awaitility:3.1.3")
    testImplementation("org.awaitility:awaitility-kotlin:3.1.3")
}

java.sourceCompatibility = VERSION_11
java.targetCompatibility = VERSION_11

sourceSets.create("infra") {
    java.srcDir("src/infra/docker")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = VERSION_11.toString()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
object UnitTest
object IntegrationTest
tasks.register<Test>("unitTest") {
    useJUnitPlatform {
        filter {
            includeTags(UnitTest::class.java.simpleName)
        }
    }
}
tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        filter {
            includeTags(IntegrationTest::class.java.simpleName)
        }
    }
}

configure<SpringBootExtension> {
    buildInfo()
    mainClassName = "lit.fass.litfass.server.ServerApplicationKt"
}
tasks.withType<BootJar> {
    enabled = false
}
tasks.withType<Jar> {
    enabled = true
    archiveBaseName.set(project.name)
    manifest {
        attributes["Implementation-Title"] = "LITFASS"
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = "lit.fass.litfass.server.ServerApplicationKt"
    }
}
tasks.create("allJar", Jar::class) {
    archiveClassifier.set("all")
    manifest {
        attributes["Implementation-Title"] = "LITFASS"
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = "lit.fass.litfass.server.ServerApplicationKt"
    }
    from(project.configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.named<Jar>("jar").get())
}
tasks.assembleDist.get().dependsOn(tasks["allJar"])

distributions {
    main {
        contents {
            val runFile = File.createTempFile("run", ".sh")
            runFile.writeText("""!/bin/sh\njava -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -server -ea -classpath "./*:./lib/*" lit.fass.litfass.server.ServerApplicationKt""")
            runFile.setExecutable(true)
            runFile.deleteOnExit()
            from(runFile) {
                rename { "run.sh" }
            }
            from(tasks.jar)
            into("lib") {
                from(configurations.runtimeClasspath)
            }
        }
    }
}

configure<DockerExtension> {
    registryCredentials {
        username.set(rootProject.extra["dockerHubUsername"] as String?)
        password.set(rootProject.extra["dockerHubPassword"] as String?)
        email.set(rootProject.extra["dockerHubEmail"] as String?)
    }
}
tasks.create("prepareImage", Copy::class) {
    dependsOn(tasks.distZip)
    from(zipTree(tasks.named<Zip>("distZip").get().archiveFile))

    include("${project.name}-${project.version}/lib/*")
    include("${project.name}-${project.version}/${project.name}-${project.version}.jar")
    from(sourceSets["infra"].allSource.srcDirs)
    include("Dockerfile")
    into("${project.buildDir}/docker/")
    doLast {
        ant.withGroovyBuilder {
            "move"(
                "file" to "${project.buildDir}/docker/${project.name}-${project.version}/lib/",
                "todir" to "${project.buildDir}/docker/"
            )
            "move"(
                "file" to "${project.buildDir}/docker/${project.name}-${project.version}/${project.name}-${project.version}.jar",
                "tofile" to "${project.buildDir}/docker/app.jar"
            )
            "delete"(
                "dir" to "${project.buildDir}/docker/${project.name}-${project.version}/",
                "quiet" to true
            )
        }
    }
}
tasks.create("buildImage", DockerBuildImage::class) {
    dependsOn(tasks.named("prepareImage"))
    inputDir.set(file("${buildDir}/docker/"))
    images.add(
        "${rootProject.extra["dockerHubUsername"]}/${rootProject.name}:${project.version.toString().replace(
            "\\+",
            "."
        )}"
    )
}
tasks.create("pushImage", DockerPushImage::class) {
    dependsOn(tasks.named("buildImage"))
    images.add(
        "${rootProject.extra["dockerHubUsername"]}/${rootProject.name}:${project.version.toString().replace(
            "\\+",
            "."
        )}"
    )
}

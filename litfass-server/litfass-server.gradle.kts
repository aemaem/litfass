import com.bmuschko.gradle.docker.DockerExtension
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import org.gradle.api.JavaVersion.VERSION_11
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
    `java-library`
    distribution
    id("com.github.johnrengelman.shadow")
    id("com.bmuschko.docker-remote-api")
}

repositories {
    jcenter()
}

dependencies {
    val scalaVersion = "2.13"
    val versions = mapOf(
        "kotlin" to "1.4.21",
        "scala" to "${scalaVersion}.2",
        "akka" to "2.6.10",
        "akka-http" to "10.2.1",
        "jackson" to "2.11.2",
        "shiro" to "1.5.2",
        "groovy" to "2.5.11",
        "junit" to "5.6.2",
        "testcontainers" to "1.14.3"
    )

    implementation("org.jetbrains.kotlin:kotlin-reflect:${versions["kotlin"]}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${versions["kotlin"]}")

    implementation("org.scala-lang:scala-library:${versions["scala"]}")
    implementation("com.typesafe.akka:akka-actor-typed_${scalaVersion}:${versions["akka"]}")
    implementation("com.typesafe.akka:akka-cluster-typed_${scalaVersion}:${versions["akka"]}")
    implementation("com.typesafe.akka:akka-discovery_${scalaVersion}:${versions["akka"]}")
    implementation("com.lightbend.akka.management:akka-management-cluster-bootstrap_${scalaVersion}:1.0.9")
    implementation("com.typesafe.akka:akka-stream_${scalaVersion}:${versions["akka"]}")
    implementation("com.typesafe.akka:akka-http_${scalaVersion}:${versions["akka-http"]}")
    implementation("com.typesafe.akka:akka-http-jackson_${scalaVersion}:${versions["akka-http"]}")
    implementation("com.typesafe.akka:akka-http-spray-json_${scalaVersion}:${versions["akka-http"]}")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.apache.shiro:shiro-core:${versions["shiro"]}")
    implementation("org.apache.shiro:shiro-web:${versions["shiro"]}")
    implementation("org.apache.commons:commons-lang3:3.10")
    implementation("com.google.guava:guava:29.0-jre")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${versions["jackson"]}")
    implementation("com.fasterxml.jackson.core:jackson-databind:${versions["jackson"]}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${versions["jackson"]}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${versions["jackson"]}")
    implementation("com.github.ben-manes.caffeine:caffeine:2.8.5")
    implementation("com.cronutils:cron-utils:8.0.0")
    implementation("org.quartz-scheduler:quartz:2.3.0")
    implementation("org.quartz-scheduler:quartz-jobs:2.3.0")
    implementation("org.jooq:jooq:3.11.12")
    implementation("org.postgresql:postgresql:42.2.5")
    implementation("org.apache.httpcomponents:httpclient:4.5.11")
    implementation("org.codehaus.groovy:groovy:${versions["groovy"]}")
    implementation("org.codehaus.groovy:groovy-jsr223:${versions["groovy"]}")
    implementation("org.codehaus.groovy:groovy-json:${versions["groovy"]}")
    implementation("org.codehaus.groovy:groovy-xml:${versions["groovy"]}")


    testImplementation("com.typesafe.akka:akka-actor-testkit-typed_${scalaVersion}:${versions["akka"]}")
    testImplementation("com.typesafe.akka:akka-http-testkit_${scalaVersion}:${versions["akka-http"]}")
    testImplementation("junit:junit:4.13")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:${versions["junit"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${versions["junit"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${versions["junit"]}")
    testImplementation("org.assertj:assertj-core:3.15.0")
    testImplementation("org.awaitility:awaitility:4.0.2")
    testImplementation("io.mockk:mockk:1.10.0")
    testImplementation("org.springframework.boot:spring-boot-test:2.3.3.RELEASE")
    testImplementation("org.testcontainers:junit-jupiter:${versions["testcontainers"]}")
    testImplementation("org.testcontainers:postgresql:${versions["testcontainers"]}")
    testImplementation("com.github.kittinunf.fuel:fuel:2.2.3")
    testImplementation("com.github.kittinunf.fuel:fuel-jackson:2.2.3")
}

java.sourceCompatibility = VERSION_11
java.targetCompatibility = VERSION_11

sourceSets.create("infra") {
    java.srcDir("src/infra/docker")
}


tasks.withType<Test> {
    useJUnitPlatform {
        filter {
            excludeTags(ApiTest::class.java.simpleName)
        }
    }
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor?) {}
        override fun afterSuite(suite: TestDescriptor?, result: TestResult?) {}

        override fun beforeTest(testDescriptor: TestDescriptor?) {
            logger.quiet(testDescriptor.toString())
        }

        override fun afterTest(testDescriptor: TestDescriptor?, result: TestResult?) {}
    })
}
object UnitTest
object IntegrationTest
object ApiTest
tasks.register<Test>("unitTest") {
    useJUnitPlatform {
        filter {
            excludeTags(IntegrationTest::class.java.simpleName, ApiTest::class.java.simpleName)
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
tasks.register<Test>("apiTest") {
    useJUnitPlatform {
        filter {
            includeTags(ApiTest::class.java.simpleName)
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = VERSION_11.majorVersion
    }
}
tasks.withType<Jar> {
    enabled = true
    archiveBaseName.set(project.name)
    manifest {
        attributes["Implementation-Title"] = "LITFASS"
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = "lit.fass.server.LitfassApplication"
    }
}
tasks.create("allJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    archiveClassifier.set("all")
    manifest {
        attributes["Implementation-Title"] = "LITFASS"
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = "lit.fass.server.LitfassApplication"
    }
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.AppendingTransformer::class.java) {
        resource = "reference.conf"
    }
    with(tasks.named<Jar>("jar").get())
}
tasks.assembleDist.get().dependsOn(tasks["allJar"])

distributions {
    main {
        contents {
            val runFile = File.createTempFile("run", ".sh")
            runFile.writeText("#!/bin/sh\njava -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -server -ea -classpath \"./*:./lib/*\" lit.fass.server.LitfassApplication")
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
        "${rootProject.extra["dockerHubUsername"]}/${rootProject.name}:${project.version.toString().replace("+",".")}"
    )
    images.add(
        "${rootProject.extra["dockerHubUsername"]}/${rootProject.name}:latest"
    )
}
tasks.create("pushImage", DockerPushImage::class) {
    dependsOn(tasks.named("buildImage"))
    images.add(
        "${rootProject.extra["dockerHubUsername"]}/${rootProject.name}:${project.version.toString().replace("+",".")}"
    )
}

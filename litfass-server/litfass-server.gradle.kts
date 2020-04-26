import com.bmuschko.gradle.docker.DockerExtension
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import org.gradle.api.JavaVersion.VERSION_11
import org.gradle.jvm.tasks.Jar

plugins {
    scala
    `java-library`
    distribution
    id("com.bmuschko.docker-remote-api")
}

repositories {
    jcenter()
}

dependencies {
    implementation("org.scala-lang:scala-library:2.13.2")
    implementation("com.typesafe.akka:akka-actor-typed_2.13:2.6.4")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    testImplementation("org.scalatest:scalatest_2.13:3.1.1")
    testImplementation("org.scalatestplus:scalatestplus-junit_2.13:1.0.0-M2")
    testImplementation("com.typesafe.akka:akka-actor-testkit-typed_2.13:2.6.4")
    testImplementation("org.assertj:assertj-core:3.15.0")
    testImplementation("org.awaitility:awaitility:4.0.2")
}

java.sourceCompatibility = VERSION_11
java.targetCompatibility = VERSION_11

sourceSets.create("infra") {
    java.srcDir("src/infra/docker")
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
tasks.create("allJar", Jar::class) {
    archiveClassifier.set("all")
    manifest {
        attributes["Implementation-Title"] = "LITFASS"
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = "lit.fass.server.LitfassApplication"
    }
    from(project.configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.named<Jar>("jar").get())
}
tasks.assembleDist.get().dependsOn(tasks["allJar"])

distributions {
    main {
        contents {
            val runFile = File.createTempFile("run", ".sh")
            runFile.writeText("""!/bin/sh\njava -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -server -ea -classpath "./*:./lib/*" lit.fass.server.LitfassApplication""")
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

gradle.afterProject {
    rootProject.extra["dockerHubUsername"] = System.getenv("DOCKER_HUB_USERNAME")
        ?: if (extra.has("DOCKER_HUB_USERNAME")) extra["DOCKER_HUB_USERNAME"] else null
    rootProject.extra["dockerHubPassword"] = System.getenv("DOCKER_HUB_PASSWORD")
        ?: if (extra.has("DOCKER_HUB_PASSWORD")) extra["DOCKER_HUB_PASSWORD"] else null
    rootProject.extra["dockerHubEmail"] = System.getenv("DOCKER_HUB_EMAIL")
        ?: if (extra.has("DOCKER_HUB_EMAIL")) extra["DOCKER_HUB_EMAIL"] else null
}

rootProject.name = "litfass"

include("litfass-server")

rootProject.children.forEach { subproject ->
    val dir = subproject.projectDir.absolutePath
    val name = subproject.name.split("/").last()
    val buildFileName = "${subproject.name}.gradle.kts"
    assert(file("$dir/$buildFileName").exists()) {
        "File ${subproject.projectDir}/${buildFileName} does not exist"
    }
    subproject.name = name
    subproject.buildFileName = buildFileName
}

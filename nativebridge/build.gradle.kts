plugins {
    `java-library`
}

dependencies {
    api(project(":core"))

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    from(rootProject.layout.projectDirectory.dir("native/build")) {
        include("seedatlas.dll")
        into("native/windows-x86_64")
    }
}

tasks.test {
    systemProperty(
        "java.library.path",
        rootProject.layout.projectDirectory.dir("native/build").asFile.absolutePath
    )
}

plugins {
    application
}

dependencies {
    implementation(project(":core"))
    implementation(project(":engine"))
    implementation(project(":nativebridge"))
    implementation(project(":ingest"))
    implementation(project(":testkit"))
    implementation("info.picocli:picocli:4.7.6")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "dev.seedatlas.cli.SeedAtlasCli"
    applicationName = "seedatlas"
}

tasks.register<JavaExec>("runCli") {
    group = "application"
    description = "Run the SeedAtlas CLI with the given arguments."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dev.seedatlas.cli.SeedAtlasCli")
}

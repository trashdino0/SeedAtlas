plugins {
    `java-library`
}

dependencies {
    api(project(":core"))
    api(project(":engine"))
    api(project(":nativebridge"))

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

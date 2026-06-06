plugins {
    id("java")
    `java-library`
}

group = "com.lahuca"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("com.google.code.gson:gson-extras:2.13.2-rc1")
    implementation("net.kyori:adventure-api:4.24.0")
    api(project(":"))
    implementation("net.kyori:adventure-text-serializer-gson:4.19.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    from(project(":").sourceSets["main"].output)
}

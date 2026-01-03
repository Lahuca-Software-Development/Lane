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
    compileOnly("com.google.code.gson:gson:2.10.1")
    api(project(":"))
    compileOnly("net.kyori:adventure-api:4.19.0")
    compileOnly("net.kyori:adventure-text-serializer-gson:4.19.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    from(project(":").sourceSets["main"].output)
}

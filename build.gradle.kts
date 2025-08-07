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
    implementation("net.kyori:adventure-api:4.24.0")
}

tasks.test {
    useJUnitPlatform()
}
plugins {
    id("java")
}

group = "com.lahuca.laneinstancespigot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(project(":"))
    implementation(project(":LaneInstance"))
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.google.code.gson:gson:2.10.1")
}

tasks.test {
    useJUnitPlatform()
}
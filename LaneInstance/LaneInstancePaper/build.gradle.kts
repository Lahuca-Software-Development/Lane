plugins {
    id("java")
    `java-library`
}

group = "com.lahuca.laneinstancepaper"
version = "1.0-SNAPSHOT"

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    compileOnly("com.google.code.gson:gson:2.10.1")
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    api(project(":"))
    api(project(":LaneInstance"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    from(project(":").sourceSets["main"].output)
    from(project(":LaneInstance").sourceSets["main"].output)
}

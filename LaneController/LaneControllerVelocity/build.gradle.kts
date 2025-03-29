plugins {
    id("java")
    `java-library`
    id("com.gradleup.shadow") version "9.0.0-beta11"
}

group = "com.lahuca"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(project(":LaneController"))
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    api(project(":"))
    api(project(":LaneController"))
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("com.mysql:mysql-connector-j:9.2.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    from(project(":").sourceSets["main"].output)
    from(project(":LaneController").sourceSets["main"].output)
}

tasks.shadowJar {
    dependencies {
        include(dependency(":"))
        include(dependency(":LaneController"))
        include(dependency("com.zaxxer:HikariCP:6.3.0"))
        include(dependency("com.mysql:mysql-connector-j:9.2.0"))
    }
}
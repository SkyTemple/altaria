plugins {
    id("java")
    application
}

group = "org.skytemple"
version = "1.0.0"

application {
    mainClass.set("org.skytemple.altaria.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.javacord:javacord:3.8.0")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("com.mysql:mysql-connector-j:8.2.0")
}

tasks.test {
    useJUnitPlatform()
}
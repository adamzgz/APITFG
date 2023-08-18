val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.8.21"
    id("io.ktor.plugin") version "2.3.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0-Beta"


}

group = "com.example"
version = "0.0.1"
application {
    mainClass.set("com.example.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.ryanharter.ktor:ktor-moshi:1.0.1")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.20-RC")
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.jetbrains.exposed:exposed-java-time:0.41.1")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-client-auth-jvm:2.3.0")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation ("io.ktor:ktor-serialization:1.4.1")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")








}
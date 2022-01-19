import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "me.duncte123"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.2.10")
    implementation("net.dv8tion:JDA:5.0.0-alpha.4")
    implementation("com.github.KittyBot-Org:Lavalink-Client:v1.0.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

application {
    mainClass.set("MainKt")
}
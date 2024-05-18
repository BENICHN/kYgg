import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    application
}

kotlin {
    jvmToolchain(9)
}

group = "fr.benichn"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    // https://mvnrepository.com/artifact/org.jsoup/jsoup
    implementation("org.jsoup:jsoup:1.16.1")
    // https://mvnrepository.com/artifact/io.ktor/ktor-server-netty
    implementation("io.ktor:ktor-server-netty:2.3.3")
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.10.1")

}

// val compileKotlin: KotlinCompile by tasks
// compileKotlin.compilerOptions {
//     freeCompilerArgs.add("-Xdebug") // !
// }

application {
    // Define the main class for the application.
    mainClass.set("fr.benichn.kygg.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest.attributes["Main-Class"] = application.mainClass
    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree) // OR .map { zipTree(it) }
    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
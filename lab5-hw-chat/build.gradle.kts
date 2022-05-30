import com.google.protobuf.gradle.*

val grpcVersion = "1.46.0"
val protobufVersion = "0.8.18"
val protocVersion = "3.20.1"
val springVersion = "5.3.20"
val springBootVersion = "2.6.7"

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.18")
    }
}

plugins {
    java
    idea
    id("com.google.protobuf") version "0.8.18"
    id("org.springframework.boot") version "2.6.7"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-services:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")

    implementation("com.google.protobuf:protobuf-java:$protocVersion")

    implementation("org.springframework.boot:spring-boot-starter:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-actuator:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.getByName<JavaCompile>("compileJava") {
    inputs.files(tasks.named("processResources"))
}

protobuf {
    generatedFilesBaseDir = "${project.buildDir}/generated/source/proto"

    protoc {
        artifact = "com.google.protobuf:protoc:$protocVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}

idea {
    module {
        generatedSourceDirs.addAll(listOf(
            file("${protobuf.protobuf.generatedFilesBaseDir}/main/grpc"),
            file("${protobuf.protobuf.generatedFilesBaseDir}/main/java")
        ))
    }
}
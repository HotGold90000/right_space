plugins {
    kotlin("jvm") version "1.9.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}

// 定义一个可执行任务运行 UpdateJson.kt
tasks.register<JavaExec>("updateJson") {
    group = "app"
    description = "Run UpdateJson.kt to update data.json"
    mainClass.set("UpdateJsonKt")       // 注意类名是文件名 + Kt
    classpath = sourceSets["main"].runtimeClasspath
}



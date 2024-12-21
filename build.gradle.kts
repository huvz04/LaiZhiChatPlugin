plugins {
    val kotlinVersion = "2.0.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("net.mamoe.mirai-console") version "2.16.0"

}
val kotlinVersion = "2.0.0"
group = "LaiZhi.Chat.Repeater"
version = "0.5.1"
val exposedVersion: String by project
repositories {
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public") // 阿里云国内代理仓库
}
dependencies {
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jodatime:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")

    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-money:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
    implementation("io.ktor:ktor-client-okhttp:2.2.4")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.8.4")
}
mirai {
    jvmTarget = JavaVersion.VERSION_17
}
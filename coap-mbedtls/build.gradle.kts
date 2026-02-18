plugins {
    id("java-library")
}

description = "coap-mbedtls"

dependencies {
    api(project(":coap-core"))
    api("io.github.open-coap:kotlin-mbedtls:1.33.1")
    api("io.github.open-coap:kotlin-mbedtls-netty:1.33.1")

    testImplementation(project(":coap-netty"))

    testImplementation(testFixtures(project(":coap-core")))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testImplementation("io.netty:netty-transport-native-epoll:4.2.6.Final:linux-x86_64")
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation("org.awaitility:awaitility:4.3.0")
}


tasks {

    register<Copy>("extractNativeLibs") {
        from(configurations.runtimeClasspath.get().filter { it.name.contains("mbedtls-lib") }.map { zipTree(it) })
        include("win32-x86-64/**")
        into(layout.buildDirectory.dir("native-libs"))
    }


    test {
        if (System.getProperty("os.name").lowercase().contains("windows")) {
            // On Windows, JNA cannot automatically load native libraries from JAR when they have dependencies on each other,
            // so we extract them and set the path explicitly.
            dependsOn("extractNativeLibs")
            systemProperty("jna.library.path", layout.buildDirectory.dir("native-libs/win32-x86-64").get().asFile.absolutePath)
        }
    }
}

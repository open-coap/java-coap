plugins {
    id("java-library")
    id("me.champeau.jmh") version "0.7.2"
}

description = "coap-netty"

dependencies {
    api(project(":coap-core"))
    api("io.netty:netty-handler:4.1.112.Final")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")

    testImplementation(testFixtures(project(":coap-core")))
    testImplementation("ch.qos.logback:logback-classic:1.3.14")
    testImplementation("io.netty:netty-transport-native-epoll:4.1.112.Final:linux-x86_64")

    jmhImplementation("io.netty:netty-all:4.1.112.Final")
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-bytecode:1.37")
}

tasks {
    named("pmdJmh").get().enabled = false
    named("spotbugsJmh").get().enabled = false
}

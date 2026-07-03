plugins {
    id("java-library")
    id("me.champeau.jmh") version "0.7.3"
}

description = "coap-netty"

dependencies {
    api(project(":coap-core"))
    api("io.netty:netty-handler:4.2.15.Final")

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.1")

    testImplementation(testFixtures(project(":coap-core")))
    testImplementation("ch.qos.logback:logback-classic:1.5.37")
    testImplementation("io.netty:netty-transport-native-epoll:4.2.15.Final:linux-x86_64")

    jmhImplementation("io.netty:netty-all:4.2.15.Final")
    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-bytecode:1.37")
}

tasks {
    named("pmdJmh").get().enabled = false
    named("spotbugsJmh").get().enabled = false
}

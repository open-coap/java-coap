plugins {
    id("java-library")
    id("me.champeau.jmh").version("0.7.0")
}

description = "coap-netty"

dependencies {
    api(project(":coap-core"))
    api("io.netty:netty-handler:4.1.93.Final")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")

    testImplementation(testFixtures(project(":coap-core")))
    testImplementation("ch.qos.logback:logback-classic:1.3.5")

    jmhImplementation("io.netty:netty-all:4.1.93.Final")
}

tasks {
    named("pmdJmh").get().enabled = false
    named("spotbugsJmh").get().enabled = false
}

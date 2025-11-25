plugins {
    id("java-library")
}

description = "coap-mbedtls"

dependencies {
    api(project(":coap-core"))
    api("io.github.open-coap:kotlin-mbedtls:1.32.2")
    api("io.github.open-coap:kotlin-mbedtls-netty:1.32.2")

    testImplementation(project(":coap-netty"))

    testImplementation(testFixtures(project(":coap-core")))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testImplementation("io.netty:netty-transport-native-epoll:4.2.6.Final:linux-x86_64")
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation("org.awaitility:awaitility:4.3.0")
}

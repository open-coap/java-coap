plugins {
    id("java-library")
}

description = "coap-mbedtls"

dependencies {
    api(project(":coap-core"))
    api("io.github.open-coap:kotlin-mbedtls:1.30.0")
    api("io.github.open-coap:kotlin-mbedtls-netty:1.30.0")

    testImplementation(project(":coap-netty"))

    testImplementation(testFixtures(project(":coap-core")))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.0")
    testImplementation("io.netty:netty-transport-native-epoll:4.2.1.Final:linux-x86_64")
    testImplementation("ch.qos.logback:logback-classic:1.3.15")
    testImplementation("org.awaitility:awaitility:4.3.0")
}

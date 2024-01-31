plugins {
    id("java-library")
}

description = "coap-mbedtls"

dependencies {
    api(project(":coap-core"))
    api("io.github.open-coap:kotlin-mbedtls:1.21.0")

    testImplementation(project(":coap-netty"))
    testImplementation("io.github.open-coap:kotlin-mbedtls-netty:1.21.0")

    testImplementation(testFixtures(project(":coap-core")))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("ch.qos.logback:logback-classic:1.3.5")
}

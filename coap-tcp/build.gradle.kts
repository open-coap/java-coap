plugins {
    id("java-library")
}

description = "coap-tcp"

dependencies {
    api(project(":coap-core"))
    api("org.slf4j:slf4j-api:2.0.17")

    testImplementation(testFixtures(project(":coap-core")))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation("org.mockito:mockito-core:5.19.0")
    testImplementation("org.assertj:assertj-core:3.27.4")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:4.1")
    testImplementation("org.awaitility:awaitility:4.3.0")
}

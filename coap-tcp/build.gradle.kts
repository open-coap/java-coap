plugins {
    id("java-library")
}

description = "coap-tcp"

dependencies {
    api(project(":coap-core"))
    api("org.slf4j:slf4j-api:2.0.13")

    testImplementation(testFixtures(project(":coap-core")))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
    testImplementation("ch.qos.logback:logback-classic:1.3.14")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.16.1")
    testImplementation("org.awaitility:awaitility:4.2.1")
}

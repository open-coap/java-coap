plugins {
    id("java-library")
}

description = "coap-tcp"

dependencies {
    api(project(":coap-core"))
    api("org.slf4j:slf4j-api:2.0.18")

    testImplementation(testFixtures(project(":coap-core")))

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.3")
    testImplementation("ch.qos.logback:logback-classic:1.6.3")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:4.5.2")
    testImplementation("org.awaitility:awaitility:4.3.0")
}

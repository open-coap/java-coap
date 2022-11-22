plugins {
    id("java-library")
    id("java-test-fixtures")
}

description = "coap-core"

dependencies {
    api("org.slf4j:slf4j-api:2.0.0")

    testFixturesApi("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testFixturesApi("org.assertj:assertj-core:3.23.1")
    testFixturesApi("org.awaitility:awaitility:4.2.0")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    testImplementation("ch.qos.logback:logback-classic:1.4.5")
    testImplementation("org.mockito:mockito-core:4.7.0")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.10.1")
    testImplementation("io.github.artsok:rerunner-jupiter:2.1.6")
}

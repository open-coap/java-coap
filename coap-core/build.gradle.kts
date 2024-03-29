plugins {
    id("java-library")
    id("java-test-fixtures")
    id("me.champeau.jmh").version("0.7.0")
}

description = "coap-core"

dependencies {
    api("org.slf4j:slf4j-api:2.0.11")

    testFixturesApi("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testFixturesApi("org.assertj:assertj-core:3.25.2")
    testFixturesApi("org.awaitility:awaitility:4.2.0")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("ch.qos.logback:logback-classic:1.3.5")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.15.6")
    testImplementation("io.github.artsok:rerunner-jupiter:2.1.6")
}

tasks {
    named("pmdTestFixtures").get().enabled = false
    named("pmdJmh").get().enabled = false
    named("spotbugsJmh").get().enabled = false
}

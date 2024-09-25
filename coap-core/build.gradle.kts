plugins {
    id("java-library")
    id("java-test-fixtures")
    id("me.champeau.jmh") version "0.7.2"
}

description = "coap-core"

dependencies {
    api("org.slf4j:slf4j-api:2.0.16")

    testFixturesApi("org.junit.jupiter:junit-jupiter-api:5.11.1")
    testFixturesApi("org.assertj:assertj-core:3.26.3")
    testFixturesApi("org.awaitility:awaitility:4.2.2")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.1")
    testImplementation("ch.qos.logback:logback-classic:1.3.14")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.17")
    testImplementation("io.github.artsok:rerunner-jupiter:2.1.6")

    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-bytecode:1.37")
}

tasks {
    named("pmdTestFixtures").get().enabled = false
    named("pmdJmh").get().enabled = false
    named("spotbugsJmh").get().enabled = false
}

plugins {
    id("java-library")
    id("java-test-fixtures")
    id("me.champeau.jmh") version "0.7.3"
}

description = "coap-core"

dependencies {
    api("org.slf4j:slf4j-api:2.0.17")

    testFixturesApi("org.junit.jupiter:junit-jupiter-api:5.13.0")
    testFixturesApi("org.assertj:assertj-core:3.27.3")
    testFixturesApi("org.awaitility:awaitility:4.3.0")

    testImplementation("ch.qos.logback:logback-classic:1.3.15")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.19.4")
    testImplementation("io.github.artsok:rerunner-jupiter:2.1.6")

    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-bytecode:1.37")
}

tasks {
    named("pmdTestFixtures").get().enabled = false
    named("pmdJmh").get().enabled = false
    named("spotbugsJmh").get().enabled = false
}

plugins {
    id("java-library")
    id("java-test-fixtures")
    id("me.champeau.jmh") version "0.7.3"
}

description = "coap-core"

dependencies {
    api("org.slf4j:slf4j-api:2.0.18")

    testFixturesApi("org.junit.jupiter:junit-jupiter-api:6.1.1")
    testFixturesApi("org.assertj:assertj-core:3.27.7")
    testFixturesApi("org.awaitility:awaitility:4.3.0")

    testImplementation("ch.qos.logback:logback-classic:1.5.37")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:4.5")
    testImplementation("io.github.artsok:rerunner-jupiter:2.1.6")

    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-bytecode:1.37")
}

tasks {
    named("pmdTestFixtures").get().enabled = false
    named("pmdJmh").get().enabled = false
    named("spotbugsJmh").get().enabled = false
}

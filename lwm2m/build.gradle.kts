plugins {
    id("java-library")
}

description = "lwm2m"

dependencies {
    api("com.google.code.gson:gson:2.11.0")
    api("org.slf4j:slf4j-api:2.0.13")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
    testImplementation("commons-io:commons-io:2.16.1")
    testImplementation("ch.qos.logback:logback-classic:1.3.14")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.16.1")
}

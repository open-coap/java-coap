plugins {
    id("java-library")
}

description = "lwm2m"

dependencies {
    api("com.google.code.gson:gson:2.12.1")
    api("org.slf4j:slf4j-api:2.0.16")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.0")
    testImplementation("commons-io:commons-io:2.18.0")
    testImplementation("ch.qos.logback:logback-classic:1.3.14")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:3.19.1")
}

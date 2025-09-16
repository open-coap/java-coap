plugins {
    id("java-library")
}

description = "lwm2m"

dependencies {
    api("com.google.code.gson:gson:2.13.2")
    api("org.slf4j:slf4j-api:2.0.17")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testImplementation("commons-io:commons-io:2.20.0")
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation("org.mockito:mockito-core:5.19.0")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:4.1")
}

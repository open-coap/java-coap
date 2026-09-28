plugins {
    id("java-library")
}

description = "lwm2m"

dependencies {
    api("com.google.code.gson:gson:2.14.0")
    api("org.slf4j:slf4j-api:2.0.20")

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.3")
    testImplementation("commons-io:commons-io:2.22.0")
    testImplementation("ch.qos.logback:logback-classic:1.6.4")
    testImplementation("org.mockito:mockito-core:5.24.0")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("nl.jqno.equalsverifier:equalsverifier:4.5.2")
}

tasks {
    // Disable PMD for this deprecated module
    named("pmdMain").configure {
        enabled = false
    }
}

tasks {
    // Disable PMD for this deprecated module
    named("pmdMain").configure {
        enabled = false
    }
}

plugins {
    id("java-library")
}

description = "coap-metrics"

dependencies {
    api(project(":coap-core"))

    implementation("io.micrometer:micrometer-core:1.10.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

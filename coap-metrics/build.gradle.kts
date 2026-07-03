plugins {
    id("java-library")
}

description = "coap-metrics"

dependencies {
    api(project(":coap-core"))

    implementation("io.micrometer:micrometer-core:1.17.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.1")
    testImplementation(testFixtures(project(":coap-core")))
}

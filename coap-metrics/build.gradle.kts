plugins {
    id("java-library")
}

description = "coap-metrics"

dependencies {
    api(project(":coap-core"))

    implementation("io.micrometer:micrometer-core:1.15.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.0")
    testImplementation(testFixtures(project(":coap-core")))
}

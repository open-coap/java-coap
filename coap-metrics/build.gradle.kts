plugins {
    id("java-library")
}

description = "coap-metrics"

dependencies {
    api(project(":coap-core"))

    implementation("io.micrometer:micrometer-core:1.14.4")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.0")
    testImplementation(testFixtures(project(":coap-core")))
}

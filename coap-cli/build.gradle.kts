plugins {
    id("application")
}

description = "coap-cli"

dependencies {
    implementation(project(":coap-core"))
    implementation(project(":coap-tcp"))
    implementation(project(":lwm2m"))
    implementation(project(":coap-mbedtls"))
    implementation("org.slf4j:slf4j-api:2.0.18")
    implementation("ch.qos.logback:logback-classic:1.6.1")
    implementation("info.picocli:picocli:4.7.7")

    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.2")
    testImplementation("org.awaitility:awaitility:4.3.0")
    testImplementation(testFixtures(project(":coap-core")))
}

tasks {
    withType<JacocoBase> { enabled = false }
    withType<AbstractPublishToMaven> { enabled = false }
}

application {
    mainClass.set("com.mbed.coap.cli.Main")
}

distributions {
    application.applicationName = "coap"
    main {
        distributionBaseName.set("coap-cli")
    }
}

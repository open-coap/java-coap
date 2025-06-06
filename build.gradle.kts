import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.spotbugs.snom.Effort

plugins {
    id("java")
    id("maven-publish")
    id("com.github.mfarsikov.kewt-versioning") version "1.0.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("pmd")
    id("com.github.spotbugs") version "6.1.13"
    id("org.gradle.signing")
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.adarshr.test-logger") version "4.0.0"
}

allprojects {
    apply {
        plugin("java")
        plugin("com.github.mfarsikov.kewt-versioning")
        plugin("se.patrikerdes.use-latest-versions")
        plugin("com.github.ben-manes.versions")
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.0")
    }

    kewtVersioning.configuration {
        separator = ""
    }
    version = kewtVersioning.version
    group = "io.github.open-coap"

    tasks.withType<DependencyUpdatesTask> {
        rejectVersionIf {
            val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { candidate.version.uppercase().contains(it) }
            val regex = "^[0-9,.v-]+(-r)?$".toRegex()
            val isNonStable = !(stableKeyword || regex.matches(candidate.version))

            // newer version of logback-classic is not java8 compatible
            // newer version of kewt-versioning plugin requires java 21
            // newer version of equalsverifier is not java8 compatible
            isNonStable || listOf("logback-classic", "mockito-core", "com.github.mfarsikov.kewt-versioning.gradle.plugin", "nl.jqno.equalsverifier").contains(candidate.module)
        }
    }

}

subprojects {
    apply {
        plugin("java")
        plugin("pmd")
        plugin("com.github.spotbugs")
        plugin("jacoco")
        plugin("maven-publish")
        plugin("org.gradle.signing")
        plugin("com.adarshr.test-logger")
    }

    val projSourceSets = extensions.getByName("sourceSets") as SourceSetContainer

    tasks {
        withType<Test> {
            useJUnitPlatform()
        }

        withType<JavaCompile> {
            if (!JavaVersion.current().isJava8) {
                options.release.set(8)
            }
            options.encoding = "UTF-8"
        }

        withType<JacocoReport> {
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }

        withType<Javadoc> {
            (options as CoreJavadocOptions).addBooleanOption("Xdoclint:accessibility,html,syntax,reference", true)
        }

        create<Jar>("sourceJar") {
            archiveClassifier.set("sources")
            from(projSourceSets["main"].allSource)
        }

        create<Jar>("javadocJar") {
            archiveClassifier.set("javadoc")
            from(javadoc)
        }

        named("pmdTest").get().enabled = false
        named("spotbugsTest").get().enabled = false
    }

    pmd {
        toolVersion = "6.55.0"
        isConsoleOutput = true
        ruleSets = emptyList()
        ruleSetFiles = files(rootProject.file("pmd-rules.xml"))
    }

    spotbugs {
        effort.set(Effort.MAX)
        excludeFilter.set(rootProject.file("spotbugs-exlude.xml"))
    }
    configurations.named("spotbugs").configure {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.ow2.asm") {
                useVersion("9.5")
                because("Asm 9.5 is required for JDK 21 support")
            }
        }
    }

    publishing {
        publications {
            create<MavenPublication>("OSSRH") {
                from(components["java"])
                groupId = "io.github.open-coap"
                artifact(tasks["sourceJar"])
                artifact(tasks["javadocJar"])

                pom {
                    name.set("Java CoAP")
                    description.set("Java implementation of CoAP protocol")
                    url.set("https://github.com/open-coap/java-coap")
                    scm {
                        url.set("https://github.com/open-coap/java-coap")
                    }
                    licenses {
                        license {
                            name.set("Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            name.set("Szymon Sasin")
                            email.set("szymon.sasin@gmail.com")
                        }
                    }
                }
            }
        }
    }

    signing {
        val signingKeyId: String? by project
        val signingKey: String? by project
        val signingPassword: String? by project

        if (signingKey != null) {
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
            sign(publishing.publications["OSSRH"])
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            val ossrhUserName: String? by project
            val ossrhPassword: String? by project

            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(ossrhUserName)
            password.set(ossrhPassword)
        }
    }
}

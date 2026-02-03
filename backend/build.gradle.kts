plugins {
    `java-library`
    id("org.springframework.boot") version "3.3.5" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
}

allprojects {
    group = "com.mhub"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
            mavenBom("io.github.resilience4j:resilience4j-bom:2.2.0")
            mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:3.2.1")
            mavenBom("org.testcontainers:testcontainers-bom:1.20.4")
        }
        dependencies {
            dependency("net.javacrumbs.shedlock:shedlock-spring:5.16.0")
            dependency("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.16.0")
            dependency("org.mapstruct:mapstruct:1.6.3")
            dependency("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
            dependency("org.wiremock:wiremock-standalone:3.10.0")
            dependency("io.hypersistence:hypersistence-utils-hibernate-63:3.9.0")
        }
    }

    dependencies {
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        "annotationProcessor"("org.mapstruct:mapstruct-processor:1.6.3")
        "annotationProcessor"("org.projectlombok:lombok-mapstruct-binding:0.2.0")

        "implementation"("org.slf4j:slf4j-api")

        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"("org.projectlombok:lombok")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

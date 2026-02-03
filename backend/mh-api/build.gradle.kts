plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":mh-core"))
    implementation(project(":mh-marketplace"))
    implementation(project(":mh-shipping"))
    implementation(project(":mh-erp"))
    implementation(project(":mh-scheduler"))

    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")

    // Test
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.wiremock:wiremock-standalone")
}

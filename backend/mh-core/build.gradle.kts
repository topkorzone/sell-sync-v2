dependencies {
    api(project(":mh-common"))

    // Spring (api - types exposed to dependent modules)
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // OAuth2 Resource Server + JWT
    api("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    api("org.springframework.security:spring-security-oauth2-jose")

    // MapStruct
    implementation("org.mapstruct:mapstruct")

    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3")
    implementation("io.github.resilience4j:resilience4j-ratelimiter")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Hypersistence for JSONB
    implementation("io.hypersistence:hypersistence-utils-hibernate-63")
}

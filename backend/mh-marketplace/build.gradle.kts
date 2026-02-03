dependencies {
    api(project(":mh-core"))

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
}

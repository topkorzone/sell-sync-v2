dependencies {
    implementation(project(":mh-core"))
    implementation(project(":mh-marketplace"))
    implementation(project(":mh-erp"))

    // ShedLock
    implementation("net.javacrumbs.shedlock:shedlock-spring")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template")

    // AWS SQS
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
}

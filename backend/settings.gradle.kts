pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "marketplace-hub"

include(
    "mh-common",
    "mh-core",
    "mh-marketplace",
    "mh-shipping",
    "mh-erp",
    "mh-scheduler",
    "mh-api"
)

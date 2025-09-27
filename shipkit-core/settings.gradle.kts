rootProject.name = "shipkit-core"
include(
    "shipkit-app",
    "shipkit-web",
    "shipkit-api",
    "shipkit-users",
    "shipkit-deployments",
    "shipkit-templates",
    "shipkit-k8s-adapter"
)

include("buildSrc")
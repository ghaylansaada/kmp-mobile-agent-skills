import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT

buildkonfig {
    packageName = "{your.package}"
    objectName = "BuildKonfig"

    val flavor = project.findProperty("flavor")?.toString() ?: "dev"

    defaultConfigs {
        buildConfigField(STRING, "BASE_URL", "https://dev-api.example.com/")
        buildConfigField(STRING, "ENVIRONMENT", "dev")
        buildConfigField(STRING, "APP_NAME", "Mobile Template Dev")
        buildConfigField(BOOLEAN, "ENABLE_LOGGING", "true")
        buildConfigField(BOOLEAN, "ENABLE_MOCK_DATA", "true")
        buildConfigField(BOOLEAN, "ENABLE_ANALYTICS", "false")
        buildConfigField(BOOLEAN, "ENABLE_CRASH_REPORTING", "false")
        buildConfigField(BOOLEAN, "ENABLE_NETWORK_INSPECTOR", "true")
        buildConfigField(INT, "API_TIMEOUT_SECONDS", "60")
    }

    defaultConfigs(flavor = "staging") {
        buildConfigField(STRING, "BASE_URL", "https://staging-api.example.com/")
        buildConfigField(STRING, "ENVIRONMENT", "staging")
        buildConfigField(STRING, "APP_NAME", "Mobile Template Staging")
        buildConfigField(BOOLEAN, "ENABLE_LOGGING", "true")
        buildConfigField(BOOLEAN, "ENABLE_MOCK_DATA", "false")
        buildConfigField(BOOLEAN, "ENABLE_ANALYTICS", "true")
        buildConfigField(BOOLEAN, "ENABLE_CRASH_REPORTING", "true")
        buildConfigField(BOOLEAN, "ENABLE_NETWORK_INSPECTOR", "true")
        buildConfigField(INT, "API_TIMEOUT_SECONDS", "30")
    }

    defaultConfigs(flavor = "prod") {
        buildConfigField(STRING, "BASE_URL", "https://api.example.com/")
        buildConfigField(STRING, "ENVIRONMENT", "prod")
        buildConfigField(STRING, "APP_NAME", "Mobile Template")
        buildConfigField(BOOLEAN, "ENABLE_LOGGING", "false")
        buildConfigField(BOOLEAN, "ENABLE_MOCK_DATA", "false")
        buildConfigField(BOOLEAN, "ENABLE_ANALYTICS", "true")
        buildConfigField(BOOLEAN, "ENABLE_CRASH_REPORTING", "true")
        buildConfigField(BOOLEAN, "ENABLE_NETWORK_INSPECTOR", "false")
        buildConfigField(INT, "API_TIMEOUT_SECONDS", "15")
    }
}

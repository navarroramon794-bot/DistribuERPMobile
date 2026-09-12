import groovy.json.JsonSlurper
import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val versionJsonPath = providers.gradleProperty("versionJsonPath").getOrElse(
    rootProject.file("version.json").takeIf { it.exists() }?.absolutePath ?: "C:/Distribu-erp/version.json"
)

val apiUrlProp: String? = providers.gradleProperty("apiUrl").getOrNull()

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties()
if (keystorePropsFile.exists()) {
    FileInputStream(keystorePropsFile).use { keystoreProps.load(it) }
}

fun loadVersionInfo(): Pair<Int, String> {
    val archivo = File(versionJsonPath)
    if (!archivo.exists()) return 1 to "0.9.0"
    return try {
        val json = JsonSlurper().parse(archivo) as Map<*, *>
        val version = json["version"] as? String ?: "0.9.0"
        val build = when (val valor = json["build"]) {
            is Number -> valor.toInt()
            is String -> valor.filter { it.isDigit() }.toIntOrNull() ?: 1
            else -> 1
        }
        build to version
    } catch (_: Exception) {
        1 to "0.9.0"
    }
}

val (versionCodeVal, versionNameVal) = loadVersionInfo()

android {
    namespace = "com.distribuerp.mobile"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.distribuerp.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = versionCodeVal
        versionName = versionNameVal
        buildConfigField("String", "APP_VERSION", "\"$versionNameVal\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${apiUrlProp ?: "https://distribu-erp.onrender.com/"}\""
            )
        }

        release {
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${apiUrlProp ?: "https://distribu-erp.onrender.com/"}\""
            )

            if (keystoreProps.containsKey("storeFile")) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = rootProject.file(
                        keystoreProps.getProperty("storeFile")
                    )
                    storePassword = keystoreProps.getProperty("storePassword")
                    keyAlias = keystoreProps.getProperty("keyAlias")
                    keyPassword = keystoreProps.getProperty("keyPassword")
                }
            }

            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation(libs.zxing.android.embedded)
    implementation(libs.osmdroid.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
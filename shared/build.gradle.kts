plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.example.shared"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    minSdk = 24
    consumerProguardFiles("consumer-rules.pro")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
  }
}

dependencies {
  api(platform(libs.androidx.compose.bom))
  api(platform(libs.firebase.bom))
  api(libs.androidx.activity.compose)
  api(libs.androidx.compose.material.icons.core)
  api(libs.androidx.compose.material.icons.extended)
  api(libs.androidx.compose.material3)
  api(libs.androidx.compose.ui)
  api(libs.androidx.compose.ui.graphics)
  api(libs.androidx.compose.ui.tooling.preview)
  api(libs.androidx.core.ktx)
  api(libs.androidx.datastore.preferences)
  api(libs.androidx.lifecycle.runtime.compose)
  api(libs.androidx.lifecycle.runtime.ktx)
  api(libs.androidx.lifecycle.viewmodel.compose)
  api(libs.androidx.navigation.compose)
  api(libs.coil.compose)
  api(libs.firebase.firestore)
  api(libs.firebase.auth)
  api(libs.androidx.credentials)
  api(libs.androidx.credentials.play.services)
  api(libs.googleid)
  api(libs.kotlinx.coroutines.android)
  api(libs.kotlinx.coroutines.core)
  api(libs.mlkit.barcode.scanning)
  api(libs.play.services.location)
  api(libs.androidx.camera.camera2)
  api(libs.androidx.camera.core)
  api(libs.androidx.camera.lifecycle)
  api(libs.androidx.camera.view)
  debugApi(libs.androidx.compose.ui.tooling)
}

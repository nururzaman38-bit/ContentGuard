plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android { namespace = "com.etnajid.appblocker"; compileSdk = 35
    defaultConfig { applicationId = "com.etnajid.appblocker"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0" }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt") } }
    buildTypes { release { isMinifyEnabled = false } }
}

dependencies { implementation("androidx.core:core-ktx:1.15.0"); implementation("androidx.appcompat:appcompat:1.7.0"); implementation("androidx.activity:activity-ktx:1.10.0"); implementation("androidx.lifecycle:lifecycle-service:2.8.7"); implementation("androidx.security:security-crypto:1.1.0-alpha06") }

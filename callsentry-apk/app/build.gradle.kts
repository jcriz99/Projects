plugins { id("com.android.application") }
android {
    namespace = "com.example.callsentry"
    compileSdk = 36
    defaultConfig { applicationId = "com.example.callsentry"; minSdk = 29; targetSdk = 36; versionCode = 5; versionName = "1.2.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

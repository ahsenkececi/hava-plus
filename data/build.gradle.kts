/*
 * :data — verinin nereden geldiğini bilen tek modül: Open-Meteo, OSRM, Photon (Retrofit + Gson),
 * DataStore ve Android konum servisi.
 *
 * Bütün sınıfları `internal`; dışarıya sadece DataModule üzerinden domain arayüzlerini verir.
 */
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ahsen.hava.data"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        // ApiClient'ta BuildConfig.DEBUG: ağ log'u sadece debug derlemede.
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Domain'in tiplerini (arayüzler, modeller) bu modülü kullanan da görsün.
    api(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

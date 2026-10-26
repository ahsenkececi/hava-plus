import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * :domain — uygulamanın kalbi: modeller, iş kuralları, use case'ler ve repository arayüzleri.
 *
 * Bilerek bir Android modülü DEĞİL, saf Kotlin/JVM kütüphanesi. Android SDK'yı, Retrofit'i,
 * Compose'u hiç görmez; görmeye çalışan bir import derlenmez. Testleri de emülatörsüz,
 * milisaniyeler içinde çalışır.
 */
plugins {
    alias(libs.plugins.kotlin.jvm)
    // src/testFixtures: sahte repository'ler ve test verileri; :app testleri de kullanır.
    `java-test-fixtures`
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    // Flow tipi repository arayüzlerinde kullanılıyor (Android'e bağımlı değil).
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

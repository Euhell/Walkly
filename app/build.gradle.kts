import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val localProps = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}
val WEB_CLIENT_ID = localProps["WEB_CLIENT_ID"] as String
val API_KEY = localProps["API_KEY"] as String


android {
    namespace = "com.example.samsungproject"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.samsungproject"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "WEB_CLIENT_ID", "\"$WEB_CLIENT_ID\"")
        buildConfigField("String", "API_KEY", "\"$API_KEY\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.navigation:navigation-fragment:2.8.9")
    implementation("androidx.navigation:navigation-ui:2.8.9")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation ("org.osmdroid:osmdroid-android:6.1.20")
    implementation ("com.google.firebase:firebase-auth:23.2.0")
    implementation ("com.google.firebase:firebase-database:21.0.0")
    implementation("com.google.firebase:firebase-firestore:25.1.4")
    implementation ("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation(libs.coordinatorlayout)
    implementation(libs.cronet.embedded)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}
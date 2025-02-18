plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    //id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.nguyenmoclam.kotlinsandboxexec"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nguyenmoclam.kotlinsandboxexec"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
    packaging {
        resources {
//            pickFirsts += listOf(
//                "kotlin/coroutines/coroutines.kotlin_builtins",
//                "kotlin/kotlin.kotlin_builtins",
//                "kotlin/reflect/reflect.kotlin_builtins",
//                "kotlin/collections/collections.kotlin_builtins",
//                "kotlin/annotation/annotation.kotlin_builtins",
//                "kotlin/internal/internal.kotlin_builtins",
//                "kotlin/ranges/ranges.kotlin_builtins"
//            )
            pickFirsts += listOf(
                "*/*.kotlin_builtins",
                "*/*/*.kotlin_builtins",
                "*/*/*/*.kotlin_builtins"
            )
        }
    }

}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    // Android Core
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.0")

    // Navigation Components
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

//    // Kotlin Scripting Dependencies
//    implementation("org.jetbrains.kotlin:kotlin-scripting-common:1.9.24")
//    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:1.9.24")
//    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:1.9.24")
//    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:1.9.24")
//    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.9.24")
//    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:1.9.24")
//    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-impl-embeddable:1.9.24")

// Kotlin stdlib
    //implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")

    // Kotlin scripting core
//    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.9.24")
//    implementation("org.jetbrains.kotlin:kotlin-scripting-common:1.9.24")
//    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:1.9.24")
//    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:1.9.24")
//    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:1.9.24")
    // Compiler nhúng (giúp compile code tại runtime)
    //implementation(files("libs/kotlin-compiler-embeddable-1.7.20-RC.jar"))
    //implementation(files("libs/kotlin-compiler-embeddable-1.9.24.jar"))
    // Remove kotlin-scripting dependencies if they exist
    // implementation("org.jetbrains.kotlin:kotlin-scripting-common")
    // implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")
    // implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host")

    // Code editor component
    implementation(platform("io.github.Rosemoe.sora-editor:bom:0.23.5"))
    implementation("io.github.Rosemoe.sora-editor:editor")
    implementation("io.github.Rosemoe.sora-editor:language-textmate")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")

    // Room for local storage
//    implementation("androidx.room:room-runtime:2.5.0")
//    implementation("androidx.room:room-ktx:2.5.0")
//    kapt("androidx.room:room-compiler:2.5.0")

    // Security Crypto
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // OpenAI API client
    implementation("com.aallam.openai:openai-client:3.2.0")
    implementation("io.ktor:ktor-client-android:2.3.0")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.12.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation ("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.7.20-RC")
}
plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.subhadeeproy3902.emojet"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.subhadeeproy3902.emojet"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // The Laya engine (llama-server) ships as libllama_server.so and must be extracted
    // to nativeLibraryDir: the only place Android lets an app execute its own binary.
    packaging {
        jniLibs { useLegacyPackaging = true }
    }

    // moods.json, tones.json, emoji-catalog.json come straight from the repo's shared/ folder.
    sourceSets {
        getByName("main") { assets.srcDir("../../shared") }
    }
}

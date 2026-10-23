import java.util.Properties

plugins {
    id("com.android.application")
}

// The owner's signing key lives outside the repo (~/.config/emojet). Same key for test and release
// builds, so a test install can update in place to a release. Other builders get the debug key.
val keyProps = Properties().apply {
    val f = file("${System.getProperty("user.home")}/.config/emojet/keystore.properties")
    if (f.isFile) f.inputStream().use { load(it) }
}

android {
    namespace = "io.github.subhadeeproy3902.emojet"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.subhadeeproy3902.emojet"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "1.0.0"
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    if (keyProps.isNotEmpty()) {
        signingConfigs {
            create("emojet") {
                storeFile = file(keyProps.getProperty("storeFile"))
                storePassword = keyProps.getProperty("storePassword")
                keyAlias = keyProps.getProperty("keyAlias")
                keyPassword = keyProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            if (keyProps.isNotEmpty()) signingConfig = signingConfigs.getByName("emojet")
        }
        release {
            if (keyProps.isNotEmpty()) signingConfig = signingConfigs.getByName("emojet")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    buildFeatures { buildConfig = true }

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

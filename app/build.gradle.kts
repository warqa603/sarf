import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val geminiApiKey: String = localProperties.getProperty("gemini.api.key", "")

android {
    namespace = "com.cash.guide"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tajir.sarf"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

        // Vector drawable support
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Multi-dex support (if needed in future)
        multiDexEnabled = false
    }

    // Signing configuration for release builds
    // To set up signing:
    // 1. Create a keystore file: keytool -genkey -v -keystore sarf-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias sarf
    // 2. Create keystore.properties file in the project root with:
    //    storePassword=your_store_password
    //    keyPassword=your_key_password
    //    keyAlias=sarf
    //    storeFile=../sarf-release-key.jks
    // 3. Uncomment the signingConfigs block below and update the paths
    signingConfigs {
        // Uncomment and configure when ready for release:
        /*
        getByName("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = java.util.Properties()
                keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
        */
    }

    buildTypes {
        release {
            // Enable code shrinking and resource shrinking for smaller APK
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Uncomment when signing config is set up:
            // signingConfig = signingConfigs.getByName("release")
            
            // Optimize for release
            isDebuggable = false
            isJniDebuggable = false
        }
        debug {
            // Keep debug builds fast
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    
    // Packaging options for Play Store
    packaging {
        resources {
            excludes += "/META-INF/{ALICE.AND,Bob.and,Carol,He,eve.and,it.and,j.marry,and,l.END,she.END,will.END,Alice.and,Bob.and,Carol.and,David.and,Eve.and,Frank.and,Grace.and,Henry.and,Ivan.and,Julia.and,Kenny.and,Laura.and,Mallory.and,Niaj.and,Oscar.and,Peggy.and,Quentin.and,Rupert.and,Sybil.and,Ted.and,Una.and,Victor.and,Wendy.and,Xavier.and,Yvonne.and,Zoe.and}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            // Exclude duplicate files
            excludes += "/META-INF/*.kotlin_module"
            excludes += "/META-INF/*.version"
        }
        // Play Store: Use standard APK/AAB format
        jniLibs {
            useLegacyPackaging = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    // Lint options for Play Store compliance
    lint {
        // Don't abort build if lint finds errors
        abortOnError = false
        // Check all issues, including those that are off by default
        checkAllWarnings = true
        // Treat warnings as errors (optional, for stricter checks)
        warningsAsErrors = false
        // Disable lint checks that are not relevant for Play Store
        disable += listOf("MissingTranslation", "ExtraTranslation")
    }
    
    // Play Store: Bundle configuration (recommended for smaller downloads)
    bundle {
        language {
            // Disabled: In-app language picker requires all locales to be bundled in the APK
            enableSplit = false
        }
        density {
            // Enable density splits (optional, can reduce APK size)
            enableSplit = false // Keep false for simplicity, enable if needed
        }
        abi {
            // Enable ABI splits (optional, can reduce APK size)
            enableSplit = false // Keep false for simplicity, enable if needed
        }
    }
    testOptions {
        animationsDisabled = true
    }
}

dependencies {
    implementation("com.android.billingclient:billing-ktx:6.2.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.google.play.services.ads)

    // Room persistence
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation("org.json:json:20240303")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

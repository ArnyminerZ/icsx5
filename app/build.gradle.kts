import com.mikepenz.aboutlibraries.plugin.DuplicateMode

plugins {
    alias(libs.plugins.aboutLibs)
    alias(libs.plugins.android)
    alias(libs.plugins.kapt)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.ksp)
}

android {
    compileSdk = 34

    namespace = "at.bitfire.icsdroid"

    defaultConfig {
        applicationId = "at.bitfire.icsdroid"
        minSdk = 21
        targetSdk = 34

        versionCode = 73
        versionName = "2.2-beta.1"

        setProperty("archivesBaseName", "icsx5-$versionCode-$versionName")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
        dataBinding = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("standard") {}
        create("gplay") {}
    }

    signingConfigs {
        create("bitfire") {
            storeFile = file(System.getenv("ANDROID_KEYSTORE") ?: "/dev/null")
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("bitfire")
        }
    }

    lint {
        disable.addAll(
            listOf("ExtraTranslation", "MissingTranslation", "InvalidPackage", "OnClick")
        )
    }

    packaging {
        resources {
            excludes += "META-INF/*.md"
        }
    }

    androidResources {
        generateLocaleConfig = true
    }
}

configurations {
    configureEach {
        // exclude modules which are in conflict with system libraries
        exclude(module = "commons-logging")
        exclude(group = "org.json", module = "json")

        // Groovy requires SDK 26+, and it"s not required, so exclude it
        exclude(group = "org.codehaus.groovy")
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    coreLibraryDesugaring(libs.desugar)

    implementation(libs.bitfire.cert4android)
    implementation(libs.bitfire.ical4android)

    implementation(libs.android.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.core)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.work.runtime)

    // Jetpack Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.compose.material)
    implementation(libs.compose.preview)
    implementation(libs.compose.runtime.livedata)
    debugImplementation(libs.compose.tooling)

    implementation(libs.compose.accompanist.themeadapter)
    implementation(libs.compose.materialDialogs.color)

    implementation(libs.jaredrummler.colorpicker)
    implementation(libs.compose.aboutLibs)
    implementation(libs.okhttp.base)
    implementation(libs.okhttp.brotli)
    implementation(libs.okhttp.coroutines)
    implementation(libs.jodatime)

    // latest commons that don"t require Java 8
    //noinspection GradleDependency
    implementation(libs.commons.io)
    //noinspection GradleDependency
    implementation(libs.commons.lang)

    // Room Database
    implementation(libs.room.base)
    ksp(libs.room.compiler)

    // for tests
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.arch)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.work.testing)

    testImplementation(libs.junit)
}

aboutLibraries {
    duplicationMode = DuplicateMode.MERGE
    includePlatform = false
}

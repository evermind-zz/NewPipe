/*
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

plugins {
    id("brave.pipe.plugin")
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.kapt)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.jetbrains.kotlin.parcelize)
    alias(libs.plugins.sonarqube)
    checkstyle
}

val gitWorkingBranch = providers.exec {
    commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
}.standardOutput.asText.map { it.trim() }

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    compilerOptions {
        // TODO: Drop annotation default target when it is stable
        freeCompilerArgs.addAll(
            "-Xannotation-default-target=param-property"
        )
    }
}

android {
    compileSdk = 36
    namespace = "org.schabi.newpipe"

    defaultConfig {
        applicationId = "org.schabi.newpipe"
        resValue("string", "app_name", "NewPipe")
        minSdk = 21
        targetSdk = 35

        versionCode = System.getProperty("versionCodeOverride")?.toInt() ?: 1008

        versionName = "0.28.3"
        System.getProperty("versionNameSuffix")?.let { versionNameSuffix = it }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true

            // suffix the app id and the app name with git branch name
            val defaultBranches = listOf("master", "dev")
            val workingBranch = gitWorkingBranch.getOrElse("")
            val normalizedWorkingBranch = workingBranch
                .replaceFirst("^[^A-Za-z]+".toRegex(), "")
                .replace("[^0-9A-Za-z]+".toRegex(), "")

            if (normalizedWorkingBranch.isEmpty() || workingBranch in defaultBranches) {
                // default values when branch name could not be determined or is master or dev
                applicationIdSuffix = ".debug"
                resValue("string", "app_name", "BravePipe Debug")
            } else {
                applicationIdSuffix = ".debug.$normalizedWorkingBranch"
                resValue("string", "app_name", "BravePipe $workingBranch")
            }
        }

        release {
            System.getProperty("packageSuffix")?.let { suffix ->
                applicationIdSuffix = suffix
                resValue("string", "app_name", "NewPipe $suffix")
            }
            isMinifyEnabled = true
            isShrinkResources = false // disabled to fix F-Droid"s reproducible build
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    // use productFlavors to keep the name/version changes AFAP for BravePipe
    // more separate in hope of not getting to many merge conflicts
    flavorDimensions += "default"
    productFlavors {
        // the amount of trailing zeros depends on the amount of digits the
        // defaultConfig.versionCode has -> we just prepend our increasing
        // versionCode before those zeros.
        val braveVersionCode = 530000
        // -> our versionName will be added as suffix to defaultConfig.versionName
        // We use major.minor.patch
        val braveVersionName = "2.6.0"

        create("sponsorblock") { // only for strings of sponsorblock stuff
            dimension = "default"
            android.sourceSets.getByName("sponsorblock") {
                res.srcDirs(listOf(
                    "src/sponsorblock/res",
                    android.sourceSets.getByName("sponsorblock").res.srcDirs
                ))
            }
        }

        create("brave") {
            dimension = "default"
            applicationId = "com.github.bravenewpipe"
            resValue("string", "app_name", "BravePipe")
            versionCode = defaultConfig.versionCode!! + braveVersionCode
            versionName = "${defaultConfig.versionName}-$braveVersionName"

            android.sourceSets.getByName("brave") {
                res.srcDirs(listOf(
                    "src/brave/res",
                    android.sourceSets.getByName("sponsorblock").res.srcDirs
                ))
            }
        }

        create("braveConscrypt") {
            dimension = "default"
            applicationId = "com.github.bravenewpipe"
            resValue("string", "app_name", "BravePipe")
            versionCode = defaultConfig.versionCode!! + braveVersionCode
            versionName = "${defaultConfig.versionName}-$braveVersionName"

            android.sourceSets.getByName("braveConscrypt") {
                res.srcDirs(listOf(
                    android.sourceSets.getByName("brave").res.srcDirs
                ))
            }

            //noinspection WrongGradleMethod
            dependencies {
                "braveConscryptImplementation"("org.conscrypt:conscrypt-android:2.5.2")
            }
        }

        create("braveLegacy") {
            dimension = "default"
            applicationId = "com.github.bravenewpipe.kitkat"
            resValue("string", "app_name", "BravePipe Kitkat")
            versionCode = defaultConfig.versionCode!! + braveVersionCode
            versionName = "${defaultConfig.versionName}-$braveVersionName"

            android.sourceSets.getByName("braveLegacy") {
                res.srcDirs(listOf(
                    "src/braveLegacy/res",
                    android.sourceSets.getByName("braveConscrypt").res.srcDirs
                ))
            }

            multiDexEnabled = true
            minSdk = 19

            //noinspection WrongGradleMethod
            dependencies {
                "braveLegacyImplementation"("androidx.multidex:multidex:2.0.1")
                "braveLegacyImplementation"("org.conscrypt:conscrypt-android:2.5.2")
                "braveLegacyImplementation"("com.github.evermind-zz.OsExt:osext-stat:1.0.1")
            }
        }
    }

    lint {
        checkReleaseBuilds = false
        // Or, if you prefer, you can continue to check for errors in release builds,
        // but continue the build even when errors are found:
        abortOnError = false
        // suppress false warning ("Resource IDs will be non-final in Android Gradle Plugin version
        // 5.0, avoid using them in switch case statements"), which affects only library projects
        disable += "NonConstantResourceId"
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        encoding = "utf-8"
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDir("$projectDir/schemas")
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            // remove two files which belong to jsoup
            // no idea how they ended up in the META-INF dir...
            excludes += setOf(
                "META-INF/README.md",
                "META-INF/CHANGES",
                "META-INF/COPYRIGHT" // "COPYRIGHT" belongs to RxJava...
            )
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}


// Custom dependency configuration for ktlint
val ktlint by configurations.creating

// https://checkstyle.org/#JRE_and_JDK
tasks.withType<Checkstyle>().configureEach {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

checkstyle {
    configDirectory = rootProject.file("checkstyle")
    isIgnoreFailures = false
    isShowViolations = true
    toolVersion = libs.versions.checkstyle.get()
}

tasks.register<Checkstyle>("runCheckstyle") {
    source("src")
    include("**/*.java")
    exclude("**/gen/**")
    exclude("**/R.java")
    exclude("**/BuildConfig.java")
    exclude("main/java/us/shandian/giga/**")
    exclude("braveLegacy/java/us/shandian/giga/**")

    classpath = configurations.getByName("checkstyle")

    isShowViolations = true

    reports {
        xml.required = true
        html.required = true
    }
}

val outputDir = project.layout.buildDirectory.dir("reports/ktlint/")
val inputFiles = fileTree("src") { include("**/*.kt") }

tasks.register<JavaExec>("runKtlint") {
    inputs.files(inputFiles)
    outputs.dir(outputDir)
    mainClass.set("com.pinterest.ktlint.Main")
    classpath = configurations.getByName("ktlint")
    args = listOf("--editorconfig=../.editorconfig", "src/**/*.kt")
    jvmArgs = listOf("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

tasks.register<JavaExec>("formatKtlint") {
    inputs.files(inputFiles)
    outputs.dir(outputDir)
    mainClass.set("com.pinterest.ktlint.Main")
    classpath = configurations.getByName("ktlint")
    args = listOf("--editorconfig=../.editorconfig", "-F", "src/**/*.kt")
    jvmArgs = listOf("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

tasks.register<CheckDependenciesOrder>("checkDependenciesOrder") {
    tomlFile = layout.projectDirectory.file("../gradle/libs.versions.toml")
}

afterEvaluate {
    tasks.named("preBraveDebugBuild").configure {
        if (!System.getProperties().containsKey("skipFormatKtlint")) {
            dependsOn("formatKtlint")
        }
        dependsOn("runCheckstyle", "runKtlint", "checkDependenciesOrder")
    }
}

sonar {
    properties {
        property("sonar.projectKey", "TeamNewPipe_NewPipe")
        property("sonar.organization", "teamnewpipe")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

dependencies {
    /** Desugaring **/
    coreLibraryDesugaring(libs.android.desugar)

    /** NewPipe libraries **/
    implementation(libs.newpipe.nanojson)
    implementation(libs.newpipe.extractor)
    implementation(libs.newpipe.filepicker)

    /** Checkstyle **/
    checkstyle(libs.puppycrawl.checkstyle)
    ktlint(libs.pinterest.ktlint)

    /** AndroidX **/
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.androidx.media)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.rxjava3)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.work.rxjava3)
    implementation(libs.google.android.material)
    implementation(libs.androidx.webkit)

    /** Third-party libraries **/
    implementation(libs.livefront.bridge)
    implementation(libs.evernote.statesaver.core)
    kapt(libs.evernote.statesaver.compiler)

    // HTML parser
    implementation(libs.jsoup)

    // HTTP client
    implementation(libs.squareup.okhttp)

    // Media player
    implementation(libs.google.exoplayer.core)
    implementation(libs.google.exoplayer.dash)
    implementation(libs.google.exoplayer.database)
    implementation(libs.google.exoplayer.datasource)
    implementation(libs.google.exoplayer.hls)
    implementation(libs.google.exoplayer.mediasession)
    implementation(libs.google.exoplayer.smoothstreaming)
    implementation(libs.google.exoplayer.ui)

    // Manager for complex RecyclerView layouts
    implementation(libs.lisawray.groupie.core)
    implementation(libs.lisawray.groupie.viewbinding)

    // Image loading
    implementation(libs.squareup.picasso)

    // Markdown library for Android
    implementation(libs.noties.markwon.core)
    implementation(libs.noties.markwon.linkify)

    // Crash reporting
    implementation(libs.acra.core)
    compileOnly(libs.google.autoservice.annotations)
    ksp(libs.zacsweers.autoservice.compiler)

    // Properly restarting
    implementation(libs.jakewharton.phoenix)

    // Reactive extensions for Java VM
    implementation(libs.reactivex.rxjava)
    implementation(libs.reactivex.rxandroid)
    // RxJava binding APIs for Android UI widgets
    implementation(libs.jakewharton.rxbinding)

    // Date and time formatting
    implementation(libs.ocpsoft.prettytime)

    /** Debugging **/
    // Memory leak detection
    debugImplementation(libs.squareup.leakcanary.watcher)
    debugImplementation(libs.squareup.leakcanary.plumber)
    debugImplementation(libs.squareup.leakcanary.core)
    // Debug bridge for Android
    debugImplementation(libs.facebook.stetho.core)
    debugImplementation(libs.facebook.stetho.okhttp3)

    /** Testing **/
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.assertj.core)
}

// keep the changed dependencies for BravePipe more
// separate in hope of not getting to many merge conflicts
val okHttpVersion: String = libs.versions.okhttp.get()
// for JavaNetCookieJar see https://github.com/bravepipeproject/BravePipeExtractor/issues/123
project.dependencies.implementation("com.squareup.okhttp3:okhttp-urlconnection:$okHttpVersion")
// for hls support on rumble
project.dependencies.implementation("com.github.evermind-zz:hlsdownloader:1.0.0")
project.dependencies.implementation("com.github.evermind-zz:slimhls-converter:1.0.0")
// apk upgrade dialog/downloader
project.dependencies.implementation("com.github.evermind-zz.AppUpdater:app-dialog:1.2.0-1.2.0")
project.dependencies.implementation("com.github.evermind-zz.AppUpdater:app-updater:1.2.0-1.2.0")
// the eventbus
project.dependencies.implementation("org.greenrobot:eventbus:3.3.1")
project.dependencies.implementation("com.github.bravepipeproject:extractor:v0.25.2-2.3.4")
// the logcatviewer
project.dependencies.implementation("com.github.evermind-zz:logcatviewer:1.0.0")
// cf challenge helper
project.dependencies.implementation("com.github.evermind-zz:challengeFloatsAway:1.0.0")

configurations.all {
    exclude(group = "com.github.TeamNewPipe", module = "NewPipeExtractor")

    if (name.contains("braveLegacy") || name.contains("BraveLegacy")) {
        braveLegacyFlavorLastWorkingMinSdk19Libraries(resolutionStrategy)
    }
}

fun braveLegacyFlavorLastWorkingMinSdk19Libraries(resolutionStrategy: ResolutionStrategy) {
    resolutionStrategy.dependencySubstitution {

        substitute(module("com.github.TeamNewPipe:NoNonsense-FilePicker"))
            .using(module("com.github.bravepipeproject:NoNonsense-FilePicker:21d5c57"))
            .because("we need Sdk 19 support")

        val constraintlayout_last_android_sdk19_version = "2.1.4"
        substitute(module("androidx.constraintlayout:constraintlayout"))
            .using(module("androidx.constraintlayout:constraintlayout:${constraintlayout_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")

        val appcompat_last_android_sdk19_version = "1.6.1"
        substitute(module("androidx.appcompat:appcompat"))
            .using(module("androidx.appcompat:appcompat:${appcompat_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")

        val core_ktx_last_android_sdk19_version = "1.13.0"
        substitute(module("androidx.core:core-ktx"))
            .using(module("androidx.core:core-ktx:${core_ktx_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")

        val documentfile_last_android_sdk19_version = "1.0.1"
        substitute(module("androidx.documentfile:documentfile"))
            .using(module("androidx.documentfile:documentfile:${documentfile_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")

        val fragment_ktx_last_android_sdk19_version = "1.7.1"
        substitute(module("androidx.fragment:fragment-ktx"))
            .using(module("androidx.fragment:fragment-ktx:${fragment_ktx_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")

        val lifecycle_last_android_sdk19_version = "2.8.7"
        substitute(module("androidx.lifecycle:lifecycle-livedata-ktx"))
            .using(module("androidx.lifecycle:lifecycle-livedata-ktx:${lifecycle_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")
        substitute(module("androidx.lifecycle:lifecycle-viewmodel-ktx"))
            .using(module("androidx.lifecycle:lifecycle-viewmodel-ktx:${lifecycle_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")

        val localbroadcastmanager_last_android_sdk19_version = "1.1.0"
        substitute(module("androidx.localbroadcastmanager:localbroadcastmanager"))
            .using(module("androidx.localbroadcastmanager:localbroadcastmanager:${localbroadcastmanager_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")

        val recyclerview_last_android_sdk19_version = "1.3.2"
        substitute(module("androidx.recyclerview:recyclerview"))
            .using(module("androidx.recyclerview:recyclerview:${recyclerview_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")

        val room_last_android_sdk19_version = "2.6.1"
        substitute(module("androidx.room:room-runtime"))
            .using(module("androidx.room:room-runtime:${room_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")
        substitute(module("androidx.room:room-rxjava3"))
            .using(module("androidx.room:room-rxjava3:${room_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")
        substitute(module("androidx.room:room-compiler"))
            .using(module("androidx.room:room-compiler:${room_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")
        substitute(module("androidx.room:room-testing"))
            .using(module("androidx.room:room-testing:${room_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")

        val swiperefreshlayout_last_android_sdk19_version = "1.1.0"
        substitute(module("androidx.swiperefreshlayout:swiperefreshlayout"))
            .using(module("androidx.swiperefreshlayout:swiperefreshlayout:${swiperefreshlayout_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")

        val androidx_work_last_android_sdk19_version = "2.9.1"
        substitute(module("androidx.work:work-runtime"))
            .using(module("androidx.work:work-runtime:${androidx_work_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")
        substitute(module("androidx.work:work-rxjava3"))
            .using(module("androidx.work:work-rxjava3:${androidx_work_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")

        val okhttp_last_android_sdk19_version = "3.12.13"
        substitute(module("com.squareup.okhttp3:okhttp"))
            .using(module("com.squareup.okhttp3:okhttp:${okhttp_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")
        substitute(module("com.squareup.okhttp3:okhttp-urlconnection"))
            .using(module("com.squareup.okhttp3:okhttp-urlconnection:${okhttp_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")

        val webkit_last_android_sdk19_version = "1.12.0"
        substitute(module("androidx.webkit:webkit"))
            .using(module("androidx.webkit:webkit:${webkit_last_android_sdk19_version}"))
            .because("we need Sdk 19 support")
    }
}

/*
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

pluginManagement {
    includeBuild("build-logic/brave-pipe-plugin")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // BravePipe: we have own maven repo for dependencies (jitpack replacement)
        maven(url = "https://raw.githubusercontent.com/bravepipeproject/maven-repo/master/repository")
        maven(url = "https://jitpack.io")
        maven(url = "https://repo.clojars.org")
    }
}
include (":app")

// Use a local copy of NewPipe Extractor by uncommenting the lines below.
// We assume, that NewPipe and NewPipe Extractor have the same parent directory.
// If this is not the case, please change the path in includeBuild().

//includeBuild("../BravePipeExtractor") {
//    dependencySubstitution {
//        substitute(module("com.github.bravepipeproject:extractor"))
//            .using(project(":extractor"))
//    }
//}

//includeBuild("../logcatviewer") {
//    dependencySubstitution {
//        substitute(module("com.github.evermind-zz:logcatviewer"))
//            .using(project(":"))
//    }
//}

//includeBuild("../challengeFloatsAway") {
//    dependencySubstitution {
//        substitute(module("com.github.evermind-zz:challengeFloatsAway"))
//            .using(project(":library"))
//    }
//}

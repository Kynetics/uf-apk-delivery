/*
 *
 *  Copyright (c) 2019 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 */

plugins {
    kotlin("jvm") version ("1.3.0")
    id("maven-publish")
    id("java-gradle-plugin")
}


group = "com.kynetics.updatefactory.plugin"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
    jcenter()

}


val retrofitVersion = "2.4.0"
val androidBuildToolVersion = "3.2.1"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.android.tools.build:gradle:$androidBuildToolVersion")
    implementation ("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation ("com.squareup.retrofit2:converter-gson:$retrofitVersion")

}

gradlePlugin {
    plugins {
        create("UF Apk Delivery") {
            id = "com.kynetics.updatefactory.plugin.ufapkdelivery"
            implementationClass = "com.kynetics.updatefactory.plugin.ufapkdelivery.Plugin"
        }
    }
}
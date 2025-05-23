@file:Suppress("PropertyName")

import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("fabric-loom") version "1.9-SNAPSHOT"
  id("maven-publish")
  kotlin("jvm") version "2.1.0"
  id("com.diffplug.spotless") version "7.0.2"
}

val gradle_version: String by project
val java_version: String by project

val mod_version: String by project
val maven_group: String by project
val archives_base_name: String by project
val access_widener_path: String by project

val minecraft_version: String by project
val minecraft_target_versions: String by project

val fabric_yarn_mappings: String by project
val fabric_loader_version: String by project
val fabric_loader_target_versions: String by project
val fabric_api_version: String by project
val fabric_kotlin_version: String by project

val jts_version: String by project

val palantir_java_format_version: String by project
val ktfmt_version: String by project

version = "$mod_version+$minecraft_version"
group = maven_group

base {
  archivesName.set(archives_base_name)
}

dependencies {
  minecraft("com.mojang", "minecraft", minecraft_version)
  mappings("net.fabricmc", "yarn", fabric_yarn_mappings, classifier = "v2")
  modImplementation("net.fabricmc", "fabric-loader", fabric_loader_version)
  modImplementation("net.fabricmc.fabric-api", "fabric-api", fabric_api_version)
  modImplementation("net.fabricmc", "fabric-language-kotlin", fabric_kotlin_version)
  modImplementation("org.locationtech.jts", "jts-core", jts_version)
}

fabricApi {}

loom {
  accessWidenerPath = file(access_widener_path)
}

tasks.withType<ValidatePlugins>().configureEach {
  failOnWarning.set(true)
  enableStricterValidation.set(true)
}

tasks.named<Wrapper>("wrapper") {
  gradleVersion = gradle_version
}

tasks.named<ProcessResources>("processResources") {
  inputs.property("version", version)

  filesMatching("fabric.mod.json") {
    expand(
        mapOf(
            "version" to version,
            "minecraft_target_versions" to minecraft_target_versions,
            "fabric_loader_target_versions" to fabric_loader_target_versions,
        )
    )
  }
}

tasks.named<JavaCompile>("compileJava") {
  options.release = java_version.toInt()
}

tasks.named<KotlinCompile>("compileKotlin") {
  compilerOptions {
    jvmTarget.set(JvmTarget.fromTarget(java_version))
  }
}

java {
  withSourcesJar()

  sourceCompatibility = JavaVersion.toVersion(java_version)
  targetCompatibility = JavaVersion.toVersion(java_version)
}

tasks.named<Jar>("jar") {
  from("LICENSE") {
    rename {
      "${it}_$archives_base_name"
    }
  }
}

extensions.configure<SpotlessExtension>("spotless") {
  ratchetFrom("origin/main")

  format("misc") {
    target(
        "*.gradle.*",
        ".editorconfig",
        ".git-blame-ignore-revs",
        ".gitignore",
        "gradle.properties",
    )

    trimTrailingWhitespace()
    leadingTabsToSpaces(2)
    endWithNewline()
  }

  java {
    palantirJavaFormat(palantir_java_format_version).style("GOOGLE").formatJavadoc(true)
  }

  kotlin {
    ktfmt(ktfmt_version).googleStyle().configure {
      it.setBlockIndent(2)
      it.setContinuationIndent(4)
    }
  }
}

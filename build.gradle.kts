plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

val localBuildRoot = System.getenv("LOCALAPPDATA")
    ?.let { file("$it/SnakeBuild") }
    ?: file("${rootDir.absolutePath}/.local-build")

rootProject.layout.buildDirectory.set(localBuildRoot.resolve("root"))

subprojects {
    layout.buildDirectory.set(localBuildRoot.resolve(name))
}

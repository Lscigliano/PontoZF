plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
}

// O projeto mora dentro do OneDrive, que trava os milhares de arquivos
// temporários do build no meio da compilação. A pasta de build vai para
// fora da área sincronizada (LOCALAPPDATA nunca é sincronizado).
val localAppData: String? = System.getenv("LOCALAPPDATA")
if (localAppData != null) {
    val raizBuild = file("$localAppData/PontoZF-build")
    layout.buildDirectory.set(raizBuild.resolve("raiz"))
    subprojects {
        layout.buildDirectory.set(raizBuild.resolve(name))
    }
}

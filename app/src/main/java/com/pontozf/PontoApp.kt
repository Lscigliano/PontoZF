package com.pontozf

import android.app.Application
import android.os.Build
import android.util.Log
import java.io.File

/** Nome do arquivo onde o último travamento fica registrado. */
const val ARQUIVO_CRASH = "ultimo-crash.txt"

/**
 * Captura qualquer erro fatal e grava o relatório em disco antes de o app
 * fechar. Na abertura seguinte, a MainActivity mostra o relatório com um
 * botão de compartilhar, para o erro chegar exato ao desenvolvedor.
 */
class PontoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val padrao = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, erro ->
            try {
                File(filesDir, ARQUIVO_CRASH).writeText(
                    buildString {
                        appendLine(
                            "PontoZF ${BuildConfig.VERSION_NAME} — " +
                                "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}"
                        )
                        appendLine()
                        appendLine(Log.getStackTraceString(erro))
                    }
                )
            } catch (_: Exception) {
                // Sem espaço/sem acesso: segue para o encerramento normal.
            }
            padrao?.uncaughtException(thread, erro)
        }
    }
}

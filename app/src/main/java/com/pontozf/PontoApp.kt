package com.pontozf

import android.app.Application
import android.os.Build
import android.util.Log
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Nome do arquivo onde o último travamento fica registrado (mostrado na próxima abertura). */
const val ARQUIVO_CRASH = "ultimo-crash.txt"

/** Histórico permanente de erros, com data e hora (Ajustes → Histórico de erros). */
const val ARQUIVO_HISTORICO_ERROS = "historico-erros.txt"

/** Tamanho máximo do histórico antes de recomeçar (evita crescer para sempre). */
private const val LIMITE_HISTORICO_BYTES = 512 * 1024L

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
                val relatorio = buildString {
                    appendLine(
                        "Easy Point ${BuildConfig.VERSION_NAME} — " +
                            "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}"
                    )
                    appendLine()
                    appendLine(Log.getStackTraceString(erro))
                }

                // Caixa-preta da próxima abertura
                File(filesDir, ARQUIVO_CRASH).writeText(relatorio)

                // Histórico permanente, com carimbo de data e hora
                val carimbo = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                val historico = File(filesDir, ARQUIVO_HISTORICO_ERROS)
                if (historico.exists() && historico.length() > LIMITE_HISTORICO_BYTES) {
                    historico.delete()
                }
                historico.appendText("═══ $carimbo ═══\n$relatorio\n")
            } catch (_: Exception) {
                // Sem espaço/sem acesso: segue para o encerramento normal.
            }
            padrao?.uncaughtException(thread, erro)
        }
    }
}

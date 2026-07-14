package com.pontozf

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

private const val CANAL_AVISOS = "avisos"
private const val TIPO_INTERVALO = "intervalo"
private const val TIPO_FIM = "fim"
private const val ID_ALARME_INTERVALO = 1
private const val ID_ALARME_FIM = 2

/** Dispara as notificações agendadas: fim do intervalo e fim da jornada. */
class LembreteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val gerente = context.getSystemService(NotificationManager::class.java)

        gerente.createNotificationChannel(
            NotificationChannel(
                CANAL_AVISOS,
                "Avisos de jornada",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
            }
        )

        val tipo = intent.getStringExtra("tipo") ?: TIPO_INTERVALO
        val (titulo, texto) = when (tipo) {
            TIPO_FIM ->
                "Hora de ir embora! 🏠" to
                    "Sua jornada de 8h48 está completa. Registre a saída e bom descanso!"
            else ->
                "Hora de voltar!" to
                    // O texto vem pronto de quem agendou (varia com a duração
                    // do intervalo configurada em Ajustes: 1h01 ou 1h30).
                    (intent.getStringExtra("texto")
                        ?: "Seu intervalo terminou. Hora de voltar ao trabalho!")
        }

        val abrirApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificacao = NotificationCompat.Builder(context, CANAL_AVISOS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()

        try {
            gerente.notify(if (tipo == TIPO_FIM) ID_ALARME_FIM else ID_ALARME_INTERVALO, notificacao)
        } catch (_: SecurityException) {
            // Permissão de notificação negada: nada a fazer.
        }
    }
}

private fun pendenteDoLembrete(context: Context, tipo: String, id: Int, texto: String = ""): PendingIntent {
    val intent = Intent(context, LembreteReceiver::class.java)
        .putExtra("tipo", tipo)
        .putExtra("texto", texto)
    return PendingIntent.getBroadcast(
        context,
        id,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

private fun agendar(context: Context, quando: Long, pendente: PendingIntent) {
    val alarmes = context.getSystemService(AlarmManager::class.java)
    val aoTocar = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    try {
        alarmes.setAlarmClock(AlarmManager.AlarmClockInfo(quando, aoTocar), pendente)
    } catch (_: SecurityException) {
        // Sem permissão de alarme exato (endurecida no Android 14+):
        // agenda com janela aproximada de 10 min — o aviso não pode derrubar o app.
        alarmes.setWindow(AlarmManager.RTC_WAKEUP, quando, 10 * 60 * 1000L, pendente)
    }
}

/** Agenda o aviso de fim do intervalo (1 minuto antes do retorno previsto). */
fun agendarLembreteIntervalo(context: Context, quando: Long, texto: String) {
    agendar(context, quando, pendenteDoLembrete(context, TIPO_INTERVALO, ID_ALARME_INTERVALO, texto))
}

fun cancelarLembreteIntervalo(context: Context) {
    context.getSystemService(AlarmManager::class.java)
        .cancel(pendenteDoLembrete(context, TIPO_INTERVALO, ID_ALARME_INTERVALO))
}

/** Agenda o aviso de fim da jornada ("hora de ir embora") para a previsão calculada. */
fun agendarLembreteFim(context: Context, quando: Long) {
    agendar(context, quando, pendenteDoLembrete(context, TIPO_FIM, ID_ALARME_FIM))
}

fun cancelarLembreteFim(context: Context) {
    context.getSystemService(AlarmManager::class.java)
        .cancel(pendenteDoLembrete(context, TIPO_FIM, ID_ALARME_FIM))
}

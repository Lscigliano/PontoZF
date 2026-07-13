package com.pontozf

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

private const val CANAL_LEMBRETES = "lembretes"
private const val ID_NOTIFICACAO_INTERVALO = 1
private const val ID_ALARME_INTERVALO = 1

/** Dispara a notificação de fim do intervalo agendada por [agendarLembreteIntervalo]. */
class LembreteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val gerente = context.getSystemService(NotificationManager::class.java)

        gerente.createNotificationChannel(
            NotificationChannel(
                CANAL_LEMBRETES,
                "Lembretes de intervalo",
                NotificationManager.IMPORTANCE_HIGH
            )
        )

        val horaRetorno = intent.getStringExtra("horaRetorno") ?: ""
        val abrirApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificacao = NotificationCompat.Builder(context, CANAL_LEMBRETES)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Hora de voltar!")
            .setContentText("Seu intervalo completou 1 hora. Retorno liberado a partir das $horaRetorno.")
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()

        try {
            gerente.notify(ID_NOTIFICACAO_INTERVALO, notificacao)
        } catch (_: SecurityException) {
            // Permissão de notificação negada: nada a fazer.
        }
    }
}

/**
 * Agenda a notificação de fim do intervalo para [quando] (1 hora após a saída).
 * Usa setAlarmClock, que é exato e não exige permissão especial.
 */
fun agendarLembreteIntervalo(context: Context, quando: Long, horaRetorno: String) {
    val alarmes = context.getSystemService(AlarmManager::class.java)
    val intent = Intent(context, LembreteReceiver::class.java)
        .putExtra("horaRetorno", horaRetorno)
    val pendente = PendingIntent.getBroadcast(
        context,
        ID_ALARME_INTERVALO,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val aoTocar = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    alarmes.setAlarmClock(AlarmManager.AlarmClockInfo(quando, aoTocar), pendente)
}

/** Cancela o lembrete pendente (ex.: retorno registrado ou saída excluída). */
fun cancelarLembreteIntervalo(context: Context) {
    val alarmes = context.getSystemService(AlarmManager::class.java)
    val pendente = PendingIntent.getBroadcast(
        context,
        ID_ALARME_INTERVALO,
        Intent(context, LembreteReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    alarmes.cancel(pendente)
}

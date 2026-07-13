package com.pontozf.ui

import com.pontozf.data.Ponto
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val LocalePtBr = Locale("pt", "BR")
internal val FormatoHora = DateTimeFormatter.ofPattern("HH:mm", LocalePtBr)

internal fun Long.paraDataLocal(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

internal fun Long.paraHora(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(FormatoHora)

/** Soma os períodos trabalhados (pares entrada/saída) de um dia. */
internal fun totalTrabalhado(pontosDoDia: List<Ponto>): Duration {
    val ordenados = pontosDoDia.sortedBy { it.timestamp }
    var totalMs = 0L
    for (i in ordenados.indices step 2) {
        if (i + 1 < ordenados.size) {
            totalMs += ordenados[i + 1].timestamp - ordenados[i].timestamp
        }
    }
    return Duration.ofMillis(totalMs)
}

internal fun Duration.formatar(): String = "%dh %02dmin".format(toMinutes() / 60, toMinutes() % 60)

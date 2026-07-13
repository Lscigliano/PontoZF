package com.pontozf.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pontozf.data.Ponto
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val FormatoDataCurta = DateTimeFormatter.ofPattern("dd/MM/yyyy (EEE)", LocalePtBr)
private val FormatoMes = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", LocalePtBr)
private val FormatoDiaMes = DateTimeFormatter.ofPattern("dd/MM", LocalePtBr)

/**
 * Monta o texto do mês e abre o menu de compartilhar do Android
 * (WhatsApp, e-mail, salvar em arquivo etc.).
 */
private fun exportarMes(contexto: Context, mes: YearMonth, porDia: Map<LocalDate, List<Ponto>>) {
    val nomeMes = mes.format(FormatoMes).replaceFirstChar { it.uppercase(LocalePtBr) }
    val texto = buildString {
        appendLine("PontoZF — $nomeMes")
        appendLine()
        porDia.toSortedMap(compareBy { it }).forEach { (data, pontosDoDia) ->
            val horarios = pontosDoDia.sortedBy { it.timestamp }
                .joinToString(" | ") { it.timestamp.paraHora() }
            appendLine("${data.format(FormatoDiaMes)}: $horarios (${totalTrabalhado(pontosDoDia).formatar()})")
        }
        val totalDoMes = porDia.values
            .fold(java.time.Duration.ZERO) { acc, dia -> acc + totalTrabalhado(dia) }
        appendLine()
        appendLine("Total do mês: ${totalDoMes.formatar()}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "PontoZF — $nomeMes")
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    contexto.startActivity(Intent.createChooser(intent, "Exportar $nomeMes"))
}

/**
 * Lista de todos os pontos agrupados por mês, do mais recente ao mais antigo.
 * Meses sem nenhum registro não aparecem (ex.: férias).
 */
@Composable
fun ConteudoHistorico(pontos: List<Ponto>, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val porMes = pontos
        .groupBy { YearMonth.from(it.timestamp.paraDataLocal()) }
        .toSortedMap(compareByDescending { it })

    if (porMes.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Nenhum ponto registrado ainda.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        porMes.forEach { (mes, pontosDoMes) ->
            val porDia = pontosDoMes
                .groupBy { it.timestamp.paraDataLocal() }
                .toSortedMap(compareByDescending { it })
            val totalDoMes = porDia.values
                .fold(java.time.Duration.ZERO) { acc, dia -> acc + totalTrabalhado(dia) }

            item(key = "mes-$mes") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        mes.format(FormatoMes).replaceFirstChar { it.uppercase(LocalePtBr) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        totalDoMes.formatar(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { exportarMes(contexto, mes, porDia) }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Exportar mês",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            porDia.forEach { (data, pontosDoDia) ->
                item(key = "dia-$data") {
                    CartaoDia(data = data, pontosDoDia = pontosDoDia.sortedBy { it.timestamp })
                }
            }
        }
    }
}

@Composable
private fun CartaoDia(data: LocalDate, pontosDoDia: List<Ponto>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    data.format(FormatoDataCurta).replaceFirstChar { it.uppercase(LocalePtBr) },
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    totalTrabalhado(pontosDoDia).formatar(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                pontosDoDia.joinToString("  •  ") { it.timestamp.paraHora() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

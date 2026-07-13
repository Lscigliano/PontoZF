package com.pontozf.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pontozf.data.Ponto

/** Jornada padrão de 8 horas, com intervalo mínimo de 1h04 após 4h de turno. */
private const val JORNADA_MS = 8 * 60 * 60 * 1000L
private const val TURNO_ATE_INTERVALO_MS = 4 * 60 * 60 * 1000L
private const val INTERVALO_PADRAO_MS = 64 * 60 * 1000L

private val VerdeEntrada = Color(0xFF43A047)

internal sealed interface ItemLinha {
    data class Registro(val ponto: Ponto, val entrada: Boolean, val destaque: Boolean) : ItemLinha
    data class Trecho(val texto: String) : ItemLinha
    data class Previsao(val timestamp: Long, val rotulo: String, val entrada: Boolean) : ItemLinha
}

private fun formatarHm(ms: Long): String {
    val minutos = ms / 60_000
    return "%02dh %02dm".format(minutos / 60, minutos % 60)
}

/**
 * Monta a linha do tempo do dia: os pontos batidos, a duração de cada
 * turno/intervalo entre eles e as previsões (cinza) do restante da jornada,
 * baseadas na jornada padrão de 8h com 1h de intervalo.
 */
internal fun montarLinhaDoTempo(pontosHoje: List<Ponto>): List<ItemLinha> {
    val ordenados = pontosHoje.sortedBy { it.timestamp }
    if (ordenados.isEmpty()) return emptyList()

    val itens = mutableListOf<ItemLinha>()
    ordenados.forEachIndexed { i, ponto ->
        if (i > 0) {
            val duracao = ponto.timestamp - ordenados[i - 1].timestamp
            val nome = if (i % 2 == 1) "Turno" else "Intervalo"
            itens += ItemLinha.Trecho("$nome de ${formatarHm(duracao)}")
        }
        itens += ItemLinha.Registro(
            ponto = ponto,
            entrada = i % 2 == 0,
            destaque = i == ordenados.lastIndex
        )
    }

    val ultimo = ordenados.last()
    val trabalhado = totalTrabalhado(ordenados).toMillis()
    val trabalhando = ordenados.size % 2 == 1

    if (trabalhando && ordenados.size == 1) {
        // Início da jornada: prevê intervalo e fim do dia.
        val saidaPrevista = ultimo.timestamp + TURNO_ATE_INTERVALO_MS
        val retornoPrevisto = saidaPrevista + INTERVALO_PADRAO_MS
        val restante = JORNADA_MS - TURNO_ATE_INTERVALO_MS
        itens += ItemLinha.Trecho("Turno de ${formatarHm(TURNO_ATE_INTERVALO_MS)}")
        itens += ItemLinha.Previsao(saidaPrevista, "Previsão de saída para o intervalo", entrada = false)
        itens += ItemLinha.Trecho("Intervalo de ${formatarHm(INTERVALO_PADRAO_MS)}")
        itens += ItemLinha.Previsao(retornoPrevisto, "Previsão de retorno do intervalo", entrada = true)
        itens += ItemLinha.Trecho("Turno de ${formatarHm(restante)}")
        itens += ItemLinha.Previsao(retornoPrevisto + restante, "Previsão de fim da jornada", entrada = false)
    } else if (trabalhando) {
        // Voltou do intervalo: prevê só o fim da jornada.
        val restante = JORNADA_MS - trabalhado
        if (restante > 0) {
            itens += ItemLinha.Trecho("Turno de ${formatarHm(restante)}")
            itens += ItemLinha.Previsao(ultimo.timestamp + restante, "Previsão de fim da jornada", entrada = false)
        }
    } else {
        // Saiu (intervalo ou fim do dia): se ainda falta jornada, assume intervalo.
        val restante = JORNADA_MS - trabalhado
        if (restante > 0) {
            val retornoPrevisto = ultimo.timestamp + INTERVALO_PADRAO_MS
            itens += ItemLinha.Trecho("Intervalo de ${formatarHm(INTERVALO_PADRAO_MS)}")
            itens += ItemLinha.Previsao(retornoPrevisto, "Previsão de retorno do intervalo", entrada = true)
            itens += ItemLinha.Trecho("Turno de ${formatarHm(restante)}")
            itens += ItemLinha.Previsao(retornoPrevisto + restante, "Previsão de fim da jornada", entrada = false)
        }
    }
    return itens
}

@Composable
internal fun LinhaDoTempoDia(itens: List<ItemLinha>, aoExcluir: (Ponto) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        itens.forEachIndexed { i, item ->
            val primeiro = i == 0
            val ultimo = i == itens.lastIndex
            when (item) {
                is ItemLinha.Registro -> NoRegistro(item, primeiro, ultimo, aoExcluir)
                is ItemLinha.Trecho -> TrechoDuracao(item.texto)
                is ItemLinha.Previsao -> NoPrevisao(item, ultimo)
            }
        }
    }
}

/** Linha com a coluna da esquerda (linha vertical + nó opcional) e o conteúdo à direita. */
@Composable
private fun LinhaComTrilha(
    primeiro: Boolean,
    ultimo: Boolean,
    no: (@Composable () -> Unit)?,
    conteudo: @Composable RowScope.() -> Unit
) {
    val corLinha = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(if (primeiro) Color.Transparent else corLinha)
            )
            no?.invoke()
            Box(
                Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(if (ultimo) Color.Transparent else corLinha)
            )
        }
        conteudo()
    }
}

@Composable
private fun NoRegistro(
    item: ItemLinha.Registro,
    primeiro: Boolean,
    ultimo: Boolean,
    aoExcluir: (Ponto) -> Unit
) {
    val cor = if (item.entrada) VerdeEntrada else MaterialTheme.colorScheme.error
    LinhaComTrilha(
        primeiro = primeiro,
        ultimo = ultimo,
        no = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        if (item.destaque) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(cor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.entrada) {
                        Icons.AutoMirrored.Filled.ArrowForward
                    } else {
                        Icons.AutoMirrored.Filled.ArrowBack
                    },
                    contentDescription = if (item.entrada) "Entrada" else "Saída",
                    tint = cor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                item.ponto.timestamp.paraHora(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { aoExcluir(item.ponto) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
        }
    }
}

@Composable
private fun NoPrevisao(item: ItemLinha.Previsao, ultimo: Boolean) {
    val corCinza = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
    LinhaComTrilha(
        primeiro = false,
        ultimo = ultimo,
        no = {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(corCinza.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.entrada) {
                        Icons.AutoMirrored.Filled.ArrowForward
                    } else {
                        Icons.AutoMirrored.Filled.ArrowBack
                    },
                    contentDescription = null,
                    tint = corCinza,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, top = 10.dp, bottom = 10.dp)
        ) {
            Text(
                item.timestamp.paraHora(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = corCinza
            )
            Text(
                item.rotulo,
                style = MaterialTheme.typography.bodySmall,
                color = corCinza
            )
        }
    }
}

@Composable
private fun TrechoDuracao(texto: String) {
    LinhaComTrilha(primeiro = false, ultimo = false, no = null) {
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 12.dp, top = 14.dp, bottom = 14.dp)
        )
    }
}

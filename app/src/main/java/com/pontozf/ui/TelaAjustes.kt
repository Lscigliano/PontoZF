package com.pontozf.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pontozf.ARQUIVO_HISTORICO_ERROS
import com.pontozf.BuildConfig
import com.pontozf.INTERVALO_1H30_MIN
import com.pontozf.INTERVALO_1H_MIN
import com.pontozf.Tema
import com.pontozf.data.Ponto
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val RotulosPeriodos =
    listOf("Entrada", "Saída almoço", "Retorno almoço", "Saída trabalho")
private val FormatoDataAjuste = DateTimeFormatter.ofPattern("dd/MM/yyyy", LocalePtBr)
private val FormatoHoraAjuste = DateTimeFormatter.ofPattern("HH:mm", LocalePtBr)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConteudoAjustes(
    tema: Tema,
    aoDefinirTema: (Tema) -> Unit,
    biometriaAtiva: Boolean,
    aoDefinirBiometria: (Boolean) -> Unit,
    intervaloMinutos: Int,
    aoDefinirIntervalo: (Int) -> Unit,
    pontos: List<Ponto>,
    aoAjustarDia: (LocalDate, List<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarData by remember { mutableStateOf(false) }
    var dataEscolhida by remember { mutableStateOf<LocalDate?>(null) }
    var indiceEditando by remember { mutableStateOf<Int?>(null) }
    var erroAjuste by remember { mutableStateOf<String?>(null) }
    var mostrarErros by remember { mutableStateOf(false) }
    val horarios = remember { mutableStateListOf<LocalTime?>(null, null, null, null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Registro",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconeTonal(Icons.Default.Fingerprint)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Confirmação por digital", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Pede sua digital antes de registrar o ponto",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(checked = biometriaAtiva, onCheckedChange = aoDefinirBiometria)
            }
        }

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconeTonal(Icons.Default.Restaurant)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Ajuste de intervalo", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Duração do seu intervalo de almoço",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                OpcaoIntervalo(
                    rotulo = "1 hora",
                    descricao = "Retorno liberado a partir de 1h01 de intervalo",
                    selecionado = intervaloMinutos != INTERVALO_1H30_MIN,
                    aoSelecionar = { aoDefinirIntervalo(INTERVALO_1H_MIN) }
                )
                OpcaoIntervalo(
                    rotulo = "1 hora e 30 minutos",
                    descricao = "Pode voltar antes se quiser, nunca antes de 1h01",
                    selecionado = intervaloMinutos == INTERVALO_1H30_MIN,
                    aoSelecionar = { aoDefinirIntervalo(INTERVALO_1H30_MIN) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { mostrarData = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconeTonal(Icons.Default.EditCalendar)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Ajustar pontos do dia", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Corrija os 4 períodos: entrada, saída e retorno do almoço, saída",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Aparência",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                OpcaoTema("Seguir o sistema", Icons.Default.BrightnessAuto, tema == Tema.SISTEMA) {
                    aoDefinirTema(Tema.SISTEMA)
                }
                OpcaoTema("Tema claro", Icons.Default.LightMode, tema == Tema.CLARO) {
                    aoDefinirTema(Tema.CLARO)
                }
                OpcaoTema("Tema escuro", Icons.Default.DarkMode, tema == Tema.ESCURO) {
                    aoDefinirTema(Tema.ESCURO)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Sobre",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val contexto = LocalContext.current
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                LinhaSobre(
                    icone = Icons.Default.Info,
                    titulo = "Easy Point",
                    detalhe = "Versão ${BuildConfig.VERSION_NAME} (release v${BuildConfig.VERSION_NAME})"
                )
                LinhaSobre(
                    icone = Icons.Default.Person,
                    titulo = "Desenvolvedor",
                    detalhe = "Leonardo Scigliano"
                )
                LinhaSobre(
                    icone = Icons.Default.Schedule,
                    titulo = "Jornada configurada",
                    detalhe = if (intervaloMinutos == INTERVALO_1H30_MIN) {
                        "8h48 por dia • intervalo de 1h30 (mínimo de 1h01)"
                    } else {
                        "8h48 por dia • intervalo mínimo de 1h01"
                    }
                )
                LinhaSobre(
                    icone = Icons.Default.Code,
                    titulo = "Projeto no GitHub",
                    detalhe = "Código-fonte, versões e downloads",
                    aoClicar = {
                        contexto.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/Lscigliano/EasyPoint/releases")
                            )
                        )
                    }
                )
                LinhaSobre(
                    icone = Icons.Default.BugReport,
                    titulo = "Histórico de erros",
                    detalhe = "Registros de travamentos, com data e hora",
                    aoClicar = { mostrarErros = true }
                )
            }
        }
    }

    if (mostrarData) {
        val estadoData = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { mostrarData = false },
            confirmButton = {
                TextButton(onClick = {
                    val selecionado = estadoData.selectedDateMillis
                    if (selecionado != null) {
                        val data = Instant.ofEpochMilli(selecionado)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        val zona = ZoneId.systemDefault()
                        val doDia = pontos
                            .filter { it.timestamp.paraDataLocal() == data }
                            .sortedBy { it.timestamp }
                        for (i in 0..3) {
                            horarios[i] = doDia.getOrNull(i)?.let {
                                Instant.ofEpochMilli(it.timestamp).atZone(zona).toLocalTime()
                            }
                        }
                        erroAjuste = null
                        mostrarData = false
                        dataEscolhida = data
                    }
                }) { Text("Avançar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarData = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estadoData, title = {
                Text("Dia do ajuste", modifier = Modifier.padding(start = 24.dp, top = 16.dp))
            })
        }
    }

    dataEscolhida?.let { data ->
        AlertDialog(
            onDismissRequest = { dataEscolhida = null },
            title = { Text("Pontos de ${data.format(FormatoDataAjuste)}") },
            text = {
                Column {
                    RotulosPeriodos.forEachIndexed { i, rotulo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { indiceEditando = i }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(rotulo, fontWeight = FontWeight.SemiBold)
                                Text(
                                    horarios[i]?.format(FormatoHoraAjuste) ?: "Toque para definir",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (horarios[i] != null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    }
                                )
                            }
                            if (horarios[i] != null) {
                                IconButton(onClick = { horarios[i] = null }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Limpar $rotulo",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                    erroAjuste?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val preenchidos = horarios.takeWhile { it != null }.filterNotNull()
                    val zona = ZoneId.systemDefault()
                    val timestamps = preenchidos.map {
                        data.atTime(it).atZone(zona).toInstant().toEpochMilli()
                    }
                    when {
                        preenchidos.isEmpty() ->
                            erroAjuste = "Defina pelo menos a entrada."
                        horarios.drop(preenchidos.size).any { it != null } ->
                            erroAjuste = "Preencha os períodos em ordem, sem pular."
                        preenchidos.zipWithNext().any { (a, b) -> !b.isAfter(a) } ->
                            erroAjuste = "Os horários precisam estar em ordem crescente."
                        timestamps.any { it > System.currentTimeMillis() } ->
                            erroAjuste = "Não é possível definir horários no futuro."
                        else -> {
                            aoAjustarDia(data, timestamps)
                            dataEscolhida = null
                        }
                    }
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { dataEscolhida = null }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarErros) {
        val contextoErros = LocalContext.current
        val textoErros = remember {
            val arquivo = File(contextoErros.filesDir, ARQUIVO_HISTORICO_ERROS)
            if (arquivo.exists()) arquivo.readText() else ""
        }
        AlertDialog(
            onDismissRequest = { mostrarErros = false },
            title = { Text("Histórico de erros") },
            text = {
                if (textoErros.isBlank()) {
                    Text("Nenhum erro registrado até agora. 🎉")
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            textoErros,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (textoErros.isNotBlank()) {
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Easy Point — histórico de erros")
                            putExtra(Intent.EXTRA_TEXT, textoErros)
                        }
                        contextoErros.startActivity(
                            Intent.createChooser(intent, "Compartilhar histórico de erros")
                        )
                    }) { Text("Compartilhar") }
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarErros = false }) { Text("Fechar") }
            }
        )
    }

    indiceEditando?.let { i ->
        key(i) {
            val estadoHora = rememberTimePickerState(
                initialHour = horarios[i]?.hour ?: 8,
                initialMinute = horarios[i]?.minute ?: 0,
                is24Hour = true
            )
            AlertDialog(
                onDismissRequest = { indiceEditando = null },
                title = { Text(RotulosPeriodos[i]) },
                text = { TimeInput(state = estadoHora) },
                confirmButton = {
                    TextButton(onClick = {
                        horarios[i] = LocalTime.of(estadoHora.hour, estadoHora.minute)
                        erroAjuste = null
                        indiceEditando = null
                    }) { Text("Definir") }
                },
                dismissButton = {
                    TextButton(onClick = { indiceEditando = null }) { Text("Cancelar") }
                }
            )
        }
    }
}

/** Ícone dentro de um círculo tonal azul, padrão visual dos Ajustes. */
@Composable
private fun IconeTonal(icone: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun LinhaSobre(
    icone: ImageVector,
    titulo: String,
    detalhe: String,
    aoClicar: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (aoClicar != null) Modifier.clickable(onClick = aoClicar) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconeTonal(icone)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(titulo, fontWeight = FontWeight.SemiBold)
            Text(
                detalhe,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/** Linha de opção do intervalo: rótulo + explicação e botão de rádio. */
@Composable
private fun OpcaoIntervalo(
    rotulo: String,
    descricao: String,
    selecionado: Boolean,
    aoSelecionar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoSelecionar)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(rotulo, fontWeight = FontWeight.SemiBold)
            Text(
                descricao,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        RadioButton(selected = selecionado, onClick = aoSelecionar)
    }
}

@Composable
private fun OpcaoTema(
    rotulo: String,
    icone: ImageVector,
    selecionado: Boolean,
    aoSelecionar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoSelecionar)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.width(16.dp))
        Text(rotulo, Modifier.weight(1f))
        RadioButton(selected = selecionado, onClick = aoSelecionar)
    }
}

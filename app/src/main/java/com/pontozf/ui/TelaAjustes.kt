package com.pontozf.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pontozf.BuildConfig
import com.pontozf.Tema
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConteudoAjustes(
    tema: Tema,
    aoDefinirTema: (Tema) -> Unit,
    biometriaAtiva: Boolean,
    aoDefinirBiometria: (Boolean) -> Unit,
    aoAdicionarManual: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarData by remember { mutableStateOf(false) }
    var mostrarHora by remember { mutableStateOf(false) }
    var dataEscolhida by remember { mutableStateOf(LocalDate.now()) }

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
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
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
                Icon(
                    Icons.Default.MoreTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Adicionar registro manual", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Para pontos esquecidos — ex.: a entrada que não foi batida",
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
                    titulo = "PontoZF",
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
                    detalhe = "8h48 por dia • intervalo mínimo de 1h01"
                )
                LinhaSobre(
                    icone = Icons.Default.Code,
                    titulo = "Projeto no GitHub",
                    detalhe = "Código-fonte, versões e downloads",
                    aoClicar = {
                        contexto.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/Lscigliano/PontoZF/releases")
                            )
                        )
                    }
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
                        dataEscolhida = Instant.ofEpochMilli(selecionado)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        mostrarData = false
                        mostrarHora = true
                    }
                }) { Text("Avançar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarData = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estadoData, title = {
                Text("Dia do registro", modifier = Modifier.padding(start = 24.dp, top = 16.dp))
            })
        }
    }

    if (mostrarHora) {
        val estadoHora = rememberTimePickerState(is24Hour = true)
        AlertDialog(
            onDismissRequest = { mostrarHora = false },
            title = { Text("Horário do registro") },
            text = { TimePicker(state = estadoHora) },
            confirmButton = {
                TextButton(onClick = {
                    val timestamp = dataEscolhida
                        .atTime(estadoHora.hour, estadoHora.minute)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    aoAdicionarManual(timestamp)
                    mostrarHora = false
                }) { Text("Adicionar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarHora = false }) { Text("Cancelar") }
            }
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
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

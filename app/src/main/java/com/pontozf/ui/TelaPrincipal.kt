package com.pontozf.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pontozf.PontoViewModel
import com.pontozf.ResultadoRegistro
import com.pontozf.Tema
import com.pontozf.data.Ponto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val FormatoRelogio = DateTimeFormatter.ofPattern("HH:mm:ss", LocalePtBr)
private val FormatoData = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", LocalePtBr)

private enum class Aba { HOJE, HISTORICO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPrincipal(viewModel: PontoViewModel) {
    val pontos by viewModel.pontos.collectAsStateWithLifecycle()
    val tema by viewModel.tema.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()

    var abaAtual by remember { mutableStateOf(Aba.HOJE) }
    var avisoIntervalo by remember { mutableStateOf<Long?>(null) }
    var pontoParaExcluir by remember { mutableStateOf<Ponto?>(null) }
    var menuTemaAberto by remember { mutableStateOf(false) }

    fun registrar(forcar: Boolean = false) {
        viewModel.registrar(forcar) { resultado ->
            when (resultado) {
                is ResultadoRegistro.Sucesso -> escopo.launch {
                    snackbarHostState.showSnackbar("Ponto registrado!")
                }
                is ResultadoRegistro.ToqueDuplo -> escopo.launch {
                    snackbarHostState.showSnackbar("Ponto já registrado há menos de 1 minuto.")
                }
                is ResultadoRegistro.IntervaloCurto -> avisoIntervalo = resultado.minutosDescanso
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("PontoZF", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { menuTemaAberto = true }) {
                        Icon(
                            imageVector = when (tema) {
                                Tema.SISTEMA -> Icons.Default.BrightnessAuto
                                Tema.CLARO -> Icons.Default.LightMode
                                Tema.ESCURO -> Icons.Default.DarkMode
                            },
                            contentDescription = "Tema"
                        )
                    }
                    DropdownMenu(
                        expanded = menuTemaAberto,
                        onDismissRequest = { menuTemaAberto = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Seguir o sistema") },
                            leadingIcon = { Icon(Icons.Default.BrightnessAuto, null) },
                            onClick = { viewModel.definirTema(Tema.SISTEMA); menuTemaAberto = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Tema claro") },
                            leadingIcon = { Icon(Icons.Default.LightMode, null) },
                            onClick = { viewModel.definirTema(Tema.CLARO); menuTemaAberto = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Tema escuro") },
                            leadingIcon = { Icon(Icons.Default.DarkMode, null) },
                            onClick = { viewModel.definirTema(Tema.ESCURO); menuTemaAberto = false }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = abaAtual == Aba.HOJE,
                    onClick = { abaAtual = Aba.HOJE },
                    icon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                    label = { Text("Hoje") }
                )
                NavigationBarItem(
                    selected = abaAtual == Aba.HISTORICO,
                    onClick = { abaAtual = Aba.HISTORICO },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Histórico") }
                )
            }
        }
    ) { padding ->
        when (abaAtual) {
            Aba.HOJE -> ConteudoHoje(
                pontos = pontos,
                aoRegistrar = { registrar() },
                aoExcluir = { pontoParaExcluir = it },
                modifier = Modifier.padding(padding)
            )
            Aba.HISTORICO -> ConteudoHistorico(
                pontos = pontos,
                modifier = Modifier.padding(padding)
            )
        }
    }

    avisoIntervalo?.let { minutos ->
        AlertDialog(
            onDismissRequest = { avisoIntervalo = null },
            icon = { Icon(Icons.Default.WarningAmber, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Intervalo menor que 1 hora") },
            text = {
                Text(
                    "Você está voltando do intervalo com apenas $minutos minutos de descanso. " +
                        "A CLT exige no mínimo 1 hora de intervalo — voltar antes pode gerar " +
                        "problemas para você e para a empresa junto ao Ministério do Trabalho.\n\n" +
                        "Deseja registrar mesmo assim?"
                )
            },
            confirmButton = {
                TextButton(onClick = { avisoIntervalo = null; registrar(forcar = true) }) {
                    Text("Registrar assim mesmo")
                }
            },
            dismissButton = {
                TextButton(onClick = { avisoIntervalo = null }) { Text("Cancelar") }
            }
        )
    }

    pontoParaExcluir?.let { ponto ->
        AlertDialog(
            onDismissRequest = { pontoParaExcluir = null },
            title = { Text("Excluir ponto") },
            text = { Text("Excluir o registro das ${ponto.timestamp.paraHora()}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.excluir(ponto); pontoParaExcluir = null }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pontoParaExcluir = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ConteudoHoje(
    pontos: List<Ponto>,
    aoRegistrar: () -> Unit,
    aoExcluir: (Ponto) -> Unit,
    modifier: Modifier = Modifier
) {
    val hoje = LocalDate.now()
    val pontosHoje = pontos.filter { it.timestamp.paraDataLocal() == hoje }.sortedBy { it.timestamp }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(16.dp)
    ) {
        item { Relogio() }

        item {
            Button(
                onClick = aoRegistrar,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(72.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("REGISTRAR PONTO", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Text(
                "Hoje",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                fontWeight = FontWeight.Bold
            )
        }

        if (pontosHoje.isEmpty()) {
            item {
                Text(
                    "Nenhum ponto registrado hoje.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            items(pontosHoje, key = { it.id }) { ponto ->
                val indice = pontosHoje.indexOf(ponto)
                CartaoPonto(
                    ponto = ponto,
                    entrada = indice % 2 == 0,
                    aoExcluir = { aoExcluir(ponto) }
                )
            }
            item {
                Text(
                    "Total trabalhado: ${totalTrabalhado(pontosHoje).formatar()}" +
                        if (pontosHoje.size % 2 != 0) " (em andamento)" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun Relogio() {
    var agora by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            agora = LocalDateTime.now()
            delay(1_000)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            agora.format(FormatoRelogio),
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            agora.format(FormatoData).replaceFirstChar { it.uppercase(LocalePtBr) },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CartaoPonto(ponto: Ponto, entrada: Boolean, aoExcluir: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (entrada) Icons.Default.Login else Icons.Default.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (entrada) "Entrada" else "Saída",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    ponto.timestamp.paraHora(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = aoExcluir) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

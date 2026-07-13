package com.pontozf.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.pontozf.data.Ponto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val FormatoRelogio = DateTimeFormatter.ofPattern("HH:mm:ss", LocalePtBr)
private val FormatoData = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", LocalePtBr)

private enum class Aba { HOJE, HISTORICO, AJUSTES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPrincipal(
    viewModel: PontoViewModel,
    autenticarBiometria: (aoResultado: (Boolean) -> Unit) -> Unit
) {
    val pontos by viewModel.pontos.collectAsStateWithLifecycle()
    val tema by viewModel.tema.collectAsStateWithLifecycle()
    val biometriaAtiva by viewModel.biometria.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()

    var abaAtual by remember { mutableStateOf(Aba.HOJE) }
    var bloqueio by remember { mutableStateOf<ResultadoRegistro?>(null) }
    var pontoParaExcluir by remember { mutableStateOf<Ponto?>(null) }

    fun registrar() {
        viewModel.registrar { resultado ->
            when (resultado) {
                is ResultadoRegistro.Sucesso -> escopo.launch {
                    snackbarHostState.showSnackbar("Ponto registrado!")
                }
                is ResultadoRegistro.ToqueDuplo -> escopo.launch {
                    snackbarHostState.showSnackbar("Ponto já registrado há menos de 1 minuto.")
                }
                is ResultadoRegistro.IntervaloCurto -> bloqueio = resultado
            }
        }
    }

    /** Fluxo do botão: se a confirmação por digital estiver ativa, autentica antes. */
    fun registrarComConfirmacao() {
        if (biometriaAtiva) {
            autenticarBiometria { autenticado ->
                if (autenticado) {
                    registrar()
                } else {
                    escopo.launch { snackbarHostState.showSnackbar("Registro cancelado.") }
                }
            }
        } else {
            registrar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("PontoZF", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
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
                NavigationBarItem(
                    selected = abaAtual == Aba.AJUSTES,
                    onClick = { abaAtual = Aba.AJUSTES },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Ajustes") }
                )
            }
        }
    ) { padding ->
        when (abaAtual) {
            Aba.HOJE -> ConteudoHoje(
                pontos = pontos,
                aoRegistrar = { registrarComConfirmacao() },
                aoExcluir = { pontoParaExcluir = it },
                modifier = Modifier.padding(padding)
            )
            Aba.HISTORICO -> ConteudoHistorico(
                pontos = pontos,
                modifier = Modifier.padding(padding)
            )
            Aba.AJUSTES -> ConteudoAjustes(
                tema = tema,
                aoDefinirTema = viewModel::definirTema,
                biometriaAtiva = biometriaAtiva,
                aoDefinirBiometria = viewModel::definirBiometria,
                aoAdicionarManual = { timestamp ->
                    if (timestamp > System.currentTimeMillis()) {
                        escopo.launch {
                            snackbarHostState.showSnackbar("Não é possível adicionar um registro no futuro.")
                        }
                    } else {
                        viewModel.inserirManual(timestamp)
                        escopo.launch { snackbarHostState.showSnackbar("Registro adicionado.") }
                    }
                },
                modifier = Modifier.padding(padding)
            )
        }
    }

    bloqueio?.let { resultado ->
        val (titulo, texto) = when (resultado) {
            is ResultadoRegistro.IntervaloCurto ->
                "Retorno bloqueado" to
                    "O intervalo mínimo é de 1 hora e 1 minuto. " +
                    "Você poderá registrar o retorno a partir das ${resultado.liberadoEm.paraHora()}."
            else -> return@let
        }
        AlertDialog(
            onDismissRequest = { bloqueio = null },
            icon = { Icon(Icons.Default.WarningAmber, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(titulo) },
            text = { Text(texto) },
            confirmButton = {
                TextButton(onClick = { bloqueio = null }) { Text("Entendi") }
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
            item {
                LinhaDoTempoDia(
                    itens = montarLinhaDoTempo(pontosHoje),
                    aoExcluir = aoExcluir
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

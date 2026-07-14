package com.pontozf.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pontozf.BuildConfig
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
    val intervaloMinutos by viewModel.intervaloMinutos.collectAsStateWithLifecycle()
    val atualizacao by viewModel.atualizacao.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val escopo = rememberCoroutineScope()

    var abaAtual by remember { mutableStateOf(Aba.HOJE) }
    var bloqueio by remember { mutableStateOf<ResultadoRegistro?>(null) }

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
                title = { Text("Easy Point", fontWeight = FontWeight.Bold) },
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
                biometriaAtiva = biometriaAtiva,
                intervaloMinutos = intervaloMinutos,
                aoRegistrar = { registrarComConfirmacao() },
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
                intervaloMinutos = intervaloMinutos,
                aoDefinirIntervalo = viewModel::definirIntervalo,
                pontos = pontos,
                aoAjustarDia = { data, horarios ->
                    viewModel.ajustarDia(data, horarios)
                    escopo.launch { snackbarHostState.showSnackbar("Pontos do dia ajustados.") }
                },
                modifier = Modifier.padding(padding)
            )
        }
    }

    atualizacao?.let { nova ->
        AlertDialog(
            onDismissRequest = {
                if (!nova.obrigatoria) viewModel.dispensarAtualizacao()
            },
            icon = { Icon(Icons.Default.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(if (nova.obrigatoria) "Atualização obrigatória" else "Nova versão disponível")
            },
            text = {
                Text(
                    "A versão ${nova.versao} do Easy Point está disponível " +
                        "(você usa a ${BuildConfig.VERSION_NAME}). " +
                        (if (nova.obrigatoria) {
                            "A sua versão tem um problema importante já corrigido — " +
                                "é preciso atualizar para continuar usando o app."
                        } else {
                            "Deseja baixar agora?"
                        }) +
                        "\n\nO aplicativo será fechado para a instalação acontecer com segurança."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(nova.url)))
                    viewModel.dispensarAtualizacao()
                    // Fecha o app: instalar por cima de um app aberto derruba o
                    // processo no meio e assusta o usuário com aviso de erro.
                    (contexto as? android.app.Activity)?.finishAffinity()
                }) { Text("Baixar") }
            },
            dismissButton = {
                if (!nova.obrigatoria) {
                    TextButton(onClick = viewModel::dispensarAtualizacao) { Text("Depois") }
                }
            }
        )
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

}

@Composable
private fun ConteudoHoje(
    pontos: List<Ponto>,
    biometriaAtiva: Boolean,
    intervaloMinutos: Int,
    aoRegistrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hoje = LocalDate.now()
    val pontosHoje = pontos.filter { it.timestamp.paraDataLocal() == hoje }.sortedBy { it.timestamp }

    // Atualiza os cartões de resumo a cada 30s enquanto há turno aberto.
    var agora by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pontosHoje.size) {
        while (true) {
            agora = System.currentTimeMillis()
            delay(30_000)
        }
    }

    val trabalhado = trabalhadoComAndamento(pontosHoje, agora)
    val restanteMs = (JORNADA_MS - trabalhado.toMillis()).coerceAtLeast(0)
    val fimPrevisto = previsaoFimDaJornada(pontosHoje, intervaloMinutos * 60_000L)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(16.dp)
    ) {
        item { Relogio() }

        item {
            Spacer(Modifier.height(12.dp))
            ChipEstado(pontosHoje = pontosHoje, trabalhado = trabalhado)
        }

        item {
            Button(
                onClick = aoRegistrar,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(64.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 3.dp,
                    pressedElevation = 8.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (biometriaAtiva) Icons.Default.Fingerprint else Icons.Default.TouchApp,
                    contentDescription = null
                )
                Spacer(Modifier.width(12.dp))
                Text("REGISTRAR PONTO", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TileResumo("Trabalhado", trabalhado.formatar())
                TileResumo("Restante", java.time.Duration.ofMillis(restanteMs).formatar())
                TileResumo("Fim previsto", fimPrevisto?.paraHora() ?: "—")
            }
        }

        item {
            Text(
                "LINHA DO TEMPO",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                fontWeight = FontWeight.SemiBold
            )
        }

        if (pontosHoje.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Nenhum ponto registrado hoje.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                        LinhaDoTempoDia(itens = montarLinhaDoTempo(pontosHoje, intervaloMinutos * 60_000L))
                    }
                }
            }
        }
    }
}

/** Situação atual do dia, com cor semântica: verde trabalhando, âmbar em intervalo. */
@Composable
private fun ChipEstado(pontosHoje: List<Ponto>, trabalhado: java.time.Duration) {
    val ultimo = pontosHoje.lastOrNull()
    val (texto, cor) = when {
        ultimo == null ->
            "Nenhum registro hoje" to MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        pontosHoje.size % 2 == 1 ->
            "Trabalhando desde ${ultimo.timestamp.paraHora()}" to MaterialTheme.colorScheme.tertiary
        trabalhado.toMillis() >= JORNADA_MS ->
            "Jornada concluída" to MaterialTheme.colorScheme.primary
        else ->
            "Em intervalo desde ${ultimo.timestamp.paraHora()}" to Color(0xFFE08700)
    }
    Surface(shape = RoundedCornerShape(50), color = cor.copy(alpha = 0.12f)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(cor)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                texto,
                style = MaterialTheme.typography.labelLarge,
                color = cor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RowScope.TileResumo(titulo: String, valor: String) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                titulo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
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

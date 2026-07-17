package com.pontozf

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pontozf.data.Ponto
import com.pontozf.data.PontoDatabase
import com.pontozf.ui.previsaoFimDaJornada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val Context.dataStore by preferencesDataStore(name = "config")
private val CHAVE_TEMA = intPreferencesKey("tema")
private val CHAVE_BIOMETRIA = booleanPreferencesKey("biometria")
private val CHAVE_INTERVALO = intPreferencesKey("intervalo_minutos")

/**
 * Intervalo mínimo legal entre a saída para descanso e o retorno: 1 hora e
 * 1 minuto. Vale para TODOS, independentemente da duração configurada em
 * Ajustes — menos de 1 hora de descanso gera problema trabalhista.
 */
const val INTERVALO_MINIMO_MS = 61 * 60 * 1000L

/** Durações de intervalo configuráveis em Ajustes (em minutos). */
const val INTERVALO_1H_MIN = 61
const val INTERVALO_1H30_MIN = 90

/** Bloqueio de toque duplo acidental no botão. */
const val BLOQUEIO_TOQUE_DUPLO_MS = 60 * 1000L

/** Entrada, Saída almoço, Retorno almoço, Saída trabalho: só 4 pontos por dia. */
const val MAXIMO_PONTOS_DIA = 4

enum class Tema { SISTEMA, CLARO, ESCURO }

/**
 * Nova versão do app disponível no GitHub. [obrigatoria] quando a release
 * declara "minVersao: X.Y" e a versão instalada está abaixo dela.
 */
data class Atualizacao(val versao: String, val url: String, val obrigatoria: Boolean)

sealed interface ResultadoRegistro {
    data object Sucesso : ResultadoRegistro
    data object ToqueDuplo : ResultadoRegistro
    /** Retorno de intervalo com menos de 1h01 de descanso: bloqueado. */
    data class IntervaloCurto(val liberadoEm: Long) : ResultadoRegistro
    /** Os 4 pontos do dia (Entrada, Saída almoço, Retorno almoço, Saída) já foram batidos. */
    data object LimiteDiarioAtingido : ResultadoRegistro
}

class PontoViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = PontoDatabase.get(app).pontoDao()
    private val dataStore = app.dataStore

    private val _atualizacao = MutableStateFlow<Atualizacao?>(null)

    /** Preenchido quando há uma release mais nova que a versão instalada. */
    val atualizacao: StateFlow<Atualizacao?> = _atualizacao

    /** Chamada sempre que o app volta ao primeiro plano (ver TelaPrincipal). */
    fun verificarAtualizacao() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val conexao = URL("https://api.github.com/repos/Lscigliano/EasyPoint/releases/latest")
                    .openConnection() as HttpURLConnection
                conexao.connectTimeout = 10_000
                conexao.readTimeout = 10_000
                val corpo = conexao.inputStream.bufferedReader().use { it.readText() }
                conexao.disconnect()

                val json = JSONObject(corpo)
                val versaoRemota = json.getString("tag_name").removePrefix("v")
                val assets = json.optJSONArray("assets")
                val url = if (assets != null && assets.length() > 0) {
                    assets.getJSONObject(0).getString("browser_download_url")
                } else {
                    json.getString("html_url")
                }
                // "minVersao: X.Y" na descrição da release torna a atualização
                // obrigatória para quem estiver abaixo dessa versão.
                val minVersao = Regex("minVersao[:=]\\s*([0-9][0-9.]*)")
                    .find(json.optString("body") ?: "")
                    ?.groupValues?.get(1)
                val obrigatoria = minVersao != null &&
                    versaoMaisNova(minVersao, BuildConfig.VERSION_NAME)
                if (versaoMaisNova(versaoRemota, BuildConfig.VERSION_NAME)) {
                    _atualizacao.value = Atualizacao(versaoRemota, url, obrigatoria)
                }
            } catch (_: Exception) {
                // Sem internet ou GitHub indisponível: segue sem aviso.
            }
        }
    }

    fun dispensarAtualizacao() {
        _atualizacao.value = null
    }

    private fun versaoMaisNova(remota: String, local: String): Boolean {
        val r = remota.split(".").map { it.toIntOrNull() ?: 0 }
        val l = local.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    val pontos: StateFlow<List<Ponto>> = dao.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tema: StateFlow<Tema> = dataStore.data
        .map { prefs -> Tema.entries.getOrElse(prefs[CHAVE_TEMA] ?: 0) { Tema.SISTEMA } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Tema.SISTEMA)

    fun definirTema(tema: Tema) {
        viewModelScope.launch {
            dataStore.edit { it[CHAVE_TEMA] = tema.ordinal }
        }
    }

    /** Exigir confirmação por digital antes de registrar o ponto. */
    val biometria: StateFlow<Boolean> = dataStore.data
        .map { prefs -> prefs[CHAVE_BIOMETRIA] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun definirBiometria(ativa: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[CHAVE_BIOMETRIA] = ativa }
        }
    }

    /** Duração do intervalo configurada em Ajustes: 61 (padrão) ou 90 minutos. */
    val intervaloMinutos: StateFlow<Int> = dataStore.data
        .map { prefs -> prefs[CHAVE_INTERVALO] ?: INTERVALO_1H_MIN }
        .stateIn(viewModelScope, SharingStarted.Eagerly, INTERVALO_1H_MIN)

    /**
     * Troca a duração do intervalo e realinha os lembretes de hoje na hora:
     * o "Hora de voltar!" (se estiver em intervalo agora) e o "Hora de ir
     * embora!" (a previsão de fim da jornada muda junto com o intervalo).
     */
    fun definirIntervalo(minutos: Int) {
        viewModelScope.launch {
            dataStore.edit { it[CHAVE_INTERVALO] = minutos }

            val agora = System.currentTimeMillis()
            val pontosHoje = pontosDeHoje()

            // Está no intervalo do almoço agora (2º ponto foi o último):
            // reagenda o lembrete para a nova duração. Se o novo horário já
            // passou, só cancela — sem notificação atrasada do nada.
            if (pontosHoje.size == 2) {
                val saida = pontosHoje.last().timestamp
                val quando = saida + minutos * 60_000L - 60_000L
                if (quando > agora) {
                    agendarLembreteIntervalo(
                        getApplication(),
                        quando = quando,
                        texto = textoLembreteIntervalo(minutos, saida)
                    )
                } else {
                    cancelarLembreteIntervalo(getApplication())
                }
            }

            reagendarLembreteFim(pontosHoje, agora, minutos)
        }
    }

    /**
     * Registra o ponto com a hora atual do celular.
     *
     * Regras (bloqueios, sem exceção pelo botão):
     * - Menos de 1 minuto desde o último ponto: toque duplo acidental.
     * - Retorno de intervalo com menos de 1h01 de descanso: bloqueado.
     *   O bloqueio de 1h01 vale mesmo para quem configurou intervalo de 1h30
     *   — quem faz 1h30 pode voltar antes, mas nunca antes de 1h01.
     * - Mais de 4 pontos no dia: bloqueado. Bater de novo depois da saída
     *   reabria a previsão da jornada como se fosse uma nova entrada,
     *   esticando a linha do tempo. Saída batida depois do horário previsto
     *   já conta como hora extra sozinha, pela diferença real entre os pontos.
     *
     * Situações legítimas fora das regras (saída antecipada autorizada,
     * entrada esquecida etc.) são resolvidas pelo registro manual em Ajustes.
     */
    fun registrar(aoResultado: (ResultadoRegistro) -> Unit) {
        viewModelScope.launch {
            val agora = System.currentTimeMillis()
            val pontosHoje = pontosDeHoje()
            val ultimo = pontosHoje.lastOrNull()

            if (pontosHoje.size >= MAXIMO_PONTOS_DIA) {
                aoResultado(ResultadoRegistro.LimiteDiarioAtingido)
                return@launch
            }

            if (ultimo != null && agora - ultimo.timestamp < BLOQUEIO_TOQUE_DUPLO_MS) {
                aoResultado(ResultadoRegistro.ToqueDuplo)
                return@launch
            }

            val ehRetornoDeIntervalo = ultimo != null && pontosHoje.size % 2 == 0
            if (ehRetornoDeIntervalo) {
                val liberadoEm = ultimo!!.timestamp + INTERVALO_MINIMO_MS
                if (agora < liberadoEm) {
                    aoResultado(ResultadoRegistro.IntervaloCurto(liberadoEm))
                    return@launch
                }
            }

            dao.inserir(Ponto(timestamp = agora))

            // Saída para o intervalo (2º ponto do dia): lembra de voltar
            // 1 minuto antes do fim do intervalo configurado.
            if (pontosHoje.size + 1 == 2) {
                val minutos = intervaloMinutos.value
                agendarLembreteIntervalo(
                    getApplication(),
                    quando = agora + minutos * 60_000L - 60_000L,
                    texto = textoLembreteIntervalo(minutos, saidaIntervalo = agora)
                )
            } else {
                cancelarLembreteIntervalo(getApplication())
            }

            reagendarLembreteFim(pontosHoje + Ponto(timestamp = agora), agora, intervaloMinutos.value)

            aoResultado(ResultadoRegistro.Sucesso)
        }
    }

    /** Pontos registrados hoje, em ordem. */
    private suspend fun pontosDeHoje(): List<Ponto> {
        val zona = ZoneId.systemDefault()
        val inicio = LocalDate.now(zona).atStartOfDay(zona).toInstant().toEpochMilli()
        val fim = LocalDate.now(zona).plusDays(1).atStartOfDay(zona).toInstant().toEpochMilli() - 1
        return dao.entre(inicio, fim)
    }

    /** Texto do lembrete "Hora de voltar!", conforme a duração configurada. */
    private fun textoLembreteIntervalo(minutos: Int, saidaIntervalo: Long): String {
        val horaRetorno = Instant.ofEpochMilli(saidaIntervalo + minutos * 60_000L)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
        return if (minutos >= INTERVALO_1H30_MIN) {
            "Seu intervalo de 1h30 está acabando. Retorno previsto às $horaRetorno."
        } else {
            "Seu intervalo completou 1 hora. Retorno liberado a partir das $horaRetorno."
        }
    }

    /**
     * Ajuste dos 4 períodos de um dia (aba Ajustes): substitui todos os pontos
     * do dia pelos horários informados (Entrada, Saída almoço, Retorno almoço,
     * Saída trabalho — os preenchidos, em ordem).
     */
    fun ajustarDia(data: LocalDate, horarios: List<Long>) {
        viewModelScope.launch {
            val zona = ZoneId.systemDefault()
            val inicio = data.atStartOfDay(zona).toInstant().toEpochMilli()
            val fim = data.plusDays(1).atStartOfDay(zona).toInstant().toEpochMilli() - 1
            dao.excluirEntre(inicio, fim)
            horarios.sorted().forEach { dao.inserir(Ponto(timestamp = it)) }

            // Ajustou o dia de hoje: realinha o aviso de fim da jornada.
            if (data == LocalDate.now(zona)) {
                reagendarLembreteFim(
                    horarios.sorted().map { Ponto(timestamp = it) },
                    System.currentTimeMillis(),
                    intervaloMinutos.value
                )
            }
        }
    }

    /**
     * Mantém o aviso "hora de ir embora" alinhado à previsão de fim da jornada:
     * agenda quando há previsão futura, cancela quando a jornada foi concluída.
     */
    private fun reagendarLembreteFim(pontosDoDia: List<Ponto>, agora: Long, intervaloMin: Int) {
        val fimPrevisto = previsaoFimDaJornada(pontosDoDia, intervaloMin * 60_000L)
        if (fimPrevisto != null && fimPrevisto > agora) {
            agendarLembreteFim(getApplication(), fimPrevisto)
        } else {
            cancelarLembreteFim(getApplication())
        }
    }
}

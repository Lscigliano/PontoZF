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

/** Intervalo mínimo entre a saída para descanso e o retorno: 1 hora e 1 minuto. */
const val INTERVALO_MINIMO_MS = 61 * 60 * 1000L

/** Bloqueio de toque duplo acidental no botão. */
const val BLOQUEIO_TOQUE_DUPLO_MS = 60 * 1000L

enum class Tema { SISTEMA, CLARO, ESCURO }

/** Nova versão do app disponível no GitHub. */
data class Atualizacao(val versao: String, val url: String)

sealed interface ResultadoRegistro {
    data object Sucesso : ResultadoRegistro
    data object ToqueDuplo : ResultadoRegistro
    /** Retorno de intervalo com menos de 1h01 de descanso: bloqueado. */
    data class IntervaloCurto(val liberadoEm: Long) : ResultadoRegistro
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
                val conexao = URL("https://api.github.com/repos/Lscigliano/PontoZF/releases/latest")
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
                if (versaoMaisNova(versaoRemota, BuildConfig.VERSION_NAME)) {
                    _atualizacao.value = Atualizacao(versaoRemota, url)
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

    /**
     * Registra o ponto com a hora atual do celular.
     *
     * Regras (bloqueios, sem exceção pelo botão):
     * - Menos de 1 minuto desde o último ponto: toque duplo acidental.
     * - Retorno de intervalo com menos de 1h01 de descanso: bloqueado.
     *
     * Situações legítimas fora das regras (saída antecipada autorizada,
     * entrada esquecida etc.) são resolvidas pelo registro manual em Ajustes.
     */
    fun registrar(aoResultado: (ResultadoRegistro) -> Unit) {
        viewModelScope.launch {
            val agora = System.currentTimeMillis()
            val zona = ZoneId.systemDefault()
            val inicioDoDia = LocalDate.now(zona).atStartOfDay(zona).toInstant().toEpochMilli()
            val fimDoDia = LocalDate.now(zona).plusDays(1).atStartOfDay(zona).toInstant().toEpochMilli() - 1
            val pontosHoje = dao.entre(inicioDoDia, fimDoDia)
            val ultimo = pontosHoje.lastOrNull()

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

            // Saída para o intervalo (2º ponto do dia): lembra de voltar em 1 hora.
            if (pontosHoje.size + 1 == 2) {
                val horaRetorno = Instant.ofEpochMilli(agora + INTERVALO_MINIMO_MS)
                    .atZone(zona)
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
                agendarLembreteIntervalo(
                    getApplication(),
                    quando = agora + 60 * 60 * 1000L,
                    horaRetorno = horaRetorno
                )
            } else {
                cancelarLembreteIntervalo(getApplication())
            }

            aoResultado(ResultadoRegistro.Sucesso)
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
        }
    }

    fun excluir(ponto: Ponto) {
        viewModelScope.launch { dao.excluir(ponto) }
    }
}

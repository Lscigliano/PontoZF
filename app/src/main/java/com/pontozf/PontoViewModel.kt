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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private val Context.dataStore by preferencesDataStore(name = "config")
private val CHAVE_TEMA = intPreferencesKey("tema")
private val CHAVE_BIOMETRIA = booleanPreferencesKey("biometria")

/** Intervalo mínimo entre a saída para descanso e o retorno: 1 hora e 4 minutos. */
const val INTERVALO_MINIMO_MS = 64 * 60 * 1000L

/** Bloqueio de toque duplo acidental no botão. */
const val BLOQUEIO_TOQUE_DUPLO_MS = 60 * 1000L

enum class Tema { SISTEMA, CLARO, ESCURO }

sealed interface ResultadoRegistro {
    data object Sucesso : ResultadoRegistro
    data object ToqueDuplo : ResultadoRegistro
    /** Retorno de intervalo com menos de 1h04 de descanso: bloqueado. */
    data class IntervaloCurto(val liberadoEm: Long) : ResultadoRegistro
}

class PontoViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = PontoDatabase.get(app).pontoDao()
    private val dataStore = app.dataStore

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
     * - Retorno de intervalo com menos de 1h04 de descanso: bloqueado.
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
            aoResultado(ResultadoRegistro.Sucesso)
        }
    }

    /** Registro manual (aba Ajustes): insere um ponto esquecido com data/hora escolhidas. */
    fun inserirManual(timestamp: Long) {
        viewModelScope.launch { dao.inserir(Ponto(timestamp = timestamp)) }
    }

    fun excluir(ponto: Ponto) {
        viewModelScope.launch { dao.excluir(ponto) }
    }
}

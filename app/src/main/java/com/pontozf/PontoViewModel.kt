package com.pontozf

import android.app.Application
import android.content.Context
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

/** Intervalo mínimo entre a saída para descanso e o retorno (CLT: 1 hora de almoço). */
const val INTERVALO_MINIMO_MS = 60 * 60 * 1000L

/** Bloqueio de toque duplo acidental no botão. */
const val BLOQUEIO_TOQUE_DUPLO_MS = 60 * 1000L

enum class Tema { SISTEMA, CLARO, ESCURO }

sealed interface ResultadoRegistro {
    data object Sucesso : ResultadoRegistro
    data object ToqueDuplo : ResultadoRegistro
    /** Retorno de intervalo com menos de 1 hora de descanso. */
    data class IntervaloCurto(val minutosDescanso: Long) : ResultadoRegistro
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

    /**
     * Registra o ponto com a hora atual do celular.
     *
     * Regras:
     * - Menos de 1 minuto desde o último ponto: bloqueado (toque duplo acidental).
     * - Retorno de intervalo (3º, 5º... ponto do dia) com menos de 1 hora de
     *   descanso: pede confirmação, pois fere o intervalo mínimo da CLT.
     */
    fun registrar(forcar: Boolean = false, aoResultado: (ResultadoRegistro) -> Unit) {
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

            val ehRetornoDeIntervalo = pontosHoje.isNotEmpty() && pontosHoje.size % 2 == 0
            if (!forcar && ehRetornoDeIntervalo && ultimo != null) {
                val descanso = agora - ultimo.timestamp
                if (descanso < INTERVALO_MINIMO_MS) {
                    aoResultado(ResultadoRegistro.IntervaloCurto(descanso / 60_000))
                    return@launch
                }
            }

            dao.inserir(Ponto(timestamp = agora))
            aoResultado(ResultadoRegistro.Sucesso)
        }
    }

    fun excluir(ponto: Ponto) {
        viewModelScope.launch { dao.excluir(ponto) }
    }
}

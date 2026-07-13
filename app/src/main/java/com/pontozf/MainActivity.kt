package com.pontozf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pontozf.ui.TelaPrincipal
import com.pontozf.ui.theme.PontoZFTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PontoViewModel = viewModel()
            val tema by viewModel.tema.collectAsStateWithLifecycle()
            val temaEscuro = when (tema) {
                Tema.SISTEMA -> isSystemInDarkTheme()
                Tema.CLARO -> false
                Tema.ESCURO -> true
            }
            PontoZFTheme(temaEscuro = temaEscuro) {
                TelaPrincipal(viewModel)
            }
        }
    }
}

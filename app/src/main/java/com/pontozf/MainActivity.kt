package com.pontozf

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pontozf.ui.TelaPrincipal
import com.pontozf.ui.theme.PontoZFTheme
import java.io.File

class MainActivity : FragmentActivity() {

    private var viewModel: PontoViewModel? = null

    private val pedirNotificacao =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Houve travamento na sessão anterior: mostra o relatório em vez do app.
        val arquivoCrash = File(filesDir, ARQUIVO_CRASH)
        if (arquivoCrash.exists()) {
            val relatorio = arquivoCrash.readText()
            arquivoCrash.delete()
            setContent { TelaDeErro(relatorio) }
            return
        }

        val vm = ViewModelProvider(this)[PontoViewModel::class.java]
        viewModel = vm

        // Permissão para o lembrete de fim do intervalo (Android 13+).
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val tema by vm.tema.collectAsStateWithLifecycle()
            val temaEscuro = when (tema) {
                Tema.SISTEMA -> isSystemInDarkTheme()
                Tema.CLARO -> false
                Tema.ESCURO -> true
            }
            PontoZFTheme(temaEscuro = temaEscuro) {
                TelaPrincipal(vm, autenticarBiometria = ::autenticar)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Verifica nova versão sempre que o app volta ao primeiro plano.
        viewModel?.verificarAtualizacao()
    }

    /**
     * Pede a digital do usuário, sempre mostrando o leitor na tela.
     * Só libera sem digital se o aparelho não tiver biometria disponível
     * (o registro não pode ficar impossível).
     */
    private fun autenticar(aoResultado: (Boolean) -> Unit) {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    aoResultado(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_NO_BIOMETRICS,
                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_HW_UNAVAILABLE -> aoResultado(true)
                        else -> aoResultado(false)
                    }
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirmar registro de ponto")
            .setSubtitle("Encoste o dedo no leitor para confirmar")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(info)
    }
}

/** Tela mínima com o relatório do travamento anterior e botão de compartilhar. */
@Composable
private fun TelaDeErro(relatorio: String) {
    val contexto = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            "O PontoZF travou na última abertura",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Envie o relatório abaixo para o desenvolvedor (botão Compartilhar). " +
                "Feche e abra o app de novo para usá-lo normalmente.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "PontoZF — relatório de erro")
                    putExtra(Intent.EXTRA_TEXT, relatorio)
                }
                contexto.startActivity(Intent.createChooser(intent, "Compartilhar relatório"))
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Compartilhar relatório") }
        Spacer(Modifier.height(12.dp))
        SelectionContainer {
            Text(
                relatorio,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        }
    }
}

package com.pontozf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val EsquemaClaro = lightColorScheme(
    primary = AzulPrimario,
    onPrimary = Branco,
    primaryContainer = AzulContainer,
    onPrimaryContainer = AzulEscuro,
    secondary = AzulEscuro,
    onSecondary = Branco,
    background = CinzaFundo,
    onBackground = TextoEscuro,
    surface = Branco,
    onSurface = TextoEscuro,
    surfaceVariant = AzulContainer,
    onSurfaceVariant = AzulEscuro
)

private val EsquemaEscuro = darkColorScheme(
    primary = AzulClaro,
    onPrimary = AzulEscuro,
    primaryContainer = AzulContainerEscuro,
    onPrimaryContainer = AzulClaro,
    secondary = AzulClaro,
    onSecondary = AzulEscuro,
    background = FundoEscuro,
    onBackground = TextoClaro,
    surface = SuperficieEscura,
    onSurface = TextoClaro,
    surfaceVariant = AzulContainerEscuro,
    onSurfaceVariant = AzulClaro
)

@Composable
fun PontoZFTheme(
    temaEscuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (temaEscuro) EsquemaEscuro else EsquemaClaro,
        typography = Typography(),
        content = content
    )
}

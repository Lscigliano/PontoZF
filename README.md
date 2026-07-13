# PontoZF

Aplicativo Android para registro de ponto pessoal — rápido, sem propagandas e sempre disponível.

## O que ele faz

- **Registrar Ponto com um toque**: grava exatamente a hora atual do celular (ex.: `08:00`, `12:00`, `13:05`).
- **Aviso de intervalo curto**: se você sair para o almoço e tentar voltar com menos de **1 hora** de descanso (ex.: sair 12:00 e voltar 12:40), o app avisa que isso fere o intervalo mínimo da CLT e pede confirmação antes de registrar.
- **Proteção contra toque duplo**: registros com menos de 1 minuto de diferença são bloqueados.
- **Total trabalhado por dia**: soma automática dos períodos (entrada/saída), sem precisar informar valor de hora.
- **Histórico**: todos os dias anteriores ficam salvos no aparelho (banco local, funciona offline).
- **Tema azul e branco**, com opção de **tema escuro** (ou seguir o sistema).

## Tecnologias

- Kotlin + Jetpack Compose (Material 3)
- Room (banco de dados local SQLite)
- DataStore (preferência de tema)
- Android 8.0+ (minSdk 26)

## Como compilar

1. Instale o [Android Studio](https://developer.android.com/studio) (ele instala o JDK e o Android SDK automaticamente).
2. Abra o Android Studio → **Open** → selecione esta pasta do projeto.
3. Aguarde a sincronização do Gradle (primeira vez demora alguns minutos).
4. Conecte o celular com **Depuração USB** ativada (ou use um emulador) e clique em **Run ▶**.

Para gerar um APK instalável: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
O arquivo fica em `app/build/outputs/apk/debug/app-debug.apk` — basta copiar para o celular e instalar.

## Estrutura do projeto

```
app/src/main/java/com/pontozf/
├── MainActivity.kt          # Ponto de entrada, aplica o tema
├── PontoViewModel.kt        # Regras de negócio (registro, validação de intervalo, tema)
├── data/
│   ├── Ponto.kt             # Entidade (id + timestamp)
│   ├── PontoDao.kt          # Consultas ao banco
│   └── PontoDatabase.kt     # Configuração do Room
└── ui/
    ├── TelaPrincipal.kt     # Relógio, botão, lista do dia e histórico
    └── theme/               # Cores azul/branco + tema escuro
```

# PontoZF

Aplicativo Android para registro de ponto pessoal — rápido, sem propagandas e sempre disponível.

📥 **Download da versão mais recente:** [Releases](https://github.com/Lscigliano/PontoZF/releases) — baixe o `.apk` no celular, toque no arquivo e instale. Atualizações instalam por cima sem perder o histórico.

## Funcionalidades

### Registro de ponto (aba Hoje)
- **Registrar Ponto com um toque**: grava exatamente a hora atual do celular (ex.: `08:00`, `12:00`, `13:05`).
- **Confirmação por digital (opcional)**: ícone de impressão digital na barra superior liga/desliga a exigência de biometria; com ela ativa, o registro só é gravado após reconhecer a digital. Se o aparelho não tiver biometria cadastrada, o registro é liberado normalmente.
- **Aviso de intervalo curto**: se você sair para o almoço e tentar voltar com menos de **1 hora** de descanso (ex.: sair 12:00 e voltar 12:40), o app avisa que isso fere o intervalo mínimo da CLT e pede confirmação antes de registrar.
- **Proteção contra toque duplo**: registros com menos de 1 minuto de diferença são bloqueados.
- **Relógio ao vivo** com data por extenso em português.

### Linha do tempo do dia (aba Hoje)
- Pontos do dia exibidos como **linha do tempo vertical**: entrada (seta verde) e saída (seta vermelha) como nós, com o último ponto batido destacado por um anel azul.
- **Duração de cada trecho** entre os nós: "Turno de 04h 00m", "Intervalo de 01h 00m".
- **Previsões em cinza** do restante da jornada (baseadas na jornada padrão CLT de 8h + 1h de intervalo):
  - Bateu só a entrada → previsão de saída para o intervalo, de retorno e de fim da jornada;
  - No intervalo → previsão de retorno e de fim da jornada;
  - Voltou do intervalo → previsão de fim da jornada;
  - Completou as 8h → sem previsões.
- **Total trabalhado no dia** somado automaticamente (pares entrada/saída), com indicação de "(em andamento)" quando há turno aberto.
- **Excluir registro**: lixeira em cada ponto, com confirmação.

### Histórico (aba Histórico)
- Todos os registros **agrupados por mês**, do mais recente ao mais antigo — meses sem nenhum ponto não aparecem (ex.: férias).
- **Total de horas por mês** e **por dia**, com os horários batidos de cada dia.
- **Exportar mês**: ícone de compartilhar ao lado de cada mês gera um texto com os registros dia a dia e os totais, enviável por WhatsApp, e-mail ou salvo em arquivo.

### Geral
- **Dados 100% locais**: tudo fica salvo no aparelho (banco SQLite via Room), funciona offline e não depende de servidor.
- **Tema azul e branco**, com opção de **tema escuro** ou de **seguir o sistema** (menu na barra superior).
- Preferências (tema, biometria) persistidas entre sessões.

## Histórico de versões

| Versão | Novidades |
|---|---|
| **1.3** | Confirmação por digital no registro; exportação do histórico por mês; build movido para fora do OneDrive |
| **1.2** | Linha do tempo do dia na aba Hoje, com previsões de intervalo e fim de jornada |
| **1.1** | Aba Histórico com pontos agrupados por mês e navegação inferior (Hoje / Histórico) |
| **1.0** | Registro de ponto, aviso de intervalo curto (CLT), proteção contra toque duplo, total do dia, temas |

## Tecnologias

- Kotlin + Jetpack Compose (Material 3)
- Room (banco de dados local SQLite)
- DataStore (preferências: tema e biometria)
- AndroidX Biometric (confirmação por digital)
- Android 8.0+ (minSdk 26)

## Como compilar

1. Instale o [Android Studio](https://developer.android.com/studio) (ele instala o JDK e o Android SDK automaticamente).
2. Abra o Android Studio → **Open** → selecione esta pasta do projeto.
3. Aguarde a sincronização do Gradle (primeira vez demora alguns minutos).
4. Conecte o celular com **Depuração USB** ativada (ou use um emulador) e clique em **Run ▶**.

Para gerar um APK instalável: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.

> **Onde o APK é gerado:** a pasta de build fica **fora do projeto**, em
> `%LOCALAPPDATA%\PontoZF-build\app\outputs\apk\debug\app-debug.apk`
> (o projeto mora no OneDrive, que travava os arquivos temporários do build —
> ver `build.gradle.kts`). O script de build também deixa uma cópia
> `PontoZF.apk` na raiz do projeto.

### Compilando pela linha de comando (máquina com Sophos/antivírus corporativo)

Antivírus corporativos podem travar o cache do Gradle durante o build
(erro "Could not move temporary workspace"). Use o script que contorna isso:

```bash
./compilar-apk.sh   # no Git Bash, dentro da pasta do projeto
```

Ele repete o build completando manualmente as movimentações de cache que o
antivírus impediu, até o APK sair. Outras particularidades desta configuração:

- `android.overridePathCheck=true` no `gradle.properties`: necessário porque o
  caminho do projeto contém acentos ("Área de Trabalho").
- Se o caminho com acentos causar problemas, compile por uma *junction* sem
  acentos: `mklink /J C:\Users\SEU_USUARIO\PontoZF "caminho\real\do\projeto"`.
- O `local.properties` (não versionado) deve apontar para o SDK com barras
  normais, ex.: `sdk.dir=C:/Users/SEU_USUARIO/AppData/Local/Android/Sdk`.

## Recuperação em outro computador

O repositório contém tudo para recompilar o app do zero (código, wrapper do
Gradle e script de build). O único arquivo externo é a **chave de assinatura**
(`C:\Users\<usuário>\.android\debug.keystore`): sem ela, o APK gerado num PC
novo não instala por cima do app já existente no celular (seria preciso
desinstalar, perdendo o histórico). Mantenha um backup da chave e copie-a de
volta para `.android\` no computador novo antes de compilar.

## Estrutura do projeto

```
app/src/main/java/com/pontozf/
├── MainActivity.kt          # Ponto de entrada, aplica o tema, autenticação por digital
├── PontoViewModel.kt        # Regras de negócio (registro, validação de intervalo, tema, biometria)
├── data/
│   ├── Ponto.kt             # Entidade (id + timestamp)
│   ├── PontoDao.kt          # Consultas ao banco
│   └── PontoDatabase.kt     # Configuração do Room
└── ui/
    ├── TelaPrincipal.kt     # Abas Hoje/Histórico, relógio, botão de registro, diálogos
    ├── LinhaDoTempo.kt      # Linha do tempo do dia (nós, trechos e previsões)
    ├── TelaHistorico.kt     # Histórico por mês + exportação
    ├── Comum.kt             # Formatadores e cálculo de horas compartilhados
    └── theme/               # Cores azul/branco + tema escuro
```

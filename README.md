# PontoZF

Aplicativo Android para registro de ponto pessoal — rápido, sem propagandas e sempre disponível.

📥 **Download da versão mais recente:** [Releases](https://github.com/Lscigliano/PontoZF/releases) — baixe o `.apk` no celular, toque no arquivo e instale. Atualizações instalam por cima sem perder o histórico.

## Funcionalidades

### Registro de ponto (aba Hoje)
- **Registrar Ponto com um toque**: grava exatamente a hora atual do celular (ex.: `08:00`, `12:00`, `13:05`).
- **Confirmação por digital (opcional)**: com a opção ativa (aba Ajustes), o leitor de digital aparece na tela e o ponto só é gravado após reconhecer a digital. Se o aparelho não tiver biometria disponível, o registro é liberado normalmente.
- **Bloqueio de retorno de intervalo**: o retorno do almoço exige no mínimo **1 hora e 1 minuto** de descanso (saiu 12:00 → só volta a partir de 13:01). Antes disso o registro é bloqueado, sem exceção — o app informa o horário liberado.
- **Proteção contra toque duplo**: registros com menos de 1 minuto de diferença são bloqueados.
- **Relógio ao vivo** com data por extenso em português.

### Linha do tempo do dia (aba Hoje)
- Pontos do dia exibidos como **linha do tempo vertical**: entrada (seta verde) e saída (seta vermelha) como nós, com o último ponto batido destacado por um anel azul.
- **Duração de cada trecho** entre os nós: "Turno de 04h 00m", "Intervalo de 01h 00m".
- **Previsões em cinza** do restante da jornada (baseadas na jornada de **8h48** — compensação do sábado — com intervalo de 1h01 após 4h de turno):
  - Bateu só a entrada → previsão de saída para o intervalo, de retorno e de fim da jornada;
  - No intervalo → previsão de retorno e de fim da jornada;
  - Voltou do intervalo → previsão de fim da jornada;
  - Completou as 8h48 → sem previsões.
- **Total trabalhado no dia** somado automaticamente (pares entrada/saída), com indicação de "(em andamento)" quando há turno aberto.
- **Excluir registro**: lixeira em cada ponto, com confirmação.

### Histórico (aba Histórico)
- Todos os registros **agrupados por mês**, do mais recente ao mais antigo — meses sem nenhum ponto não aparecem (ex.: férias).
- **Total de horas por mês** e **por dia**, com os horários batidos de cada dia.
- **Exportar mês**: ícone de compartilhar ao lado de cada mês gera um texto com os registros dia a dia e os totais, enviável por WhatsApp, e-mail ou salvo em arquivo.

### Ajustes (aba Ajustes)
- **Registro manual**: adiciona um ponto esquecido escolhendo data e hora (ex.: a entrada 08:01 que não foi batida) — é a válvula de escape para os bloqueios automáticos.
- **Confirmação por digital**: liga/desliga a exigência de biometria no registro.
- **Aparência**: tema claro, escuro ou seguir o sistema.
- **Sobre**: versão/release instalada, desenvolvedor (Leonardo Scigliano) e atalho para o projeto no GitHub.

### Geral
- **Lembrete de fim do intervalo**: ao bater a saída para o almoço, o app agenda uma notificação no celular para quando completar 1 hora — "Hora de voltar!", informando o horário liberado para o retorno.
- **Aviso de atualização**: ao abrir, o app consulta as releases do GitHub; se houver versão mais nova, oferece o download na hora (botão "Baixar" abre o APK novo direto).
- **Dados 100% locais**: os registros ficam salvos no aparelho (banco SQLite via Room) e funcionam offline — internet é usada só para checar atualização.
- Preferências (tema, biometria) persistidas entre sessões.

## Histórico de versões

| Versão | Novidades |
|---|---|
| **1.5.4** | Relatório de travamento: se o app fechar sozinho, a próxima abertura mostra o erro com botão de compartilhar |
| **1.5.3** | Correção de fechamento na abertura (crash da 1.5.2 no Samsung); checagem de atualização movida para onResume |
| **1.5.2** | Verificação de atualização a cada retorno ao app (não só na primeira abertura) |
| **1.5.1** | Seção Sobre nos Ajustes: versão instalada, desenvolvedor e link do GitHub |
| **1.5** | Aviso de nova versão (checagem no GitHub ao abrir); notificação "Hora de voltar!" 1h após a saída para o almoço |
| **1.4.1** | Jornada corrigida para 8h48 e intervalo mínimo para 1h01 |
| **1.4** | Aba Ajustes com registro manual (data/hora); bloqueio rígido do retorno de intervalo; leitor de digital sempre visível ao registrar |
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
├── PontoViewModel.kt        # Regras de negócio (registro, bloqueios, checagem de atualização)
├── LembreteReceiver.kt      # Alarme + notificação de fim do intervalo
├── data/
│   ├── Ponto.kt             # Entidade (id + timestamp)
│   ├── PontoDao.kt          # Consultas ao banco
│   └── PontoDatabase.kt     # Configuração do Room
└── ui/
    ├── TelaPrincipal.kt     # Abas Hoje/Histórico/Ajustes, relógio, botão de registro, diálogos
    ├── LinhaDoTempo.kt      # Linha do tempo do dia (nós, trechos e previsões)
    ├── TelaHistorico.kt     # Histórico por mês + exportação
    ├── TelaAjustes.kt       # Registro manual, biometria e tema
    ├── Comum.kt             # Formatadores e cálculo de horas compartilhados
    └── theme/               # Cores azul/branco + tema escuro
```

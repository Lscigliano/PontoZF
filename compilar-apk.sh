#!/bin/bash
# Compila o APK contornando os bloqueios de arquivo do antivírus (Sophos).
#
# O Sophos escaneia os arquivos que o Gradle extrai no cache e segura um
# bloqueio na hora em que o Gradle tenta renomear a pasta — o build falha com
# "Could not move temporary workspace". A pasta temporária fica completa no
# disco; este script termina a renomeação manualmente e tenta de novo.
#
# A pasta de build fica fora do OneDrive (ver build.gradle.kts), em
# %LOCALAPPDATA%\PontoZF-build, para o OneDrive não travar o build.
#
# Uso (no Git Bash): ./compilar-apk.sh

set -u

JAVA_HOME_PADRAO="C:\\Users\\leonardo.scigliano\\AppData\\Local\\Android\\jdk\\jdk-17.0.19+10"
export JAVA_HOME="${JAVA_HOME:-$JAVA_HOME_PADRAO}"

APK="$LOCALAPPDATA/PontoZF-build/app/outputs/apk/debug/app-debug.apk"

GRADLE_VERSAO=$(grep -o 'gradle-[0-9.]*-bin' gradle/wrapper/gradle-wrapper.properties | sed 's/gradle-\(.*\)-bin/\1/')
TRANS=~/.gradle/caches/"$GRADLE_VERSAO"/transforms

rm -f "$APK"

for i in 1 2 3 4 5 6 7 8; do
    # Completa as movimentações de cache que o antivírus impediu
    if [ -d "$TRANS" ]; then
        for d in "$TRANS"/????????????????????????????????-*; do
            [ -d "$d" ] || continue
            base=$(basename "$d")
            hash=${base:0:32}
            if [ ! -d "$TRANS/$hash" ]; then
                mv "$d" "$TRANS/$hash" && echo ">> cache recuperado: $hash"
            else
                rm -rf "$d"
            fi
        done
    fi

    echo "=== Tentativa $i ==="
    ./gradlew.bat assembleDebug --no-daemon
    if [ -f "$APK" ]; then
        cp "$APK" PontoZF.apk
        echo ""
        echo "APK gerado em: $APK"
        echo "Cópia na raiz do projeto: PontoZF.apk"
        exit 0
    fi
done

echo "Não foi possível gerar o APK após 8 tentativas." >&2
exit 1

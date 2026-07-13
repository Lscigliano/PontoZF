#!/bin/bash
# Compila o APK contornando os bloqueios de arquivo do antivírus (Sophos).
#
# O Sophos escaneia os arquivos que o Gradle extrai no cache e segura um
# bloqueio na hora em que o Gradle tenta renomear a pasta — o build falha com
# "Could not move temporary workspace". A pasta temporária fica completa no
# disco; este script termina a renomeação manualmente e tenta de novo.
#
# Uso (no Git Bash): ./compilar-apk.sh

set -u

JAVA_HOME_PADRAO="C:\\Users\\leonardo.scigliano\\AppData\\Local\\Android\\jdk\\jdk-17.0.19+10"
export JAVA_HOME="${JAVA_HOME:-$JAVA_HOME_PADRAO}"

GRADLE_VERSAO=$(grep -o 'gradle-[0-9.]*-bin' gradle/wrapper/gradle-wrapper.properties | sed 's/gradle-\(.*\)-bin/\1/')
TRANS=~/.gradle/caches/"$GRADLE_VERSAO"/transforms

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
    if [ -f app/build/outputs/apk/debug/app-debug.apk ]; then
        echo ""
        echo "APK gerado em: app/build/outputs/apk/debug/app-debug.apk"
        exit 0
    fi
done

echo "Não foi possível gerar o APK após 8 tentativas." >&2
exit 1

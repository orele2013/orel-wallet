#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
./gradlew assembleDebug testDebugUnitTest lintDebug --console=plain
mkdir -p artifacts
cp app/build/outputs/apk/debug/app-debug.apk artifacts/orel-wallet-demo.apk
(cd backend && npm test)
printf '\nAPK: artifacts/orel-wallet-demo.apk\n'

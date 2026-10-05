#!/usr/bin/env python3
"""Create the local release identity once; never output passwords or overwrite keys."""
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
folder = root / '.tools/signing'
store = folder / 'orel-wallet-release.p12'
properties = folder / 'release-signing.properties'
if store.exists() and properties.exists():
    print('La identidad de firma existente se conserva.')
    sys.exit(0)
if store.exists() or properties.exists():
    sys.exit('Identidad incompleta. Recupera la clave y sus propiedades; no se sobrescribirá.')
keytool = Path(os.environ['JAVA_HOME']) / 'bin/keytool' if os.environ.get('JAVA_HOME') else shutil.which('keytool')
if not keytool:
    sys.exit('Configura JAVA_HOME con JDK 17.')
folder.mkdir(parents=True, exist_ok=True)
folder.chmod(0o700)
password = secrets.token_urlsafe(48)
environment = dict(os.environ, OREL_RELEASE_PASSWORD=password)
subprocess.run([str(keytool), '-genkeypair', '-keystore', str(store), '-storetype', 'PKCS12',
                '-storepass:env', 'OREL_RELEASE_PASSWORD', '-keypass:env', 'OREL_RELEASE_PASSWORD',
                '-alias', 'orel-wallet', '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000',
                '-dname', 'CN=Orel Wallet', '-noprompt'], env=environment, check=True,
               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
store.chmod(0o600)
fd = os.open(properties, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
with os.fdopen(fd, 'w') as handle:
    handle.write(f'storeFile=.tools/signing/orel-wallet-release.p12\nstorePassword={password}\nkeyAlias=orel-wallet\nkeyPassword={password}\n')
print('Identidad release creada en .tools/signing. Guarda una copia privada de ambos archivos.')

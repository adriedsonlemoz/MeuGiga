# MeuGiga — Build Android

## APK automático

Ao enviar alterações para `main` ou `master`, o workflow **Android Signed APK** executa automaticamente.

Ele:

1. valida os quatro Secrets de assinatura;
2. restaura o keystore temporariamente no runner;
3. executa testes e lint de release;
4. gera `app-release.apk` assinado;
5. valida a assinatura com `apksigner`;
6. envia um artifact de backup chamado `MeuGiga-0.1.1-release-signed-apk`;
7. publica o APK diretamente em **GitHub Releases**, na versão de teste `meugiga-latest-build`.

Arquivo final:

`MeuGiga-0.1.1-release-signed.apk`

## Secrets obrigatórios

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

O workflow falha de forma explícita se qualquer Secret estiver ausente ou inválido.

## CI

O workflow `Android CI` executa apenas lint e testes. Ele não publica mais APK debug nem relatórios como artifacts, evitando confusão na tela de APKs/Artifacts.

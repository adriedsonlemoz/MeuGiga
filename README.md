# MeuGiga

**Seu consumo de dados, sem complicação.**

MeuGiga é um aplicativo Android nativo que acompanha exclusivamente o consumo de **dados móveis**. Não monitora Wi‑Fi, não usa VPN, não bloqueia internet, não limpa o aparelho e não inclui publicidade ou rastreamento.

## Estado da versão 0.1.3

- resumo compacto do ciclo atual com datas de início e fim;
- franquia limitada ou plano sem limite definido;
- total, restante, percentual, dias restantes, média diária, disponível por dia e previsão;
- download e upload separados;
- ranking por aplicativo, distinguindo apps removidos, tethering e sistema;
- busca de aplicativos por nome ou identificador de pacote;
- detalhes por aplicativo com histórico e intervalos fornecidos pelo Android;
- filtros Hoje, Ontem, 7 dias, 30 dias, Ciclo e Personalizado;
- média diária calculada pelos dias efetivamente cobertos pelo período, sem duplicar o dia final;
- cache e histórico local em Room;
- atualização em primeiro plano a cada 60 segundos;
- atualização periódica com WorkManager em 15, 30 ou 60 minutos;
- notificação persistente opcional e alertas percentuais configuráveis;
- tema claro, escuro ou seguindo o sistema;
- início do primeiro ciclo configurável, seguido da renovação recorrente;
- onboarding de três etapas;
- tela de novidades exibida uma vez após cada atualização instalada;
- nenhum cadastro, login ou servidor.

## Tecnologias

- Kotlin 2.2.21;
- Jetpack Compose + Material 3;
- MVVM com Coroutines e StateFlow;
- Room 2.8.4;
- DataStore 1.2.1;
- NetworkStatsManager;
- UsageStatsManager e AppOpsManager para verificar o acesso especial;
- PackageManager com visibilidade limitada a apps iniciáveis;
- WorkManager 2.11.1;
- Android Gradle Plugin 8.13.2 e Gradle 8.13;
- minSdk 23, compileSdk 36 e targetSdk 36.

O `targetSdk 36` atende à regra anunciada para novos apps e atualizações enviados à Google Play a partir de 31/08/2026.

## Arquitetura

```text
br.com.meugiga.app
├── data
│   ├── database       Room, entidades e consultas
│   ├── networkstats   acesso e leitura exclusiva de TYPE_MOBILE
│   └── repository     catálogo de apps e composição dos relatórios
├── domain             ciclos, períodos, métricas e agregação
├── notification       notificação de status e alertas
├── settings           preferências locais em DataStore
├── ui                  Compose, tema, componentes e telas
├── viewmodel           estado e coordenação da interface
├── worker              sincronização periódica econômica
└── utils               formatação de bytes e datas
```

Detalhes em [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Como o NetworkStatsManager é usado

Toda consulta de rede passa por `AndroidNetworkStatsReader` e usa somente:

```kotlin
ConnectivityManager.TYPE_MOBILE
```

O `subscriberId` é `null`, conforme permitido nas versões modernas do Android, para agregar as redes móveis acessíveis sem solicitar identificadores do SIM ou permissão de telefone. Wi‑Fi não participa de nenhuma consulta ou cálculo.

O Android reúne o histórico em buckets discretos. Esses buckets normalmente têm duração da ordem de horas e podem variar por fabricante. O MeuGiga:

- mantém o início e o fim reais de cada bucket;
- não interpola tráfego por minuto;
- atualiza totais com mais frequência do que o histórico detalhado;
- salva por `UID + início + fim`, substituindo o mesmo bucket em vez de duplicá-lo;
- mantém até 120 dias de dados locais;
- preserva o último cache válido quando o Android não entrega uma consulta de estatísticas;
- classifica os UIDs especiais de agregação, aplicativos removidos e tethering sem atribuí-los incorretamente ao Sistema Android.

Mais detalhes em [docs/NETWORK_STATS.md](docs/NETWORK_STATS.md).

## Permissões

### PACKAGE_USAGE_STATS

É uma autorização especial concedida manualmente em **Configurações > Acesso ao uso**. Permite consultar estatísticas de outros UIDs. O manifesto declara a intenção, mas somente o usuário pode concedê-la.

### POST_NOTIFICATIONS

Solicitada apenas quando o usuário ativa a notificação de acompanhamento ou os alertas no Android 13 ou superior.

### RECEIVE_BOOT_COMPLETED

Usada pelo WorkManager para restaurar o agendamento periódico após reinicializações.

O WorkManager também incorpora as permissões normais `WAKE_LOCK` e
`ACCESS_NETWORK_STATE` para executar e reagendar trabalho com segurança. Elas não
abrem telas de autorização, não permitem acesso à internet e não coletam conteúdo
de rede. A permissão e o serviço de primeiro plano trazidos genericamente pela
biblioteca são removidos no manifesto, pois o MeuGiga não promove seus workers a
foreground service.

O app **não** declara `INTERNET`, localização, telefone, VPN ou `QUERY_ALL_PACKAGES`.

## Compilar no Android Studio

1. Abra a pasta raiz `meugiga` no Android Studio compatível com AGP 8.13.
2. Instale o Android SDK Platform 36 quando solicitado.
3. Use JDK 17.
4. Sincronize o Gradle.
5. Execute a configuração `app` em um dispositivo Android 6.0 ou superior.

## Compilar pela linha de comando

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

APK gerado:

```text
app/build/outputs/apk/debug/app-debug.apk
```

O pacote de entrega também pode incluir em `artifacts/` arquivos de validação local. APKs, AABs e credenciais de assinatura não devem ser versionados no Git.

Builds `release` exigem assinatura. Com as variáveis de assinatura configuradas, gere APK + AAB com:

```bash
./gradlew assembleRelease bundleRelease
```

## Testes

```bash
./gradlew testDebugUnitTest
```

Os testes cobrem:

- conversão e formatação de bytes;
- MB/GB para bytes;
- percentual, restante, média diária e previsão;
- ciclos com dia 31 em meses curtos;
- início configurado do ciclo e transição para renovações recorrentes;
- plano sem limite;
- Hoje e período personalizado;
- intervalo invertido;
- preservação de download/upload ao agregar buckets;
- separação do histórico por UID.
- classificação dos UIDs especiais expostos pelo Android.

## GitHub Actions

`android-ci.yml` executa lint, testes e gera o APK debug em pushes, pull requests e execução manual.

`android-release.yml` gera APK release assinado e AAB manualmente, valida o keystore e verifica a assinatura do APK com `apksigner`. Configure estes GitHub Secrets:

- `ANDROID_KEYSTORE_BASE64`;
- `ANDROID_KEYSTORE_PASSWORD`;
- `ANDROID_KEY_ALIAS`;
- `ANDROID_KEY_PASSWORD`.

O keystore é reconstruído apenas no diretório temporário do runner e nunca deve ser commitado.

## Google Play

- applicationId: `br.com.meugiga.app`;
- ícone adaptativo e monocromático;
- release com minificação e redução de recursos;
- AAB preparado;
- política em [docs/PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md);
- sem `QUERY_ALL_PACKAGES`;
- sem SDKs de anúncios ou analytics.

Antes da publicação, hospede a política de privacidade em uma URL pública, crie e proteja o keystore definitivo e preencha a seção Segurança dos dados de acordo com o comportamento da versão publicada.

## Limitações conhecidas

- operadoras podem contabilizar tráfego de forma diferente do Android;
- tethering, apps removidos, UIDs compartilhados e tráfego do sistema podem aparecer agregados;
- alguns fabricantes atrasam ou restringem a atualização das estatísticas;
- WorkManager garante um intervalo mínimo, não um horário exato;
- a troca de SIM é tratada agregando todas as redes móveis disponíveis, sem coletar identificadores do assinante;
- o histórico anterior à instalação depende do que o Android ainda mantém no aparelho.

## Referências oficiais

- [NetworkStatsManager](https://developer.android.com/reference/android/app/usage/NetworkStatsManager)
- [Trabalho periódico com WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- [Visibilidade de pacotes](https://developer.android.com/training/package-visibility/declaring)
- [Requisitos de target API da Google Play](https://developer.android.com/google/play/requirements/target-sdk)

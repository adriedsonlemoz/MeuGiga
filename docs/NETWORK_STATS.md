# NetworkStatsManager: comportamento e limites

## Fonte

O MeuGiga usa `NetworkStatsManager` desde o Android 6.0 (API 23). O acesso a outros aplicativos depende de `PACKAGE_USAGE_STATS`, concedido na tela oficial do Android.

## Rede móvel

Todas as chamadas recebem `ConnectivityManager.TYPE_MOBILE`. Nenhuma chamada usa `TYPE_WIFI`.

O identificador do assinante é `null`. Desde Android 10, esse é o caminho compatível para consultar o total acessível das redes móveis sem pedir um identificador protegido do SIM.

## Totais e histórico

- `querySummaryForDevice`: total do aparelho no intervalo;
- `querySummary`: totais por UID;
- `queryDetails`: buckets históricos por UID.

As consultas rodam em `Dispatchers.IO`, pois o Android avisa que podem levar vários segundos.

## Buckets

O sistema não garante buckets de uma hora exata e não interpola buckets parciais. A interface usa rótulos como `08h`, `08–10h` ou intervalo entre datas conforme o início/fim real.

## UIDs

Um UID pode pertencer a mais de um pacote. Nessa situação, o MeuGiga mantém o consumo agregado do UID e indica que há pacotes compartilhados. UIDs do sistema, apps removidos ou pacotes invisíveis recebem um rótulo seguro.

## Diferenças para a operadora

O resultado pode divergir por:

- fuso e horário exato de renovação;
- arredondamento decimal;
- atraso do Android;
- tethering;
- tráfego contabilizado pela operadora fora do aparelho;
- redes/APNs especiais;
- restauração ou limpeza do histórico do sistema.

O MeuGiga é um monitor e não substitui o medidor oficial da operadora.


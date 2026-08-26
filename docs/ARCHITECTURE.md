# Arquitetura do MeuGiga

## Fluxo principal

1. `MainViewModel` calcula o ciclo e o período selecionado.
2. `UsageRepository` solicita uma leitura a `MobileUsageReader`.
3. `AndroidNetworkStatsReader` consulta apenas `TYPE_MOBILE` fora da thread principal.
4. Os totais por UID formam o ranking; os buckets preservam o histórico.
5. Room recebe upserts e caches por período.
6. `PackageCatalog` associa apenas aplicativos visíveis sem `QUERY_ALL_PACKAGES`.
7. O ViewModel publica um `MainUiState` imutável para o Compose.

## Responsabilidades

- `AndroidNetworkStatsReader`: comunicação exclusiva com as APIs de estatísticas.
- `UsageAccessController`: verificação e Intent do acesso especial.
- `MobileUsageDao`: persistência eficiente e limpeza de retenção.
- `UsageRepository`: cache, fallback e relatórios.
- `PlanCalculator`: ciclos e métricas do plano.
- `PeriodCalculator`: filtros e limites temporais.
- `UsageAggregator`: download/upload por intervalo e UID.
- `SettingsRepository`: plano, onboarding, tema, frequência e alertas.
- `UsageWorkScheduler`: periodicidade oficial do WorkManager.
- `UsageNotificationManager`: canais, status e limiares já enviados.
- `MainViewModel`: coordenação; não contém consultas SQL nem Android NetworkStats.

## Estratégia de bateria

- totais em primeiro plano: aproximadamente 60 segundos;
- histórico detalhado em primeiro plano: no máximo a cada 15 minutos, salvo atualização manual;
- segundo plano: 15, 30 ou 60 minutos, com padrão de 30;
- trabalho periódico exige bateria não baixa;
- nenhuma restrição de rede é aplicada, pois a fonte é local;
- nenhum foreground service é usado;
- nenhum polling continua após a Activity sair do estado iniciado.

## Persistência

`mobile_usage_buckets` usa a chave composta `(uid, startMillis, endMillis)`. Uma nova leitura do mesmo bucket atualiza seus bytes e não cria outra linha.

`usage_queries` e `usage_query_apps` armazenam o último resumo exato de cada chave de período. Isso permite abrir o app sem permissão ou quando o Android temporariamente não responde.

A retenção padrão é de 120 dias.

## Evolução

Migrações futuras do Room devem ser explícitas. Não usar `fallbackToDestructiveMigration`, pois o histórico é parte central do produto.


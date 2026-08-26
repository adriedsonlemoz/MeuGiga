# Changelog

## 0.1.1 — 26/08/2026

- indicadores compactos em três colunas no resumo, aplicativos, histórico e detalhes;
- período completo e dias restantes no card do ciclo atual;
- início do plano/ciclo configurável e aplicado aos cálculos;
- identificação correta de aplicativos removidos, tethering, agregados e sistema;
- tema escuro harmonizado, com superfícies, contrastes e bordas consistentes;
- cache do catálogo de aplicativos, persistência de identidades e consultas menos repetidas;
- agregação fora da thread principal e redução de consultas detalhadas desnecessárias;
- índices do Room e migração preservando o histórico existente;
- release no GitHub Actions gera APK assinado + AAB, exige os Secrets de assinatura e valida o APK com `apksigner`.

## 0.1.0 — 25/08/2026

- primeira versão funcional do MeuGiga;
- monitoramento exclusivo de dados móveis;
- consumo total, download, upload, restante, média e previsão;
- ranking e detalhes por aplicativo;
- filtros Hoje, Ontem, 7 dias, 30 dias, Ciclo e Personalizado;
- histórico local com Room;
- plano limitado ou sem limite definido;
- atualização em primeiro plano e WorkManager em segundo plano;
- notificação de acompanhamento e alertas percentuais opcionais;
- onboarding curto, temas claro/escuro/sistema e telas de privacidade/sobre;
- CI para lint, testes e APK debug;
- workflow manual para AAB release assinado por GitHub Secrets.

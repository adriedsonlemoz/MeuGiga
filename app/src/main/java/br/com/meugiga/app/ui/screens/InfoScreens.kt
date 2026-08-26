package br.com.meugiga.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.meugiga.app.BuildConfig
import br.com.meugiga.app.ui.components.MobileOnlyBadge

@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    InfoScreenScaffold("Privacidade", onBack, modifier) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { MobileOnlyBadge() }
            item {
                InfoCard(
                    "Tudo permanece no aparelho",
                    "O histórico de consumo, o plano e as preferências são processados e armazenados localmente. O MeuGiga não exige conta, login, e-mail ou telefone.",
                )
            }
            item {
                InfoCard(
                    "Sem rastreamento",
                    "O aplicativo não inclui publicidade, SDK de analytics nem permissão de localização. Também não possui permissão de internet própria para enviar seus dados a servidores.",
                )
            }
            item {
                InfoCard(
                    "Acesso às estatísticas de uso",
                    "Essa autorização especial do Android é usada somente para consultar os bytes recebidos e enviados pela rede móvel e associá-los aos aplicativos visíveis no aparelho.",
                )
            }
            item {
                InfoCard(
                    "Wi‑Fi ignorado",
                    "Nenhuma consulta, cálculo, gráfico, ranking ou alerta inclui tráfego Wi‑Fi.",
                )
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    InfoScreenScaffold("Sobre o MeuGiga", onBack, modifier) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("MeuGiga", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Seu consumo de dados, sem complicação.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { InfoCard("Versão", BuildConfig.VERSION_NAME) }
            item {
                InfoCard(
                    "Tecnologia",
                    "Kotlin, Jetpack Compose, Material 3, Room, DataStore, NetworkStatsManager e WorkManager.",
                )
            }
            item {
                InfoCard(
                    "Precisão dos dados",
                    "Os totais vêm das estatísticas mantidas pelo Android. A granularidade histórica varia conforme o aparelho e pode usar intervalos maiores que uma hora. O MeuGiga mostra esses intervalos sem interpolar dados inexistentes.",
                )
            }
            item {
                InfoCard(
                    "Operadora e aparelho",
                    "Pequenas diferenças em relação à operadora podem ocorrer por critérios de contabilização, arredondamento, tethering, horário do ciclo e atraso de atualização do sistema.",
                )
            }
            item {
                InfoCard(
                    "Compatibilidade",
                    "Android 6.0 ou superior. Preparado para target SDK 36 e publicação em formato AAB.",
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoScreenScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        content = content,
    )
}

@Composable
private fun InfoCard(title: String, text: String) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


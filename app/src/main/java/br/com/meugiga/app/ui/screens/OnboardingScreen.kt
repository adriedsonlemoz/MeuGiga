package br.com.meugiga.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.meugiga.app.ui.components.PlanDraft
import br.com.meugiga.app.ui.components.PlanFields
import java.time.LocalDate

@Composable
fun OnboardingScreen(
    hasUsageAccess: Boolean,
    onSavePlan: (Long?, Int, Boolean, LocalDate?) -> Unit,
    onOpenUsageAccess: () -> Unit,
    onFinish: () -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var plan by remember { mutableStateOf(PlanDraft()) }

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Text(
                "MeuGiga",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(0.35f))
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "onboarding-step",
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                when (page) {
                    0 -> IntroPage(
                        icon = Icons.Rounded.DataUsage,
                        title = "Controle seus dados móveis",
                        description = "Veja quanto você utiliza e quais aplicativos mais consomem sua internet.",
                    )
                    1 -> Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        PageHeading(
                            Icons.Rounded.CellTower,
                            "Configure seu plano",
                            "Informe sua franquia e o dia de renovação. Você poderá alterar tudo depois.",
                        )
                        PlanFields(plan, { plan = it })
                    }
                    else -> Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        PageHeading(
                            Icons.Rounded.Security,
                            "Permissão necessária",
                            "O Android exige acesso especial para mostrar o consumo de outros aplicativos. Nenhuma informação sai do aparelho.",
                        )
                        PermissionStatus(granted = hasUsageAccess)
                        if (!hasUsageAccess) {
                            Button(onClick = onOpenUsageAccess, modifier = Modifier.fillMaxWidth()) {
                                Text("Conceder acesso")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            StepDots(current = step, total = 3, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.size(18.dp))
            if (step < 2) {
                Button(
                    onClick = {
                        if (step == 1) {
                            onSavePlan(
                                plan.totalBytes,
                                plan.parsedDay!!,
                                plan.unlimited,
                                plan.cycleStartDate,
                            )
                        }
                        step++
                    },
                    enabled = step != 1 || plan.valid,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Continuar") }
            } else {
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text(if (hasUsageAccess) "Começar a usar" else "Continuar sem acesso")
                }
            }
            AnimatedVisibility(step > 0) {
                TextButton(onClick = { step-- }, modifier = Modifier.fillMaxWidth()) {
                    Text("Voltar")
                }
            }
        }
    }
}

@Composable
private fun IntroPage(icon: ImageVector, title: String, description: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(132.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(38.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(68.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.size(32.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.size(10.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageHeading(icon: ImageVector, title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PermissionStatus(granted: Boolean) {
    val background = if (granted) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val foreground = if (granted) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        if (granted) "Acesso concedido" else "Aguardando permissão",
        modifier = Modifier
            .background(background, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.titleMedium,
        color = foreground,
    )
}

@Composable
private fun StepDots(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { index ->
            Box(
                Modifier
                    .size(if (index == current) 24.dp else 8.dp, 8.dp)
                    .background(
                        if (index == current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape,
                    ),
            )
        }
    }
}

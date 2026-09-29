package com.mikhailskiy.finni.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mikhailskiy.finni.domain.GameSnapshot
import com.mikhailskiy.finni.domain.PeriodStatus
import com.mikhailskiy.finni.domain.PlanStatus
import com.mikhailskiy.finni.ui.components.CoinPill
import com.mikhailskiy.finni.ui.components.FinniCard
import com.mikhailskiy.finni.ui.components.ScreenHeader
import com.mikhailskiy.finni.ui.components.StepControl

@Composable
fun BudgetScreen(
    snapshot: GameSnapshot,
    isBusy: Boolean,
    onBack: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit,
    onCompletePeriod: () -> Unit,
    onShowSummary: () -> Unit,
) {
    val plan = snapshot.plan ?: return
    val period = snapshot.period ?: return
    var required by remember(plan.periodId) { mutableIntStateOf(if (plan.required == 0) 50 else plan.required) }
    var optional by remember(plan.periodId) { mutableIntStateOf(if (plan.optional == 0) 25 else plan.optional) }
    var savings by remember(plan.periodId) { mutableIntStateOf(if (plan.savings == 0) 20 else plan.savings) }
    val total = required + optional + savings
    val buffer = plan.baseAvailable - total

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { ScreenHeader("Три кармашка", "План бюджета · период ${period.number}", onBack) }
        if (period.status == PeriodStatus.COMPLETED) {
            item {
                FinniCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("Период уже завершён", style = MaterialTheme.typography.titleLarge)
                    Text("План сохранён, а итог можно посмотреть снова.")
                    Button(onClick = onShowSummary, modifier = Modifier.fillMaxWidth()) { Text("Открыть итоги") }
                }
            }
        } else if (plan.status == PlanStatus.DRAFT) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CoinPill(plan.baseAvailable, "Можно распределить", Modifier.weight(1f))
                    CoinPill(buffer, if (buffer >= 0) "Свободно" else "Лишнее", Modifier.weight(1f))
                }
            }
            item {
                Text(
                    "Разложи монеты. Значения можно менять до подтверждения.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            item {
                StepControl(
                    title = "Забота · нужное",
                    emoji = "🧺",
                    value = required,
                    onValueChange = { required = it },
                    color = MaterialTheme.colorScheme.secondaryContainer,
                )
            }
            item {
                StepControl(
                    title = "Радость · желаемое",
                    emoji = "🎈",
                    value = optional,
                    onValueChange = { optional = it },
                    color = Color(0xFFFFE8B1),
                )
            }
            item {
                StepControl(
                    title = "Мечта · накопления",
                    emoji = "✨",
                    value = savings,
                    onValueChange = { savings = it },
                    color = MaterialTheme.colorScheme.primaryContainer,
                )
            }
            item {
                if (required < 35) {
                    FinniCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text("На энергобатарею и моторное масло нужно 45 монет. Можно добавить в «Заботу» или попробовать исправить ситуацию заданием.")
                    }
                }
                Button(
                    onClick = { onConfirm(required, optional, savings) },
                    enabled = buffer >= 0 && total > 0 && !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) { Text("Подтвердить план · $total") }
            }
        } else {
            item {
                FinniCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text("План подтверждён", style = MaterialTheme.typography.titleLarge)
                    Text("Теперь покупки и накопления сравниваются с ним.")
                }
            }
            item {
                PlanFactRow("🧺", "Забота", plan.required, snapshot.requiredActual, MaterialTheme.colorScheme.secondary)
            }
            item {
                PlanFactRow("🎈", "Радость", plan.optional, snapshot.optionalActual, MaterialTheme.colorScheme.tertiary)
            }
            item {
                PlanFactRow("✨", "Мечта", plan.savings, snapshot.savingsDeposited, MaterialTheme.colorScheme.primary)
            }
            item {
                FinniCard {
                    Text("Свободный остаток в плане: ${plan.buffer} 🪙")
                    Text(
                        "План остаётся неизменным, даже если после задания появился дополнительный доход.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Button(
                    onClick = onCompletePeriod,
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) { Text("Завершить период") }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun PlanFactRow(emoji: String, title: String, plan: Int, fact: Int, color: Color) {
    FinniCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$emoji $title", style = MaterialTheme.typography.titleMedium)
            Text("$fact / $plan", fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { if (plan == 0) 0f else (fact.toFloat() / plan).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = color,
        )
        Text(
            when {
                fact == plan -> "Точно по плану"
                fact < plan -> "Осталось по плану: ${plan - fact}"
                else -> "Больше плана на ${fact - plan}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

package com.mikhailskiy.finni.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikhailskiy.finni.domain.GameCatalog
import com.mikhailskiy.finni.domain.GameSnapshot
import com.mikhailskiy.finni.domain.PeriodStatus
import com.mikhailskiy.finni.domain.PlanStatus
import com.mikhailskiy.finni.ui.components.CoinPill
import com.mikhailskiy.finni.ui.components.CatalogIcon
import com.mikhailskiy.finni.ui.components.FinniCard
import com.mikhailskiy.finni.ui.components.PetAvatar
import com.mikhailskiy.finni.ui.components.PetStatus

@Composable
fun HomeScreen(
    snapshot: GameSnapshot,
    stepSensorAvailable: Boolean,
    stepPermissionGranted: Boolean,
    onRequestStepPermission: () -> Unit,
    onBudget: () -> Unit,
    onTasks: () -> Unit,
    onShop: () -> Unit,
    onSavings: () -> Unit,
    onProgress: () -> Unit,
    onHelp: () -> Unit,
    onAdult: () -> Unit,
    onSummary: () -> Unit,
) {
    val profile = snapshot.profile ?: return
    val pet = snapshot.pet ?: return
    val period = snapshot.period ?: return
    val goalDefinition = snapshot.goal?.let { GameCatalog.goal(it.goalId) }
    val activeTask = GameCatalog.tasks.firstOrNull { it.id !in snapshot.completedTaskIds }
    val planConfirmed = snapshot.plan?.status == PlanStatus.CONFIRMED

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Привет, ${profile.nickname}!", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Период ${period.number} · ${pet.stage.title}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onHelp) { Text("?") }
                OutlinedButton(onClick = onAdult) { Text("Взрослым") }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CoinPill(snapshot.wallet.available, "Доступно", Modifier.weight(1f))
                CoinPill(snapshot.wallet.savings, "В мечте", Modifier.weight(1f))
            }
        }
        item {
            FinniCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PetAvatar(pet = pet, large = true)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(pet.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Рост: ${pet.growthPoints} · ${pet.stage.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        PetStatus("Сытость", pet.satiety, "🥕")
                        PetStatus("Настроение", pet.mood, "😊")
                        PetStatus("Уют", pet.comfort, "🏠")
                    }
                }
            }
        }
        item {
            FinniCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text("🚶 Активность героя", style = MaterialTheme.typography.titleMedium)
                when {
                    !stepSensorAvailable -> {
                        Text("На этом устройстве нет аппаратного датчика шагов.")
                    }
                    !stepPermissionGranted -> {
                        Text("Разреши Финни считать шаги. Данные остаются только на телефоне.")
                        Button(
                            onClick = onRequestStepPermission,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Включить подсчёт шагов") }
                    }
                    else -> {
                        val progress = snapshot.stepProgress.stepsSinceLastSatietyPoint.toFloat() /
                            snapshot.stepProgress.stepsPerSatietyPoint
                        Text(
                            "Учтено шагов: ${snapshot.stepProgress.trackedSteps}",
                            fontWeight = FontWeight.Bold,
                        )
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "До снижения сытости на 1: " +
                                "${snapshot.stepProgress.stepsUntilNextSatietyPoint} шагов",
                        )
                        Text(
                            "Каждые ${snapshot.stepProgress.stepsPerSatietyPoint} шагов " +
                                "герой тратит энергию и становится голоднее.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val equipmentHint = when {
                            pet.hasJetpack && pet.hasShoes ->
                                "🚀 Рюкзак и энергообувь увеличивают порог до 2000 шагов."
                            pet.hasJetpack ->
                                "🚀 Реактивный рюкзак увеличивает порог до 1500 шагов."
                            pet.hasShoes ->
                                "⚡ Энергообувь снижает расход сытости вдвое."
                            else -> null
                        }
                        equipmentHint?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
        item {
            FinniCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CatalogIcon(
                        catalogId = goalDefinition?.id.orEmpty(),
                        fallbackEmoji = goalDefinition?.emoji ?: "✨",
                        modifier = Modifier.size(52.dp),
                        emojiSize = 34.sp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Моя мечта", style = MaterialTheme.typography.titleMedium)
                        if (goalDefinition == null) {
                            Text("Выбери цель и начни копить")
                        } else {
                            Text("${goalDefinition.title}: ${snapshot.wallet.savings}/${goalDefinition.target}")
                        }
                    }
                    OutlinedButton(onClick = onSavings) { Text(if (goalDefinition == null) "Выбрать" else "Открыть") }
                }
            }
        }
        item {
            FinniCard(modifier = Modifier.fillMaxWidth()) {
                Text("Текущий шаг", style = MaterialTheme.typography.titleMedium)
                when {
                    period.status == PeriodStatus.COMPLETED -> {
                        Text("Период завершён — посмотри, как план совпал с фактом.")
                        Button(onClick = onSummary, modifier = Modifier.fillMaxWidth()) { Text("Посмотреть итоги") }
                    }
                    !planConfirmed -> {
                        Text("Сначала разложи монеты по трём кармашкам.")
                        Button(onClick = onBudget, modifier = Modifier.fillMaxWidth()) { Text("Составить план") }
                    }
                    activeTask != null -> {
                        Text("${activeTask.title} · награда ${activeTask.reward} 🪙")
                        Button(onClick = onTasks, modifier = Modifier.fillMaxWidth()) { Text("Открыть задание") }
                    }
                    else -> {
                        Text("Все задания пройдены. Можно закончить покупки и подвести итог.")
                        Button(onClick = onBudget, modifier = Modifier.fillMaxWidth()) { Text("План и факт") }
                    }
                }
            }
        }
        item {
            Text("Быстрые действия", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickAction("🧺", "Бюджет", onBudget, Modifier.weight(1f))
                QuickAction("🛍️", "Покупки", onShop, Modifier.weight(1f))
                QuickAction("📚", "Прогресс", onProgress, Modifier.weight(1f))
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun QuickAction(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

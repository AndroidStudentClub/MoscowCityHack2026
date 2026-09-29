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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikhailskiy.finni.data.AppSettings
import com.mikhailskiy.finni.domain.GameCatalog
import com.mikhailskiy.finni.domain.GameSnapshot
import com.mikhailskiy.finni.domain.GrowthStage
import com.mikhailskiy.finni.domain.TaskTopic
import com.mikhailskiy.finni.ui.components.FinniCard
import com.mikhailskiy.finni.ui.components.PetAvatar
import com.mikhailskiy.finni.ui.components.ScreenHeader

@Composable
fun ProgressScreen(snapshot: GameSnapshot, onBack: () -> Unit) {
    val pet = snapshot.pet ?: return
    val nextThreshold = when (pet.stage) {
        GrowthStage.BABY -> 30
        GrowthStage.EXPLORER -> 70
        GrowthStage.EXPERT -> 100
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { ScreenHeader("Прогресс", "Рост — результат нескольких решений", onBack) }
        item {
            FinniCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    PetAvatar(pet)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pet.name, style = MaterialTheme.typography.titleLarge)
                        Text("Стадия: ${pet.stage.title}", fontWeight = FontWeight.Bold)
                        Text("Очки роста: ${pet.growthPoints}")
                    }
                }
                LinearProgressIndicator(
                    progress = { (pet.growthPoints.toFloat() / nextThreshold).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (pet.stage == GrowthStage.EXPERT) "Высшая стадия открыта"
                    else "До следующей стадии: ${(nextThreshold - pet.growthPoints).coerceAtLeast(0)} очков",
                )
            }
        }
        item { Text("Пройденные темы", style = MaterialTheme.typography.titleLarge) }
        TaskTopic.entries.forEach { topic ->
            item {
                val topicTasks = GameCatalog.tasks.filter { it.topic == topic }
                val completed = topicTasks.count { it.id in snapshot.completedTaskIds }
                FinniCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(topic.title, style = MaterialTheme.typography.titleMedium)
                        Text("$completed/${topicTasks.size}", fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { completed.toFloat() / topicTasks.size },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item { Text("История периодов", style = MaterialTheme.typography.titleLarge) }
        if (snapshot.summaries.isEmpty()) {
            item { Text("Первый итог появится после завершения периода.") }
        } else {
            snapshot.summaries.forEach { summary ->
                item {
                    FinniCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Период ${summary.periodNumber}", style = MaterialTheme.typography.titleMedium)
                            Text("+${summary.growthAwarded} роста", fontWeight = FontWeight.Bold)
                        }
                        Text("Забота ${summary.requiredActual} · Радость ${summary.optionalActual} · Мечта ${summary.savingsDeposited}")
                        Text(summary.feedback, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            FinniCard {
                Text("Короткий словарик", style = MaterialTheme.typography.titleLarge)
                Text("План — как ты решил распределить монеты заранее.")
                Text("Факт — что получилось после покупок и пополнений.")
                Text("Накопления — монеты, отложенные отдельно на мечту.")
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun SummaryScreen(
    snapshot: GameSnapshot,
    isBusy: Boolean,
    onNextPeriod: () -> Unit,
    onBack: () -> Unit,
) {
    val period = snapshot.period ?: return
    val plan = snapshot.plan ?: return
    val summary = snapshot.summaries.firstOrNull { it.periodId == period.id } ?: return
    val pet = snapshot.pet ?: return
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { ScreenHeader("Итоги периода ${summary.periodNumber}", "План сравнивается с реальными решениями", onBack) }
        item {
            FinniCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Text("${pet.name} получает +${summary.growthAwarded} очков роста", style = MaterialTheme.typography.titleLarge)
                Text(summary.feedback)
                Text("Теперь у питомца ${pet.growthPoints} очков · ${pet.stage.title}", fontWeight = FontWeight.Bold)
            }
        }
        item { SummaryRow("🧺", "Забота", plan.required, summary.requiredActual) }
        item { SummaryRow("🎈", "Радость", plan.optional, summary.optionalActual) }
        item { SummaryRow("✨", "Мечта", plan.savings, summary.savingsDeposited) }
        item {
            FinniCard {
                Text("Что изменилось", style = MaterialTheme.typography.titleMedium)
                Text("Доход за период: +${summary.income} монет")
                Text("Доступный баланс: ${summary.closingAvailable}")
                Text("Всего в накоплениях: ${summary.closingSavings}")
            }
        }
        item {
            Button(
                onClick = onNextPeriod,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) { Text("Начать следующий период") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SummaryRow(emoji: String, title: String, plan: Int, fact: Int) {
    FinniCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$emoji $title", style = MaterialTheme.typography.titleMedium)
            Text("план $plan · факт $fact", fontWeight = FontWeight.Bold)
        }
        Text(
            when {
                fact == plan -> "Точно по плану"
                fact < plan -> "Потрачено меньше плана на ${plan - fact}"
                else -> "Потрачено больше плана на ${fact - plan}"
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun AdultGateScreen(onBack: () -> Unit, onUnlocked: () -> Unit) {
    var answer by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenHeader("Раздел взрослого", "Барьер защищает от случайного перехода", onBack)
        FinniCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
            Text("17 + 6 = ?", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("Введите ответ, чтобы открыть настройки и общий прогресс.")
        }
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it.filter(Char::isDigit).take(2); showError = false },
            label = { Text("Ответ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        if (showError) Text("Ответ не подошёл. Попробуйте ещё раз.", color = MaterialTheme.colorScheme.error)
        Button(
            onClick = { if (answer == "23") onUnlocked() else showError = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) { Text("Открыть") }
    }
}

@Composable
fun AdultScreen(
    snapshot: GameSnapshot,
    settings: AppSettings,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onMotionChange: (Boolean) -> Unit,
    onResetDemo: () -> Unit,
    onDeleteProfile: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { ScreenHeader("Для взрослого", "Поддержка без оценок ребёнка", onBack) }
        item {
            FinniCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Text("Чему помогает игра", style = MaterialTheme.typography.titleLarge)
                Text("• различать необходимое и желаемое")
                Text("• планировать ограниченный бюджет")
                Text("• регулярно откладывать на цель")
                Text("• объяснять последствия своих решений")
            }
        }
        item {
            FinniCard {
                Text("Общий прогресс", style = MaterialTheme.typography.titleLarge)
                Text("Завершено периодов: ${snapshot.summaries.size}")
                Text("Пройдено заданий: ${snapshot.completedTaskIds.size} из ${GameCatalog.tasks.size}")
                Text("Стадия питомца: ${snapshot.pet?.stage?.title.orEmpty()}")
                Text("Здесь нет оценок «плохо» и сравнений с другими детьми.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            FinniCard {
                SettingRow("Звуки", "Можно отключить в любой момент", settings.soundEnabled, onSoundChange)
                SettingRow("Меньше анимации", "Все изменения всё равно видны текстом", settings.reducedMotion, onMotionChange)
            }
        }
        if (snapshot.profile?.isDemo == true) {
            item {
                OutlinedButton(
                    onClick = { confirmReset = true },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Сбросить тестовый профиль") }
            }
        }
        item {
            OutlinedButton(
                onClick = { confirmDelete = true },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Удалить локальный профиль") }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Сбросить демопрофиль?",
            text = "Баланс, покупки, цель и прогресс вернутся к исходному состоянию.",
            confirm = "Сбросить",
            onDismiss = { confirmReset = false },
            onConfirm = { confirmReset = false; onResetDemo() },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Удалить профиль?",
            text = "Все локальные данные будут удалены. Восстановить их не получится.",
            confirm = "Удалить",
            onDismiss = { confirmDelete = false },
            onConfirm = { confirmDelete = false; onDeleteProfile() },
        )
    }
}

@Composable
private fun SettingRow(title: String, text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { Button(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

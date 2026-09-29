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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikhailskiy.finni.domain.BudgetCategory
import com.mikhailskiy.finni.domain.GameCatalog
import com.mikhailskiy.finni.domain.TaskDefinition
import com.mikhailskiy.finni.domain.TaskKind
import com.mikhailskiy.finni.domain.TaskTopic
import com.mikhailskiy.finni.ui.components.CatalogIcon
import com.mikhailskiy.finni.ui.components.FinniCard
import com.mikhailskiy.finni.ui.components.ScreenHeader
import com.mikhailskiy.finni.ui.components.StepControl

@Composable
fun TasksScreen(
    completedTaskIds: Set<String>,
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { ScreenHeader("Финансовые задания", "Решай ситуации действием, а не угадыванием", onBack) }
        TaskTopic.entries.forEach { topic ->
            item {
                Text(topic.title, style = MaterialTheme.typography.titleLarge)
            }
            GameCatalog.tasks.filter { it.topic == topic }.forEach { task ->
                item {
                    val completed = task.id in completedTaskIds
                    FinniCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = if (completed) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surface,
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(task.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(if (completed) "✓" else "+${task.reward} 🪙", fontWeight = FontWeight.Bold)
                        }
                        Text(task.description, style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(
                            onClick = { onOpenTask(task.id) },
                            enabled = !completed,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (completed) "Выполнено" else "Начать") }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun TaskPlayScreen(
    taskId: String,
    isBusy: Boolean,
    onBack: () -> Unit,
    onComplete: (String) -> Unit,
) {
    val task = GameCatalog.task(taskId) ?: return
    var feedback by remember(taskId) { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { ScreenHeader(task.title, "Награда: ${task.reward} монет", onBack) }
        item {
            FinniCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Text(task.description, style = MaterialTheme.typography.titleMedium)
                Text("Можно пробовать несколько раз. Ошибка не забирает прогресс.")
            }
        }
        item {
            when (task.kind) {
                TaskKind.BUDGET -> BudgetTask(
                    task = task,
                    isBusy = isBusy,
                    onWrong = { feedback = task.retryText },
                    onComplete = onComplete,
                )
                TaskKind.BASKET -> BasketTask(
                    task = task,
                    isBusy = isBusy,
                    onWrong = { feedback = task.retryText },
                    onComplete = onComplete,
                )
                TaskKind.SAVINGS -> SavingsTask(
                    task = task,
                    isBusy = isBusy,
                    onWrong = { feedback = task.retryText },
                    onComplete = onComplete,
                )
            }
        }
        if (feedback != null) {
            item {
                FinniCard(containerColor = Color(0xFFFFE8B1)) {
                    Text("Попробуем ещё раз", style = MaterialTheme.typography.titleMedium)
                    Text(feedback.orEmpty())
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun BudgetTask(
    task: TaskDefinition,
    isBusy: Boolean,
    onWrong: () -> Unit,
    onComplete: (String) -> Unit,
) {
    var required by remember(task.id) { mutableIntStateOf(20) }
    var optional by remember(task.id) { mutableIntStateOf(20) }
    var savings by remember(task.id) { mutableIntStateOf(10) }
    val total = required + optional + savings
    val requiredTarget = if (task.id == "budget_surprise") 35 else 30

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Доступно 60 · распределено $total", fontWeight = FontWeight.Bold)
        StepControl("Забота", "🧺", required, { required = it }, color = MaterialTheme.colorScheme.secondaryContainer)
        StepControl("Радость", "🎈", optional, { optional = it }, color = Color(0xFFFFE8B1))
        StepControl("Мечта", "✨", savings, { savings = it }, color = MaterialTheme.colorScheme.primaryContainer)
        Button(
            onClick = {
                val correct = total <= 60 && required >= requiredTarget && savings >= 10
                if (correct) onComplete("required=$required;optional=$optional;savings=$savings") else onWrong()
            },
            enabled = !isBusy,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) { Text("Проверить мой план") }
    }
}

@Composable
private fun BasketTask(
    task: TaskDefinition,
    isBusy: Boolean,
    onWrong: () -> Unit,
    onComplete: (String) -> Unit,
) {
    val candidateIds = remember(task.id) {
        if (task.id == "basket_breakfast") listOf("water", "food", "helmet", "glasses")
        else listOf("water", "food", "brush", "helmet", "glasses")
    }
    val selected = remember(task.id) { mutableStateListOf<String>() }
    val items = candidateIds.mapNotNull(GameCatalog::shopItem)
    val total = items.filter { it.id in selected }.sumOf { it.price }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Корзина: $total монет", style = MaterialTheme.typography.titleMedium)
        items.forEach { item ->
            FilterChip(
                selected = item.id in selected,
                onClick = {
                    if (item.id in selected) selected.remove(item.id) else selected.add(item.id)
                },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CatalogIcon(
                            catalogId = item.id,
                            fallbackEmoji = item.emoji,
                            modifier = Modifier.size(24.dp),
                            emojiSize = 20.sp,
                        )
                        Text("${item.title} · ${item.price} · ${if (item.category == BudgetCategory.REQUIRED) "нужно" else "желаемое"}")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Button(
            onClick = {
                val correct = if (task.id == "basket_breakfast") {
                    "water" in selected && "food" in selected && total <= 50
                } else {
                    items.count { it.id in selected && it.category == BudgetCategory.REQUIRED } >= 2 && total <= 55
                }
                if (correct) onComplete("basket=${selected.joinToString()};total=$total") else onWrong()
            },
            enabled = !isBusy,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) { Text("Проверить корзину") }
    }
}

@Composable
private fun SavingsTask(
    task: TaskDefinition,
    isBusy: Boolean,
    onWrong: () -> Unit,
    onComplete: (String) -> Unit,
) {
    val options = listOf(5, 10, 15, 20)
    val selected = remember(task.id) { mutableStateListOf<Int>() }
    val total = selected.sum()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Выбрано ${selected.size} взноса · всего $total", style = MaterialTheme.typography.titleMedium)
        options.forEachIndexed { index, amount ->
            FilterChip(
                selected = amount in selected,
                onClick = {
                    if (amount in selected) selected.remove(amount) else selected.add(amount)
                },
                label = { Text("Неделя ${index + 1}: отложить $amount монет") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Button(
            onClick = {
                if (selected.size >= 3 && total >= 30) {
                    onComplete("deposits=${selected.joinToString()};total=$total")
                } else onWrong()
            },
            enabled = !isBusy,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) { Text("Проверить путь к цели") }
    }
}
